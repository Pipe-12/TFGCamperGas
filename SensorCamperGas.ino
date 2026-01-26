#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"
#include <HX711.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>
#include <math.h>
#include "esp_sleep.h"
#include "esp_bt.h"
#include "esp_wifi.h"
#include "esp_task_wdt.h"
#include <EEPROM.h>

//BLE server name
#define bleServerName "CamperGas_Sensor"

// Configuración de ahorro de energía
#define LIGHT_SLEEP_TIME_CONNECTED 5 // 5 segundos cuando conectado
#define CPU_FREQ_LOW 80 // MHz para bajo consumo
#define CPU_FREQ_NORMAL 240 // MHz para operación normal

// Configuración de timeouts y reintentos
#define HX711_READY_TIMEOUT_MS 5000
#define HX711_READY_ATTEMPTS 50
#define HX711_AVERAGE_READINGS 3
#define TARE_READINGS 10
#define ZERO_FACTOR_READINGS 5

// Configuración de EEPROM para almacenamiento persistente de tara y factor de calibración
#define EEPROM_SIZE 64
#define EEPROM_TARE_VALID_ADDR 0     // Dirección para el marcador de validez de tara (1 byte)
#define EEPROM_TARE_OFFSET_ADDR 4    // Dirección para el offset de tara (4 bytes - long en ESP32)
#define EEPROM_TARE_MAGIC_NUMBER 0xAB // Número mágico para verificar que la EEPROM tiene datos válidos de tara
#define EEPROM_CAL_VALID_ADDR 8      // Dirección para el marcador de validez de calibración (1 byte)
#define EEPROM_CAL_FACTOR_ADDR 12    // Dirección para el factor de calibración (4 bytes - float en ESP32)
#define EEPROM_CAL_MAGIC_NUMBER 0xCD // Número mágico para verificar que la EEPROM tiene factor de calibración válido
#define DEFAULT_CALIBRATION_FACTOR 25000.0 // Factor de calibración por defecto

// Pin de datos y de reloj para HX711
const byte pinData = 4;
const byte pinClk = 2;

HX711 bascula;

// Crear el objeto del sensor ADXL345
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);

float factor_calibracion = DEFAULT_CALIBRATION_FACTOR;  // Factor de calibración (se carga desde EEPROM si existe)
float peso;
float pitch, roll; // Variables para inclinación

// Variables de estado de energía
bool sensorsInitialized = false;
bool tareCompleted = false;

// Timer variables para offline
unsigned long lastOfflineTime = 0;
unsigned long offlineTimerDelay = 900000; // 15 minutos (15 * 60 * 1000 ms)
const unsigned long initialOfflineDelay = 900000; // 15 minutos inicial
const unsigned long maxOfflineDelay = 86400000; // 24 horas (24 * 60 * 60 * 1000 ms)
int offlineIndex = 0; // Índice circular para reemplazar medidas antiguas

// Estructura para almacenar datos offline
struct MeasurementData {
  float weight;
  unsigned long timestamp; // Timestamp en milisegundos desde boot (millis())
};

// Array para almacenar medidas offline (máximo 100 medidas)
#define MAX_OFFLINE_MEASUREMENTS 100
MeasurementData offlineMeasurements[MAX_OFFLINE_MEASUREMENTS];
int offlineMeasurementCount = 0;

bool deviceConnected = false;

// Declaraciones de funciones
void sendOfflineData();
void resetOfflineSystem();
void powerDownSensors();
void powerUpSensors();
void initSensors();
void initHX711();
void initHX711WithTare();
void initADXL345();
void readInclination();
bool readWeight();
void storeOfflineMeasurement(float weight, unsigned long timestamp);
void enterLightSleep();
void performTare();          // Nueva función de callback para tara manual
void saveTareToEEPROM();     // Guardar offset de tara en EEPROM
bool loadTareFromEEPROM();   // Cargar offset de tara desde EEPROM
void performCalibration(float knownWeight); // Realizar calibración con peso conocido
void saveCalibrationToEEPROM();   // Guardar factor de calibración en EEPROM
bool loadCalibrationFromEEPROM(); // Cargar factor de calibración desde EEPROM

// Ver lo siguiente para generar UUIDs:
// https://www.uuidgenerator.net/

// Un único servicio para el sensor de peso con inclinación
#define SENSOR_SERVICE_UUID "91bad492-b950-4226-aa2b-4ede9fa42f59"

// Tres características dentro del mismo servicio
#define WEIGHT_CHARACTERISTIC_UUID "cba1d466-344c-4be3-ab3f-189f80dd7518"
#define OFFLINE_CHARACTERISTIC_UUID "87654321-4321-4321-4321-cba987654321"
#define INCLINATION_CHARACTERISTIC_UUID "fedcba09-8765-4321-fedc-ba0987654321"
#define TARE_CHARACTERISTIC_UUID "12345678-1234-1234-1234-123456789abc"
#define CALIBRATION_CHARACTERISTIC_UUID "a1b2c3d4-e5f6-4789-ab01-abcdef123456"

// Caracteristica de Peso (READ-only, sin descriptor)
BLECharacteristic weightCharacteristics("cba1d466-344c-4be3-ab3f-189f80dd7518", BLECharacteristic::PROPERTY_READ);

// Caracteristica de Datos Offline (READ-only, sin descriptor)
BLECharacteristic offlineDataCharacteristics("87654321-4321-4321-4321-cba987654321", BLECharacteristic::PROPERTY_READ);

// Caracteristica de Inclinacion (READ-only, sin descriptor)
BLECharacteristic inclinationCharacteristics("fedcba09-8765-4321-fedc-ba0987654321", BLECharacteristic::PROPERTY_READ);

// Caracteristica de Tara (WRITE para permitir tara manual mediante callback)
BLECharacteristic tareCharacteristics("12345678-1234-1234-1234-123456789abc", BLECharacteristic::PROPERTY_WRITE);

// Caracteristica de Calibración (WRITE para permitir calibración dinámica del factor de la báscula)
// Acepta JSON con peso conocido en kg, por ejemplo: {"cal":2.00}
BLECharacteristic calibrationCharacteristics("a1b2c3d4-e5f6-4789-ab01-abcdef123456", BLECharacteristic::PROPERTY_WRITE);

// Callback para lecturas de características
class MyCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onRead(BLECharacteristic* pCharacteristic) {
        String uuid = pCharacteristic->getUUID().toString();
        
        if (uuid == WEIGHT_CHARACTERISTIC_UUID) {
            Serial.println("Dispositivo movil solicita PESO - tomando medida...");
            
            // Asegurar que los sensores están listos
            if (!sensorsInitialized) {
                powerUpSensors();
            }
            
            // Leer peso
            bool success = readWeight();
            
            static char weightData[20];
            sprintf(weightData, "{\"w\":%.1f}", peso);
            pCharacteristic->setValue(weightData);
            
            if (success) {
                Serial.print("Peso enviado: ");
                Serial.print(peso);
                Serial.println(" kg");
            }
        }
        else if (uuid == INCLINATION_CHARACTERISTIC_UUID) {
            Serial.println("Dispositivo movil solicita INCLINACION - tomando medida...");
            
            // Asegurar que los sensores están listos
            if (!sensorsInitialized) {
                powerUpSensors();
            }
            
            // Leer inclinación
            readInclination();
            
            static char inclinationData[30];
            sprintf(inclinationData, "{\"p\":%.1f,\"r\":%.1f}", pitch, roll);
            pCharacteristic->setValue(inclinationData);
            
            Serial.print("Inclinacion enviada - Pitch: ");
            Serial.print(pitch);
            Serial.print("° | Roll: ");
            Serial.print(roll);
            Serial.println("°");
        }
        else if (uuid == OFFLINE_CHARACTERISTIC_UUID) {
            Serial.println("Dispositivo movil solicita DATOS OFFLINE");
            // La característica offline ya maneja su propio contenido
            // No necesita callback especial porque ya está preparada
        }
    }
};

// Callback para característica de TARA (escritura = realizar tara manual)
class TareCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        Serial.println("Dispositivo movil solicita TARA MANUAL");
        
        // Realizar tara manual
        performTare();
        
        // Responder con confirmación
        pCharacteristic->setValue("{\"tare\":\"ok\"}");
        
        Serial.println("Tara manual completada y guardada en EEPROM");
    }
};

// Callback para característica de CALIBRACIÓN (escritura = realizar calibración con peso conocido)
// Formato esperado: JSON con peso conocido en kg, por ejemplo: {"cal":2.00}
// El peso debe estar colocado sobre la báscula antes de enviar este comando
class CalibrationCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        Serial.println("Dispositivo movil solicita CALIBRACION");
        
        std::string value = pCharacteristic->getValue();
        if (value.length() == 0) {
            Serial.println("ERROR: Valor vacío recibido para calibración");
            pCharacteristic->setValue("{\"cal\":\"error\",\"msg\":\"empty_value\"}");
            return;
        }
        
        Serial.print("Valor recibido: ");
        Serial.println(value.c_str());
        
        // Parsear el JSON para obtener el peso conocido
        // Formato esperado: {"cal":2.00}
        float knownWeight = 0.0;
        const char* jsonStr = value.c_str();
        
        // Buscar el valor después de "cal":
        const char* calPos = strstr(jsonStr, "\"cal\":");
        if (calPos != nullptr) {
            calPos += 6; // Avanzar después de "cal":
            // Saltar espacios
            while (*calPos == ' ') calPos++;
            // Verificar que el siguiente carácter es un dígito o punto decimal
            if ((*calPos >= '0' && *calPos <= '9') || *calPos == '.') {
                knownWeight = atof(calPos);
            } else {
                Serial.println("ERROR: Valor de calibración no es un número válido");
                pCharacteristic->setValue("{\"cal\":\"error\",\"msg\":\"not_a_number\"}");
                return;
            }
        } else {
            // Intentar parsear como número directo (formato alternativo)
            // Verificar que comienza con dígito o punto decimal
            if ((*jsonStr >= '0' && *jsonStr <= '9') || *jsonStr == '.') {
                knownWeight = atof(jsonStr);
            } else {
                Serial.println("ERROR: Formato de calibración inválido");
                pCharacteristic->setValue("{\"cal\":\"error\",\"msg\":\"invalid_format\"}");
                return;
            }
        }
        
        if (knownWeight < 0.01) {
            Serial.println("ERROR: Peso conocido inválido (debe ser >= 0.01 kg)");
            pCharacteristic->setValue("{\"cal\":\"error\",\"msg\":\"weight_too_small\"}");
            return;
        }
        
        Serial.print("Peso conocido: ");
        Serial.print(knownWeight);
        Serial.println(" kg");
        
        // Realizar calibración
        performCalibration(knownWeight);
        
        // Responder con confirmación y nuevo factor
        static char response[60];
        sprintf(response, "{\"cal\":\"ok\",\"factor\":%.2f}", factor_calibracion);
        pCharacteristic->setValue(response);
        
        Serial.println("Calibración completada y guardada en EEPROM");
    }
};

// Configurar callbacks onConnect y onDisconnect
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    setCpuFrequencyMhz(CPU_FREQ_NORMAL); // Aumentar frecuencia cuando conectado
    Serial.println("Cliente conectado - modo activo");
    
    Serial.println("Proceso de conexion completado");
    Serial.println("MODO PULL ACTIVADO:");
    Serial.println("- El dispositivo movil debe LEER las caracteristicas para obtener datos");
    Serial.println("- PESO: cba1d466-344c-4be3-ab3f-189f80dd7518 (toma medida al leer)");
    Serial.println("  - INCLINACION: fedcba09-8765-4321-fedc-ba0987654321 (toma medida al leer)");
    Serial.println("- DATOS OFFLINE: 87654321-4321-4321-4321-cba987654321 (datos historicos)");
    
    // Preparar datos offline si hay datos almacenados
    if (offlineMeasurementCount > 0) {
      Serial.println("Preparando datos offline...");
      sendOfflineData();
      resetOfflineSystem();
    }
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    setCpuFrequencyMhz(CPU_FREQ_LOW); // Reducir frecuencia cuando desconectado
    Serial.println("Cliente desconectado - modo ahorro energía");
    powerDownSensors(); // Apagar sensores no críticos
    
    // Reiniciar advertising después de la desconexión
    delay(500);
    pServer->getAdvertising()->start();
    Serial.println("Advertising reiniciado");
  }
};

// Función para almacenar medida offline con sistema circular
void storeOfflineMeasurement(float weight, unsigned long timestamp) {
  // Mostrar datos antes de almacenar
  Serial.print("Preparando almacenar - Peso: ");
  Serial.print(weight);
  Serial.print(" kg | Timestamp: ");
  Serial.print(timestamp);
  Serial.println(" ms");
  
  if (offlineMeasurementCount < MAX_OFFLINE_MEASUREMENTS) {
    // Memoria no llena, añadir normalmente
    offlineMeasurements[offlineMeasurementCount].weight = weight;
    offlineMeasurements[offlineMeasurementCount].timestamp = timestamp;
    offlineMeasurementCount++;
    Serial.print("Medida offline almacenada: ");
    Serial.print(weight);
    Serial.print(" kg, timestamp: ");
    Serial.print(timestamp);
    Serial.print(" ms. Total almacenadas: ");
    Serial.println(offlineMeasurementCount);
  } else {
    // Memoria llena, reemplazar la más antigua usando índice circular
    Serial.print("Memoria llena - Reemplazando en indice ");
    Serial.print(offlineIndex);
    Serial.print(" - Peso: ");
    Serial.print(weight);
    Serial.print(" kg | Timestamp: ");
    Serial.print(timestamp);
    Serial.println(" ms");
    
    offlineMeasurements[offlineIndex].weight = weight;
    offlineMeasurements[offlineIndex].timestamp = timestamp;
    offlineIndex = (offlineIndex + 1) % MAX_OFFLINE_MEASUREMENTS;
    
    Serial.print("Medida antigua reemplazada: ");
    Serial.print(weight);
    Serial.print(" kg, timestamp: ");
    Serial.print(timestamp);
    Serial.print(" ms. Nuevo índice: ");
    Serial.println(offlineIndex);
    
    // Duplicar el tiempo entre medidas (máximo 24 horas)
    if (offlineTimerDelay < maxOfflineDelay) {
      offlineTimerDelay *= 2;
      if (offlineTimerDelay > maxOfflineDelay) {
        offlineTimerDelay = maxOfflineDelay;
      }
      Serial.print("Intervalo aumentado a: ");
      Serial.print(offlineTimerDelay / 1000);
      Serial.print(" segundos (");
      Serial.print(offlineTimerDelay / 60000);
      Serial.println(" minutos)");
    }
  }
}

// Función para reiniciar el sistema offline
void resetOfflineSystem() {
  offlineTimerDelay = initialOfflineDelay;
  offlineIndex = 0;
  Serial.println("Sistema offline reiniciado");
}

// Funciones de gestión de energía mejoradas
void powerDownSensors() {
  Serial.println("Sensores en modo ahorro de energía");
}

void powerUpSensors() {
  if (!sensorsInitialized) {
    initSensors();
  } else {
    Serial.println("Sensores activados");
  }
}

void initSensors() {
  Serial.println("=== INICIALIZANDO SENSORES ===");
  
  initHX711WithTare();
  initADXL345();
  
  sensorsInitialized = true;
  Serial.println("=== SENSORES INICIALIZADOS ===");
}

void enterLightSleep() {
  if (deviceConnected) {
    // Delay corto cuando conectado para responder rápido
    delay(100);
  } else {
    // NO usar light sleep real porque desactiva el BLE advertising
    // En su lugar, usar delay más largo para ahorrar energía manteniendo BLE activo
    delay(1000); // 1 segundo - suficiente para ahorrar energía pero mantener BLE advertising
  }
}

// Función para enviar datos offline cuando se conecta BLE
void sendOfflineData() {
  if (offlineMeasurementCount > 0) {
    Serial.print("=== PREPARANDO ");
    Serial.print(offlineMeasurementCount);
    Serial.println(" MEDIDAS OFFLINE VIA CARACTERÍSTICA READ-ONLY (BATCH MODE) ===");
    
    // Verificar que el dispositivo sigue conectado
    if (!deviceConnected) {
      Serial.println("ERROR: Dispositivo desconectado durante preparación offline");
      return;
    }
    
    // Información para el usuario sobre cómo leer los datos
    Serial.println("DATOS OFFLINE LISTOS PARA LECTURA");
    Serial.println("El cliente debe LEER la caracteristica: 87654321-4321-4321-4321-cba987654321");
    Serial.println("Los datos NO se envian automaticamente (caracteristica read-only)");
    
    // Preparar datos en lotes para lectura posterior
    static char offlineDataString[500];  // Buffer grande para múltiples medidas
    int batchCount = 0;
    int totalBatches = 0; // Contador de lotes preparados
    
    // Construir JSON array con múltiples medidas
    strcpy(offlineDataString, "[");  // Iniciar array JSON
    
    // Timestamp actual para calcular tiempo transcurrido
    unsigned long currentReadTime = millis();
    Serial.print("Tiempo actual de lectura: ");
    Serial.print(currentReadTime);
    Serial.println(" ms");
    
    for (int i = 0; i < offlineMeasurementCount; i++) {
      // Calcular tiempo transcurrido desde la medida hasta ahora
      unsigned long elapsedTime = currentReadTime - offlineMeasurements[i].timestamp;
      
      Serial.print("Medida ");
      Serial.print(i);
      Serial.print(": Peso=");
      Serial.print(offlineMeasurements[i].weight);
      Serial.print("kg, Almacenada en=");
      Serial.print(offlineMeasurements[i].timestamp);
      Serial.print("ms, Transcurrido=");
      Serial.print(elapsedTime);
      Serial.print("ms (");
      Serial.print(elapsedTime / 1000);
      Serial.println(" segundos)");
      
      char singleMeasurement[50];  // Aumentado a 50 para timestamp completo
      sprintf(singleMeasurement, "{\"w\":%.1f,\"t\":%lu}", 
              offlineMeasurements[i].weight, 
              elapsedTime); // Usar tiempo transcurrido en lugar de timestamp absoluto
      
      // Verificar si cabe en el buffer actual
      if (strlen(offlineDataString) + strlen(singleMeasurement) + 10 < 500) {
        // Añadir coma si no es el primer elemento
        if (i > 0) strcat(offlineDataString, ",");
        strcat(offlineDataString, singleMeasurement);
        batchCount++;
      } else {
        // Buffer lleno, preparar lote actual
        strcat(offlineDataString, "]");  // Cerrar array JSON
        
        totalBatches++;
        Serial.print("  Preparando LOTE ");
        Serial.print(totalBatches);
        Serial.print(" [");
        Serial.print(batchCount);
        Serial.print(" medidas]: ");
        Serial.println(offlineDataString);
        Serial.print("   UUID OFFLINE: 87654321-4321-4321-4321-cba987654321, Tamaño: ");
        Serial.print(strlen(offlineDataString));
        Serial.println(" bytes");
        
        // Verificar conexión
        if (!deviceConnected) {
          Serial.println("Conexion perdida durante preparacion");
          break;
        }
        
        // Solo establecer valor para lectura posterior (NO notify)
        offlineDataCharacteristics.setValue(offlineDataString);
        Serial.println("   Lote preparado para lectura (caracteristica actualizada)");
        
        // Delay entre preparaciones para estabilidad
        delay(500);
        
        // Reiniciar buffer para siguiente lote
        strcpy(offlineDataString, "[");
        strcat(offlineDataString, singleMeasurement);
        batchCount = 1;
      }
    }
    
    // Enviar último lote si queda algo
    if (batchCount > 0) {
      strcat(offlineDataString, "]");  // Cerrar array JSON
      
      totalBatches++;
      Serial.print("  Preparando lote FINAL ");
      Serial.print(totalBatches);
      Serial.print(" [");
      Serial.print(batchCount);
      Serial.print(" medidas]: ");
      Serial.println(offlineDataString);
      Serial.print("   UUID OFFLINE: 87654321-4321-4321-4321-cba987654321, Tamaño: ");
      Serial.print(strlen(offlineDataString));
      Serial.println(" bytes");
      
      if (deviceConnected) {
        offlineDataCharacteristics.setValue(offlineDataString);
        Serial.println("   Lote final preparado para lectura (caracteristica actualizada)");
      }
    }
    
    // Vaciar memoria después de preparar datos
    offlineMeasurementCount = 0;
    Serial.println("=== DATOS OFFLINE PREPARADOS Y MEMORIA VACIADA ===");
    Serial.print("RESUMEN FINAL: ");
    Serial.print(totalBatches);
    Serial.println(" lotes preparados total");
    Serial.println("El cliente debe LEER la caracteristica para obtener los datos");
    Serial.println("FORMATO JSON: {\"w\":peso_kg,\"t\":milisegundos_transcurridos_desde_medida}");
  } else {
    Serial.println("No hay datos offline para preparar");
  }
}

void initHX711(){
  Serial.println("Iniciando la Bascula...");
  
  // Siempre inicializar la báscula
  bascula.begin(pinData, pinClk);
  
  // Esperar a que el HX711 esté listo con timeout mejorado
  Serial.print("Esperando HX711");
  int attempts = 0;
  while (!bascula.is_ready() && attempts < HX711_READY_ATTEMPTS) {
    delay(100);
    Serial.print(".");
    attempts++;
  }
  Serial.println("");
  
  if (!bascula.is_ready()) {
    Serial.print("ERROR: HX711 no responde después de ");
    Serial.print(HX711_READY_TIMEOUT_MS / 1000);
    Serial.println(" segundos");
    return;
  }
  
  Serial.println("HX711 listo, configurando...");
  
  // Cargar factor de calibración desde EEPROM (o usar valor por defecto)
  loadCalibrationFromEEPROM();
  
  // Configurar la escala con el factor de calibración
  bascula.set_scale(factor_calibracion);
  
  Serial.print("HX711 inicializado con factor de calibración: ");
  Serial.println(factor_calibracion);
}

// Función específica para inicialización completa con tara (solo arranque inicial)
void initHX711WithTare(){
  Serial.println("Iniciando la Bascula con tara persistente (EEPROM)...");
  
  initHX711();
  
  if (!bascula.is_ready()) {
    Serial.println("ERROR: No se puede hacer tara, HX711 no está listo");
    return;
  }
  
  // Intentar cargar tara desde EEPROM
  if (loadTareFromEEPROM()) {
    Serial.println("Tara cargada desde EEPROM - no se necesita nueva tara");
    tareCompleted = true;
    
    float test_reading = bascula.get_units(HX711_AVERAGE_READINGS);
    Serial.print("Lectura de prueba con tara restaurada: ");
    Serial.print(test_reading);
    Serial.println(" kg");
    
    Serial.println("HX711 inicializado con tara desde EEPROM");
  } else {
    // Primera vez - hacer tara y guardar en EEPROM
    Serial.println("Primera inicializacion - haciendo tara y guardando en EEPROM...");
    performTare();
    
    Serial.println("HX711 inicializado con nueva tara guardada en EEPROM");
  }
}

// Función de callback para realizar tara manual
void performTare() {
  Serial.println("Realizando tara...");
  
  if (!bascula.is_ready()) {
    Serial.println("ERROR: HX711 no está listo para tara");
    return;
  }
  
  bascula.tare(TARE_READINGS);
  tareCompleted = true;
  
  // Guardar el offset de tara en EEPROM
  saveTareToEEPROM();
  
  long zero_factor = bascula.read_average(ZERO_FACTOR_READINGS);
  Serial.print("Nuevo Zero factor: ");
  Serial.println(zero_factor);
  
  float test_reading = bascula.get_units(HX711_AVERAGE_READINGS);
  Serial.print("Lectura de prueba: ");
  Serial.print(test_reading);
  Serial.println(" kg");
  
  Serial.println("Tara completada y guardada en EEPROM");
}

// Guardar offset de tara en EEPROM
void saveTareToEEPROM() {
  long offset = bascula.get_offset();
  
  // Escribir número mágico para indicar que hay datos válidos
  EEPROM.write(EEPROM_TARE_VALID_ADDR, EEPROM_TARE_MAGIC_NUMBER);
  
  // Escribir el offset de tara (long = 4 bytes en ESP32)
  EEPROM.put(EEPROM_TARE_OFFSET_ADDR, offset);
  
  // Confirmar escritura en EEPROM
  EEPROM.commit();
  
  Serial.print("Offset de tara guardado en EEPROM: ");
  Serial.println(offset);
}

// Cargar offset de tara desde EEPROM
bool loadTareFromEEPROM() {
  // Verificar si hay datos válidos en EEPROM
  uint8_t magicNumber = EEPROM.read(EEPROM_TARE_VALID_ADDR);
  
  if (magicNumber != EEPROM_TARE_MAGIC_NUMBER) {
    Serial.println("No hay tara valida en EEPROM (primera ejecucion o EEPROM vacia)");
    return false;
  }
  
  // Leer el offset de tara
  long offset;
  EEPROM.get(EEPROM_TARE_OFFSET_ADDR, offset);
  
  // Aplicar el offset a la báscula
  bascula.set_offset(offset);
  
  Serial.print("Offset de tara restaurado desde EEPROM: ");
  Serial.println(offset);
  
  return true;
}

// Realizar calibración con peso conocido
// PROCEDIMIENTO DE CALIBRACIÓN:
// 1. La báscula ya debe haber sido tarada previamente (en initHX711WithTare o vía característica TARA)
// 2. Colocar el peso de referencia conocido sobre la báscula
// 3. Enviar el comando de calibración con el peso conocido en kg: {"cal":2.00}
// 4. El sistema calculará el factor de calibración basándose en la lectura cruda y el peso conocido
// 5. El nuevo factor se guardará en EEPROM y persistirá entre reinicios
void performCalibration(float knownWeight) {
  Serial.println("Realizando calibración...");
  Serial.println("IMPORTANTE: Asegúrese de que el peso de referencia está sobre la báscula");
  Serial.println("y que la báscula fue tarada previamente sin peso.");
  
  // Verificar que el HX711 está listo
  if (!bascula.is_ready()) {
    Serial.println("ERROR: HX711 no está listo para calibración");
    // Intentar inicializar si es necesario
    if (!sensorsInitialized) {
      powerUpSensors();
    }
    
    // Verificar de nuevo
    int attempts = 0;
    while (!bascula.is_ready() && attempts < HX711_READY_ATTEMPTS) {
      delay(100);
      attempts++;
    }
    
    if (!bascula.is_ready()) {
      Serial.println("ERROR: HX711 no responde después de reintentos");
      return;
    }
  }
  
  Serial.println("HX711 listo para calibración");
  
  // Guardar el factor actual temporalmente
  float oldFactor = factor_calibracion;
  
  // Establecer escala a 1.0 para obtener lectura cruda
  // La lectura cruda incluye el offset de tara, por lo que obtenemos
  // el valor correspondiente solo al peso de referencia
  bascula.set_scale(1.0);
  
  // Leer el valor crudo del HX711 (promedio de varias lecturas)
  float rawReading = bascula.get_units(HX711_AVERAGE_READINGS);
  
  // Multiplicar por -1 para mantener coherencia con el resto del código
  rawReading = -1 * rawReading;
  
  Serial.print("Lectura cruda del HX711: ");
  Serial.println(rawReading);
  
  // Validar que la lectura cruda es razonable
  // Si es muy cercana a cero, probablemente no hay peso o hay un problema
  if (fabs(rawReading) < 100.0) {
    Serial.println("ADVERTENCIA: Lectura cruda muy baja - verifique que el peso está sobre la báscula");
  }
  
  // Calcular el nuevo factor de calibración con protección adicional
  // Verificar que knownWeight no sea extremadamente pequeño para evitar factores enormes
  if (knownWeight < 0.01) {
    Serial.println("ERROR: Peso conocido demasiado pequeño (mínimo 0.01 kg)");
    bascula.set_scale(oldFactor);
    return;
  }
  
  float newFactor = rawReading / knownWeight;
  
  Serial.print("Peso conocido: ");
  Serial.print(knownWeight);
  Serial.println(" kg");
  
  Serial.print("Nuevo factor de calibración calculado: ");
  Serial.println(newFactor);
  
  // Verificar que el factor es razonable
  // Factores típicos para HX711 están en el rango de 1000 a 1000000
  if (newFactor <= 0.0 || isnan(newFactor) || isinf(newFactor)) {
    Serial.println("ERROR: Factor de calibración inválido, restaurando valor anterior");
    bascula.set_scale(oldFactor);
    return;
  }
  
  // Verificar que el factor no sea extremadamente grande o pequeño
  if (newFactor < 100.0 || newFactor > 10000000.0) {
    Serial.println("ADVERTENCIA: Factor de calibración fuera del rango esperado (100-10000000)");
    Serial.println("Esto puede indicar un problema con la configuración o el peso de referencia");
  }
  
  // Aplicar el nuevo factor
  factor_calibracion = newFactor;
  bascula.set_scale(factor_calibracion);
  
  // Guardar en EEPROM
  saveCalibrationToEEPROM();
  
  // Lectura de prueba con el nuevo factor
  float testReading = -1 * bascula.get_units(HX711_AVERAGE_READINGS);
  Serial.print("Lectura de prueba con nuevo factor: ");
  Serial.print(testReading);
  Serial.println(" kg");
  
  Serial.println("Calibración completada exitosamente");
}

// Guardar factor de calibración en EEPROM
void saveCalibrationToEEPROM() {
  // Escribir número mágico para indicar que hay datos válidos de calibración
  EEPROM.write(EEPROM_CAL_VALID_ADDR, EEPROM_CAL_MAGIC_NUMBER);
  
  // Escribir el factor de calibración (float = 4 bytes en ESP32)
  EEPROM.put(EEPROM_CAL_FACTOR_ADDR, factor_calibracion);
  
  // Confirmar escritura en EEPROM
  EEPROM.commit();
  
  Serial.print("Factor de calibración guardado en EEPROM: ");
  Serial.println(factor_calibracion);
}

// Cargar factor de calibración desde EEPROM
bool loadCalibrationFromEEPROM() {
  // Verificar si hay datos válidos de calibración en EEPROM
  uint8_t magicNumber = EEPROM.read(EEPROM_CAL_VALID_ADDR);
  
  if (magicNumber != EEPROM_CAL_MAGIC_NUMBER) {
    Serial.println("No hay factor de calibración en EEPROM - usando valor por defecto");
    Serial.print("Factor por defecto: ");
    Serial.println(DEFAULT_CALIBRATION_FACTOR);
    factor_calibracion = DEFAULT_CALIBRATION_FACTOR;
    return false;
  }
  
  // Leer el factor de calibración
  float storedFactor;
  EEPROM.get(EEPROM_CAL_FACTOR_ADDR, storedFactor);
  
  // Verificar que el factor es válido
  if (storedFactor <= 0.0 || isnan(storedFactor) || isinf(storedFactor)) {
    Serial.println("Factor de calibración en EEPROM inválido - usando valor por defecto");
    factor_calibracion = DEFAULT_CALIBRATION_FACTOR;
    return false;
  }
  
  // Aplicar el factor de calibración
  factor_calibracion = storedFactor;
  
  Serial.print("Factor de calibración restaurado desde EEPROM: ");
  Serial.println(factor_calibracion);
  
  return true;
}

void initADXL345(){
  Serial.println("Iniciando el ADXL345...");

  if (!accel.begin()) {
    Serial.println("No se pudo encontrar el ADXL345");
    return;
  }

  accel.setRange(ADXL345_RANGE_2_G);
  
  Serial.println("ADXL345 conectado correctamente");
}

void readInclination() {
  sensors_event_t event;
  accel.getEvent(&event);

  // Obtener los valores de aceleración en los tres ejes
  float x = event.acceleration.x;
  float y = event.acceleration.y;
  float z = event.acceleration.z;

  // Calcular el pitch y roll 
  pitch = atan2(y, sqrt(x * x + z * z)) * 180.0 / PI;
  roll = atan2(-x, sqrt(y * y + z * z)) * 180.0 / PI;

  // Debug: Mostrar valores
  Serial.print("Valores crudos ADXL345 - X: ");
  Serial.print(x);
  Serial.print(" Y: ");
  Serial.print(y);
  Serial.print(" Z: ");
  Serial.println(z);

  Serial.print("Pitch: ");
  Serial.print(pitch);
  Serial.print("° | Roll: ");
  Serial.print(roll);
  Serial.println("°");
}

bool readWeight() {
  // Verificar que HX711 está listo con retry logic
  bool sensorReady = false;
  int retryAttempts = 3; // Intentar hasta 3 veces
  
  for (int i = 0; i < retryAttempts && !sensorReady; i++) {
    if (bascula.is_ready()) {
      sensorReady = true;
    } else {
      if (i == 0) {
        Serial.print("HX711 no esta listo, reintentando");
      }
      Serial.print(".");
      delay(50); // Esperar 50ms antes del siguiente intento
    }
  }
  
  if (sensorReady) {
    peso = -1 * bascula.get_units(HX711_AVERAGE_READINGS);
    
    if (!isnan(peso)) {
      Serial.print("Peso leido: ");
      Serial.print(peso);
      Serial.println(" kg");
      return true;
    } else {
      Serial.println("ERROR: Lectura NaN de HX711");
      peso = 0.0;
      return false;
    }
  } else {
    Serial.println("\nERROR: HX711 no esta listo despues de reintentos");
    peso = 0.0;
    return false;
  }
}

void setup() {
  // Configurar CPU a baja frecuencia inicialmente
  setCpuFrequencyMhz(CPU_FREQ_LOW);
  
  // Start serial communication 
  Serial.begin(115200);
  delay(500); // Reducir delay inicial
  
  // Inicialización completa
  Serial.println("Inicialización completa...");
  
  // Inicializar EEPROM para almacenamiento persistente de tara y factor de calibración
  if (!EEPROM.begin(EEPROM_SIZE)) {
    Serial.println("ERROR: No se pudo inicializar EEPROM");
  } else {
    Serial.println("EEPROM inicializada correctamente");
  }
  
  // Deshabilitar WiFi para ahorrar energía
  esp_wifi_stop();
  
  // Init Sensors
  initSensors();

  // Create the BLE Device
  BLEDevice::init(bleServerName);
  
  // Configurar MTU máximo para transmisiones más grandes
  BLEDevice::setMTU(512); // MTU más grande para mayor capacidad de datos

  // Create the BLE Server
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Services (un solo servicio con 3 características)
  BLEService *sensorService = pServer->createService(SENSOR_SERVICE_UUID);

  // Create BLE Characteristics (todas READ-only)
  // Peso (READ-only, sin descriptor)
  sensorService->addCharacteristic(&weightCharacteristics);
  weightCharacteristics.setCallbacks(new MyCharacteristicCallbacks());
  
  // Datos Offline (READ-only, sin descriptor)
  sensorService->addCharacteristic(&offlineDataCharacteristics);
  // No agregar descriptor ni callback para característica offline
  
  // Inclinacion (READ-only, sin descriptor)
  sensorService->addCharacteristic(&inclinationCharacteristics);
  inclinationCharacteristics.setCallbacks(new MyCharacteristicCallbacks());
  
  // Tara (WRITE - permite tara manual mediante callback)
  sensorService->addCharacteristic(&tareCharacteristics);
  tareCharacteristics.setCallbacks(new TareCharacteristicCallbacks());
  
  // Calibración (WRITE - permite calibración dinámica del factor de la báscula)
  // Formato JSON: {"cal":peso_conocido_kg} - por ejemplo: {"cal":2.00}
  sensorService->addCharacteristic(&calibrationCharacteristics);
  calibrationCharacteristics.setCallbacks(new CalibrationCharacteristicCallbacks());
  
  // Start the service
  sensorService->start();
  
  Serial.println("Servicio BLE iniciado correctamente");

  // Start advertising con configuración optimizada para conexión estable
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SENSOR_SERVICE_UUID);
  
  // Configurar advertising para conexión más estable
  pAdvertising->setMinInterval(320);  // 200ms - más rápido para descubrimiento
  pAdvertising->setMaxInterval(640);  // 400ms - más rápido para descubrimiento
  
  // Habilitar scan response para mejor compatibilidad
  pAdvertising->setScanResponse(true);
  
  pServer->getAdvertising()->start();
  Serial.println("Sistema BLE activo - esperando conexion...");
  Serial.println("INFORMACION IMPORTANTE:");
  Serial.println("- MODO PULL ACTIVADO: El dispositivo movil debe LEER las caracteristicas");
  Serial.println("- PESO: cba1d466-344c-4be3-ab3f-189f80dd7518 (toma medida al leer)");
  Serial.println("- INCLINACION: fedcba09-8765-4321-fedc-ba0987654321 (toma medida al leer)");
  Serial.println("- DATOS OFFLINE: 87654321-4321-4321-4321-cba987654321 (datos historicos)");
  Serial.println("- TARA: 12345678-1234-1234-1234-123456789abc (escribir para hacer tara manual)");
  Serial.println("- CALIBRACION: a1b2c3d4-e5f6-4789-ab01-abcdef123456 (escribir JSON {\"cal\":peso_kg})");
  
  // Apagar sensores no críticos inicialmente
  powerDownSensors();
}

void loop() {
  unsigned long currentTime = millis();
  
  if (!deviceConnected) {
    // Modo offline - tomar medidas periódicamente para almacenar datos históricos
    if ((currentTime - lastOfflineTime) > offlineTimerDelay) {
      Serial.println("Tomando medida offline (sin conexión BLE)...");
      
      // Asegurar que los sensores están listos
      if (!sensorsInitialized) {
        powerUpSensors();
      }
      
      // Verificar que HX711 está listo
      if (bascula.is_ready()) {
        float offlineWeight = -1 * bascula.get_units(HX711_AVERAGE_READINGS);
        unsigned long timestamp = currentTime; // Usar millis() directamente
        
        Serial.print("Medida offline obtenida - Peso: ");
        Serial.print(offlineWeight);
        Serial.print(" kg | Timestamp generado: ");
        Serial.print(timestamp);
        Serial.print(" ms (");
        Serial.print(currentTime);
        Serial.println(" ms)");
        
        if (!isnan(offlineWeight)) {
          storeOfflineMeasurement(offlineWeight, timestamp);
          Serial.print("Proceso completado - Peso almacenado: ");
          Serial.print(offlineWeight);
          Serial.println(" kg");
        } else {
          Serial.println("ERROR: Lectura NaN en modo offline - no se almacena");
        }
      } else {
        Serial.println("WARNING: HX711 no está listo en modo offline");
      }
      
      lastOfflineTime = currentTime;
    }
    
    // Usar enterLightSleep para ahorrar energía manteniendo BLE disponible
    enterLightSleep();
    return;
  }
  
  // Dispositivo conectado - modo PULL activo
  if (!sensorsInitialized) {
    powerUpSensors();
  }
  
  // En modo PULL, no hay medidas automáticas
  // Las medidas se toman solo cuando el dispositivo móvil lee las características
  // Esto se maneja en los callbacks de MyCharacteristicCallbacks
  
  // Usar enterLightSleep entre verificaciones para ahorrar energía
  enterLightSleep();
}
