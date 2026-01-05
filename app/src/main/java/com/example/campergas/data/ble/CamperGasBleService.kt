package com.example.campergas.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.domain.model.CamperGasUuids
import com.example.campergas.domain.model.FuelMeasurement
import com.example.campergas.domain.model.Inclination
import com.example.campergas.domain.usecase.GetActiveCylinderUseCase
import com.example.campergas.domain.usecase.SaveFuelMeasurementUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified BLE service to handle all characteristics from CamperGas sensor
 * The sensor has a single service with three READ-only characteristics:
 * - FuelMeasurement: fuel measurement data (on-demand reading)
 * - Inclination: inclination data (on-demand reading)
 * - Offline: historical data in batches (continuous reading on connect)
 *
 * IMPORTANT: The sensor now uses READ mode instead of notifications.
 * The application must actively request data by reading the characteristics.
 */
@Singleton
class CamperGasBleService @Inject constructor(
    private val bleManager: BleManager,
    private val saveFuelMeasurementUseCase: SaveFuelMeasurementUseCase,
    private val getActiveCylinderUseCase: GetActiveCylinderUseCase,
    private val preferencesDataStore: PreferencesDataStore,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CamperGasBleService"
    }

    // Scope for background operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connection states
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    // Real-time weight data (maintain for sensor compatibility)
    private val _fuelMeasurementData = MutableStateFlow<FuelMeasurement?>(null)
    val fuelMeasurementData: StateFlow<FuelMeasurement?> = _fuelMeasurementData

    // Calculated fuel data
    private val _fuelData = MutableStateFlow<FuelMeasurement?>(null)
    val fuelData: StateFlow<FuelMeasurement?> = _fuelData

    // Real-time inclination data
    private val _inclinationData = MutableStateFlow<Inclination?>(null)
    val inclinationData: StateFlow<Inclination?> = _inclinationData

    // Historical data
    private val _historyData = MutableStateFlow<List<FuelMeasurement>>(emptyList())

    // Historical data loading state
    private val _isLoadingHistory = MutableStateFlow(false)

    // Control for lectura continua of data offline
    private var isReadingOfflineData = false
    private var offlineDataCount = 0
    private val allHistoryData = mutableListOf<FuelMeasurement>()
    private val processedOfflineData =
        mutableSetOf<String>() // Para evitar duplicados por peso+tiempo

    // Control for serializar operaciones BLE
    private var isReadingInProgress = false
    private val readingQueue = mutableListOf<() -> Unit>()
    private var readingTimeoutJob: kotlinx.coroutines.Job? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private var fuelMeasurementCharacteristic: BluetoothGattCharacteristic? = null
    private var inclinationCharacteristic: BluetoothGattCharacteristic? = null
    private var offlineCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "🔄 onConnectionStateChange - status: $status, newState: $newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "✅ Connected to CamperGas sensor")
                    _connectionState.value = true
                    // Discover services only if we have permissions
                    if (bleManager.hasBluetoothConnectPermission()) {
                        @SuppressLint("MissingPermission")
                        gatt?.discoverServices()
                    } else {
                        Log.e(TAG, "❌ No permissions to discover services")
                        disconnect()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "❌ Callback: Disconnected from sensor CamperGas (status: $status)")

                    // Only update if we have not forced disconnection yet
                    if (_connectionState.value) {
                        Log.d(TAG, "❌ Updating connection state from callback")
                        _connectionState.value = false
                    } else {
                        Log.d(TAG, "❌ State was already updated manually")
                    }

                    _isLoadingHistory.value = false
                    // Stop periodic reading on disconnect
                    stopPeriodicDataReading()
                    cleanup()

                    // If disconnection was unexpected (status != 0), log it
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.w(TAG, "⚠️ Unexpected disconnection - status: $status")
                    }
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "🔄 Connecting to sensor CamperGas...")
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.d(TAG, "🔄 Disconnecting from CamperGas sensor...")
                    // Stop periodic reading when starting disconnection
                    stopPeriodicDataReading()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                // Search for CamperGas sensor service
                val sensorService =
                    gatt?.getService(UUID.fromString(CamperGasUuids.SENSOR_SERVICE_UUID))

                if (sensorService != null) {
                    Log.d(TAG, "CamperGas service found")

                    // Get all characteristics
                    setupCharacteristics(sensorService)
                } else {
                    Log.e(TAG, "CamperGas service not found")
                    listAvailableServices(gatt)
                }
            } else {
                Log.e(TAG, "Error al descubrir servicios: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            // Cancelar el timeout ya que recibimos respuesta
            readingTimeoutJob?.cancel()

            // Marcar que ya no hay una lectura en progreso
            isReadingInProgress = false

            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid.toString().lowercase()) {
                    CamperGasUuids.WEIGHT_CHARACTERISTIC_UUID.lowercase() -> {
                        processFuelMeasurementData(value)
                    }

                    CamperGasUuids.INCLINATION_CHARACTERISTIC_UUID.lowercase() -> {
                        processInclinationData(value)
                    }

                    CamperGasUuids.OFFLINE_CHARACTERISTIC_UUID.lowercase() -> {
                        processOfflineData(value)
                    }
                }
            } else {
                Log.e(TAG, "Error al leer characteristic ${characteristic.uuid}: $status")
                if (characteristic.uuid.toString()
                        .lowercase() == CamperGasUuids.OFFLINE_CHARACTERISTIC_UUID.lowercase()
                ) {
                    _isLoadingHistory.value = false
                }
            }

            // Procesar el siguiente elemento en la cola
            processNextReadingInQueue()
        }
    }

    private fun setupCharacteristics(service: BluetoothGattService) {
        // Verify permissions before setting up characteristics
        if (!bleManager.hasBluetoothConnectPermission()) {
            Log.e(TAG, "No hay permisos for configurar characteristics")
            return
        }

        // Configure fuel measurement characteristic (READ-only)
        fuelMeasurementCharacteristic = service.getCharacteristic(
            UUID.fromString(CamperGasUuids.WEIGHT_CHARACTERISTIC_UUID)
        )
        if (fuelMeasurementCharacteristic != null) {
            Log.d(TAG, "Fuel measurement characteristic found (READ-only)")
        } else {
            Log.w(TAG, "Fuel measurement characteristic not found")
        }

        // Configure inclination characteristic (READ-only)
        inclinationCharacteristic = service.getCharacteristic(
            UUID.fromString(CamperGasUuids.INCLINATION_CHARACTERISTIC_UUID)
        )
        if (inclinationCharacteristic != null) {
            Log.d(TAG, "Inclination characteristic found (READ-only)")
            Log.d(TAG, "UUID inclination: ${inclinationCharacteristic?.uuid}")
            Log.d(TAG, "Propiedades inclination: ${inclinationCharacteristic?.properties}")
        } else {
            Log.w(TAG, "Inclination characteristic not found")
            Log.w(TAG, "UUID buscado: ${CamperGasUuids.INCLINATION_CHARACTERISTIC_UUID}")
        }

        // Configure offline characteristic (READ-only)
        offlineCharacteristic = service.getCharacteristic(
            UUID.fromString(CamperGasUuids.OFFLINE_CHARACTERISTIC_UUID)
        )
        if (offlineCharacteristic != null) {
            Log.d(TAG, "Offline characteristic found (READ-only)")
            // Start automatic offline data reading on connect
            Log.d(TAG, "Starting automatic offline data reading...")
            startAutomaticOfflineDataReading()
        } else {
            Log.w(TAG, "Offline characteristic not found")
        }

        // Start periodic real-time data reading
        loadConfigurationAndStartReading()
    }

    /**
     * Loads the saved configuration and starts periodic reading
     */
    private fun loadConfigurationAndStartReading() {
        serviceScope.launch {
            try {
                // Load configuration from preferences
                val weightInterval = preferencesDataStore.weightReadInterval.first()
                val inclinationInterval = preferencesDataStore.inclinationReadInterval.first()

                // Apply configuration
                configureReadingIntervals(weightInterval, inclinationInterval)

                // Start periodic reading
                startPeriodicDataReading()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading configuration: ${e.message}")
                // Use default values if it fails
                configureReadingIntervals(5000L, 5000L)
                startPeriodicDataReading()
            }
        }
    }

    // Variables for periodic reading control
    private var isPeriodicReadingActive = false
    private var periodicReadingJob: kotlinx.coroutines.Job? = null
    private var lastWeightReadTime = 0L
    private var lastInclinationReadTime = 0L

    // Intervalos configurables (por defecto 5 segundos)
    private var weightReadInterval = 5000L // 5 segundos entre readings of weight
    private var inclinationReadInterval = 5000L // 5 segundos entre readings of inclination

    /**
     * Configures reading intervals for weight and inclination
     * @form weightIntervalMs Intervalo en milisegundos for lectura of weight
     * @form inclinationIntervalMs Intervalo en milisegundos for lectura of inclination
     */
    fun configureReadingIntervals(weightIntervalMs: Long, inclinationIntervalMs: Long) {
        val oldWeightInterval = weightReadInterval
        val oldInclinationInterval = inclinationReadInterval

        weightReadInterval = weightIntervalMs
        inclinationReadInterval = inclinationIntervalMs

        Log.d(
            TAG,
            "Intervalos configurados - Weight: ${weightIntervalMs}ms, Inclination: ${inclinationIntervalMs}ms"
        )

        // If intervals changed and we are connected, restart periodic reading
        if ((oldWeightInterval != weightIntervalMs || oldInclinationInterval != inclinationIntervalMs)
            && isConnected()
        ) {
            Log.d(TAG, "Restarting periodic reading with new intervals...")
            restartPeriodicReading()
        }
    }

    /**
     * Restarts periodic reading with new intervals
     */
    private fun restartPeriodicReading() {
        if (isPeriodicReadingActive) {
            Log.d(TAG, "Stopping current periodic reading...")
            stopPeriodicDataReading()

            // Short pause before restarting
            serviceScope.launch {
                delay(500)
                if (isConnected()) {
                    Log.d(TAG, "Restarting periodic reading with new intervals...")
                    startPeriodicDataReading()
                }
            }
        }
    }

    /**
     * Gets el intervalo actual de lectura of weight
     */
    fun getWeightReadInterval(): Long = weightReadInterval

    /**
     * Gets el intervalo actual de lectura of inclination
     */
    fun getInclinationReadInterval(): Long = inclinationReadInterval

    /**
     * Starts periodic real-time data reading
     */
    private fun startPeriodicDataReading() {
        if (isPeriodicReadingActive) {
            Log.d(TAG, "Periodic reading is already active")
            return
        }

        isPeriodicReadingActive = true
        periodicReadingJob = serviceScope.launch {
            Log.d(TAG, "🔄 Starting periodic real-time data reading...")
            Log.d(
                TAG,
                "📊 Weight interval: ${weightReadInterval}ms, Inclination interval: ${inclinationReadInterval}ms"
            )

            while (isPeriodicReadingActive && isConnected()) {
                try {
                    val currentTime = System.currentTimeMillis()

                    // Read weight if more than configured interval has passed since last reading
                    if (currentTime - lastWeightReadTime > weightReadInterval) {
                        readWeightData()
                        lastWeightReadTime = currentTime
                    }

                    // Wait a bit more before reading inclination to avoid BLE conflicts
                    delay(500)

                    // Read inclination if more than configured interval has passed since last reading
                    if (currentTime - lastInclinationReadTime > inclinationReadInterval) {
                        readInclinationData()
                        lastInclinationReadTime = currentTime
                    }

                    // Pause between verification cycles (1000ms)
                    delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic reading: ${e.message}")
                    delay(2000) // Longer pause in case of error
                }
            }

            Log.d(TAG, "Periodic real-time data reading finished")
        }
    }

    /**
     * Stops periodic real-time data reading
     */
    private fun stopPeriodicDataReading() {
        isPeriodicReadingActive = false
        periodicReadingJob?.cancel()
        periodicReadingJob = null
        Log.d(TAG, "Periodic real-time data reading stopped")
    }

    /**
     * Lee data of weight desof the characteristic
     */
    private fun readWeightData() {
        queueBleRead {
            fuelMeasurementCharacteristic?.let { characteristic ->
                bluetoothGatt?.let { gatt ->
                    if (bleManager.hasBluetoothConnectPermission()) {
                        @SuppressLint("MissingPermission")
                        val success = gatt.readCharacteristic(characteristic)
                        if (!success) {
                            Log.e(TAG, "Error al leer data of weight")
                            isReadingInProgress = false
                            processNextReadingInQueue()
                        }
                    } else {
                        Log.e(TAG, "No hay permisos for leer data of weight")
                        isReadingInProgress = false
                        processNextReadingInQueue()
                    }
                }
            } ?: run {
                Log.e(TAG, "Weight characteristic not available")
                isReadingInProgress = false
                processNextReadingInQueue()
            }
        }
    }

    /**
     * Lee data of inclination desof the characteristic
     */
    private fun readInclinationData() {
        queueBleRead {
            inclinationCharacteristic?.let { characteristic ->
                bluetoothGatt?.let { gatt ->
                    if (bleManager.hasBluetoothConnectPermission()) {
                        Log.d(TAG, "Intentando leer data of inclination...")
                        @SuppressLint("MissingPermission")
                        val success = gatt.readCharacteristic(characteristic)
                        if (!success) {
                            Log.e(
                                TAG,
                                "Error reading inclination data - readCharacteristic() returned false"
                            )
                            isReadingInProgress = false
                            processNextReadingInQueue()
                        } else {
                            Log.d(TAG, "Lectura of inclination iniciada correctamente")
                        }
                    } else {
                        Log.e(TAG, "No hay permisos for leer data of inclination")
                        isReadingInProgress = false
                        processNextReadingInQueue()
                    }
                }
            } ?: run {
                Log.e(TAG, "Inclination characteristic not available")
                isReadingInProgress = false
                processNextReadingInQueue()
            }
        }
    }

    /**
     * Public function to read weight data on demand
     */
    fun readWeightDataOnDemand() {
        if (!isConnected()) {
            Log.w(TAG, "No connection activa for leer data of weight")
            return
        }
        readWeightData()
    }

    /**
     * Public function to read inclination data on demand
     */
    fun readInclinationDataOnDemand() {
        if (!isConnected()) {
            Log.w(TAG, "No connection activa for leer data of inclination")
            return
        }
        readInclinationData()
    }

    /**
     * Adds a BLE reading to queue to avoid concurrent readings
     */
    private fun queueBleRead(readOperation: () -> Unit) {
        synchronized(readingQueue) {
            readingQueue.add(readOperation)
            if (!isReadingInProgress) {
                processNextReadingInQueue()
            }
        }
    }

    /**
     * Procesa el siguiente elemento en la cola de readings BLE
     */
    private fun processNextReadingInQueue() {
        synchronized(readingQueue) {
            if (readingQueue.isNotEmpty() && !isReadingInProgress) {
                isReadingInProgress = true
                val nextRead = readingQueue.removeAt(0)

                // Configure timeout for read operation
                readingTimeoutJob = serviceScope.launch {
                    delay(5000) // Timeout de 5 segundos
                    if (isReadingInProgress) {
                        Log.w(TAG, "Timeout en lectura BLE, continuando con la siguiente")
                        isReadingInProgress = false
                        processNextReadingInQueue()
                    }
                }

                nextRead()
            }
        }
    }

    private fun listAvailableServices(gatt: BluetoothGatt?) {
        // Verificar permisos antes de listar servicios
        if (!bleManager.hasBluetoothConnectPermission()) {
            Log.e(TAG, "No hay permisos for listar servicios")
            return
        }

        gatt?.services?.forEach { service ->
            Log.d(TAG, "Service disponible: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d(TAG, "  - Characteristic: ${characteristic.uuid}")
            }
        }
    }

    /**
     * Procesa data de measurement EN TIEMPO REAL from sensor
     * This data comes from the WEIGHT_CHARACTERISTIC_UUID characteristic
     * and are automatically marked as isHistorical = false
     */
    private fun processFuelMeasurementData(data: ByteArray) {
        try {
            val jsonString = String(data, Charsets.UTF_8)
            Log.d(TAG, "Datos de measurement de combustible recibidos: $jsonString")

            // Parsear JSON: {"w":12.5}
            val jsonObject = JSONObject(jsonString)
            val totalWeight = jsonObject.getDouble("w").toFloat()

            // Guardar measurement de combustible EN TIEMPO REAL en la database
            // Estos data provienen de WEIGHT_CHARACTERISTIC_UUID y se marcan como isHistorical = false
            serviceScope.launch {
                try {
                    val result = saveFuelMeasurementUseCase.saveRealTimeMeasurement(
                        totalWeight = totalWeight,
                        timestamp = System.currentTimeMillis()
                    )

                    result.fold(
                        onSuccess = { saveResult ->
                            if (saveResult.processed) {
                                Log.d(
                                    TAG,
                                    "Fuel measurement saved (ID: ${saveResult.measurementId}) - ${saveResult.reason}"
                                )
                            } else {
                                Log.d(
                                    TAG,
                                    "Measurement skipped - ${saveResult.reason}"
                                )
                            }

                            // Siempre actualizar el StateFlow con los data de combustible
                            // regardless of whether it was saved to database or not
                            val activeCylinder = getActiveCylinderUseCase.getActiveCylinderSync()
                            if (activeCylinder != null) {
                                val fuelKilograms = maxOf(0f, totalWeight - activeCylinder.tare)
                                val fuelPercentage = if (activeCylinder.capacity > 0) {
                                    (fuelKilograms / activeCylinder.capacity * 100).coerceIn(
                                        0f,
                                        100f
                                    )
                                } else {
                                    0f
                                }

                                val fuelMeasurement = FuelMeasurement(
                                    id = if (saveResult.processed) saveResult.measurementId else -1L,
                                    cylinderId = activeCylinder.id,
                                    cylinderName = activeCylinder.name,
                                    timestamp = System.currentTimeMillis(),
                                    fuelKilograms = fuelKilograms,
                                    fuelPercentage = fuelPercentage,
                                    totalWeight = totalWeight,
                                    isCalibrated = true,
                                    isHistorical = false
                                )

                                _fuelMeasurementData.value = fuelMeasurement
                                _fuelData.value = fuelMeasurement
                                Log.d(
                                    TAG,
                                    "Combustible calculado: ${fuelKilograms}kg (${fuelPercentage}%)"
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.e(
                                TAG,
                                "Error al guardar measurement de combustible: ${error.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar measurement de combustible: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar data de measurement de combustible: ${e.message}")
        }
    }

    private fun processInclinationData(data: ByteArray) {
        try {
            val jsonString = String(data, Charsets.UTF_8)
            Log.d(TAG, "Datos of inclination recibidos: $jsonString")

            // Parsear JSON: {"p":15.2,"r":-3.1}
            val jsonObject = JSONObject(jsonString)
            val pitch = jsonObject.getDouble("p").toFloat()
            val roll = jsonObject.getDouble("r").toFloat()

            val inclination = Inclination(
                pitch = pitch,
                roll = roll,
                timestamp = System.currentTimeMillis()
            )

            _inclinationData.value = inclination
            Log.d(
                TAG,
                "Inclination updated: pitch=${inclination.pitch}°, roll=${inclination.roll}°"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar data of inclination: ${e.message}")
            Log.e(TAG, "Datos raw recibidos: ${data.contentToString()}")
        }
    }

    /**
     * Processes OFFLINE/HISTORICAL data from sensor
     * This data comes from the OFFLINE_CHARACTERISTIC_UUID characteristic
     * and are automatically marked as isHistorical = true
     */
    private fun processOfflineData(data: ByteArray) {
        try {
            val jsonString = String(data, Charsets.UTF_8)
            Log.d(TAG, "Datos offline recibidos (lote ${offlineDataCount + 1}): $jsonString")

            /*
             * IMPORTANT: Offline data comes with relative timestamps
             * The "t" field contains MILLISECONDS elapsed since measurement was taken
             * We must calculate absolute timestamp by subtracting these milliseconds from current time
             * Example: If "t":300000, it means measurement was taken 5 minutes ago (300000 ms)
             */

            // Verify if data is empty, is "0", or indicates end of data
            if (jsonString.isBlank() ||
                jsonString == "[]" ||
                jsonString == "{}" ||
                jsonString.equals("END", ignoreCase = true) ||
                jsonString.trim() == "0"
            ) {
                Log.d(TAG, "End of offline data detected (empty data or 0)")
                finishOfflineDataReading()
                return
            }

            // Parsear JSON array: [{"w":25.1,"t":1234567890},{"w":25.3,"t":1234567900}]
            val jsonArray = JSONArray(jsonString)

            // If array is empty, we are done
            if (jsonArray.length() == 0) {
                Log.d(TAG, "Empty array received - end of offline data")
                finishOfflineDataReading()
                return
            }

            val batchHistoricalMeasurements = mutableListOf<Pair<Float, Long>>()

            // Procesar en corrutina for manejar operaciones de database
            serviceScope.launch {
                try {
                    // Verificar si todos los data dthe batch ya han sido procesados
                    var allDataAlreadyProcessed = true

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val weightValue = jsonObject.getDouble("w").toFloat()
                        val millisecondsAgo = jsonObject.getLong("t")

                        // Create unique key for this data (weight + relative time in milliseconds)
                        val dataKey = "${weightValue}_${millisecondsAgo}"

                        if (!processedOfflineData.contains(dataKey)) {
                            allDataAlreadyProcessed = false
                            break
                        }
                    }

                    // Si todos los data ya fueron procesados, ignorar este lote
                    if (allDataAlreadyProcessed) {
                        Log.d(TAG, "🛑 Lote completo ya procesado - ignorando data duplicados")
                        return@launch
                    }

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val weightValue = jsonObject.getDouble("w").toFloat()
                        val millisecondsAgo =
                            jsonObject.getLong("t") // MILLISECONDS elapsed since measurement was taken

                        // Create unique key for this data
                        val dataKey = "${weightValue}_${millisecondsAgo}"

                        // Si ya procesamos este dato exacto, saltarlo
                        if (processedOfflineData.contains(dataKey)) {
                            Log.d(
                                TAG,
                                "⏭️ Dato duplicado ignorado: ${weightValue}kg hace ${millisecondsAgo}ms"
                            )
                            continue
                        }

                        // Marcar este dato como procesado
                        processedOfflineData.add(dataKey)

                        // Calculate real timestamp of when measurement was taken
                        val actualTimestamp = calculateHistoricalTimestamp(millisecondsAgo)

                        Log.d(
                            TAG,
                            "📊 Processing historical measurement: ${weightValue}kg taken ${millisecondsAgo}ms"
                        )
                        Log.d(
                            TAG, "🕒 Timestamp calculado: $actualTimestamp (${
                                java.text.SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm:ss",
                                    Locale.getDefault()
                                ).format(
                                    Date(actualTimestamp)
                                )
                            })"
                        )

                        batchHistoricalMeasurements.add(Pair(weightValue, actualTimestamp))
                    }

                    // Solo procesar si tenemos data nuevos for guardar
                    if (batchHistoricalMeasurements.isNotEmpty()) {
                        offlineDataCount++
                        Log.d(
                            TAG,
                            "📦 Lote $offlineDataCount procesado: ${batchHistoricalMeasurements.size} registros historical"
                        )
                        Log.d(
                            TAG,
                            "📈 Total acumulado: ${allHistoryData.size} mediciones de combustible"
                        )

                        // Save historical data from current batch to database
                        try {
                            val activeCylinder = getActiveCylinderUseCase.getActiveCylinderSync()
                            if (activeCylinder != null) {
                                // Save HISTORICAL/OFFLINE data to database
                                // Estos data provienen de OFFLINE_CHARACTERISTIC_UUID y se marcan como isHistorical = true
                                val result = saveFuelMeasurementUseCase.saveHistoricalMeasurements(
                                    cylinderId = activeCylinder.id,
                                    weightMeasurements = batchHistoricalMeasurements
                                )

                                result.fold(
                                    onSuccess = { savedCount ->
                                        Log.d(
                                            TAG,
                                            "✅ Lote $offlineDataCount saved: $savedCount mediciones de combustible"
                                        )

                                        // Crear FuelMeasurements for la UI
                                        val fuelMeasurements =
                                            batchHistoricalMeasurements.map { (weightValue, timestamp) ->
                                                val fuelKilograms =
                                                    maxOf(0f, weightValue - activeCylinder.tare)
                                                val fuelPercentage =
                                                    if (activeCylinder.capacity > 0) {
                                                        (fuelKilograms / activeCylinder.capacity * 100).coerceIn(
                                                            0f,
                                                            100f
                                                        )
                                                    } else {
                                                        0f
                                                    }

                                                FuelMeasurement(
                                                    cylinderId = activeCylinder.id,
                                                    cylinderName = activeCylinder.name,
                                                    timestamp = timestamp,
                                                    fuelKilograms = fuelKilograms,
                                                    fuelPercentage = fuelPercentage,
                                                    totalWeight = weightValue,
                                                    isCalibrated = true,
                                                    isHistorical = true
                                                )
                                            }

                                        allHistoryData.addAll(fuelMeasurements)

                                        // Updatesr UI con todos los data acumulados to ahora
                                        val sortedHistoryData =
                                            allHistoryData.sortedBy { it.timestamp }
                                        _historyData.value = sortedHistoryData
                                    },
                                    onFailure = { error ->
                                        Log.e(
                                            TAG,
                                            "❌ Error al guardar lote ${offlineDataCount}: ${error.message}"
                                        )
                                    }
                                )
                            } else {
                                Log.w(TAG, "No hay cylinder activa - data historical NO saveds")
                            }
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "❌ Error al procesar saved dthe batch ${offlineDataCount}: ${e.message}"
                            )
                        }

                        // Continue reading more data if we are in continuous reading mode and there are no duplicates
                        if (isReadingOfflineData) {
                            // Make small pause before requesting more data
                            delay(100) // 100ms de pausa entre readings
                            continueOfflineDataReading()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying timestamps duplicados: ${e.message}")
                    finishOfflineDataReading()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar data offline: ${e.message}")
            finishOfflineDataReading()
        }
    }

    fun connect(deviceAddress: String) {
        try {
            // Verificar permisos antes de conectar
            if (!bleManager.hasBluetoothConnectPermission()) {
                Log.e(TAG, "No hay permisos for conectar devices BLE")
                return
            }

            val device = bleManager.bluetoothAdapter?.getRemoteDevice(deviceAddress)
            device?.let {
                Log.d(TAG, "Connecting to sensor CamperGas: $deviceAddress")

                // Limpiar estado anterior si existe
                cleanup()

                @SuppressLint("MissingPermission")
                bluetoothGatt = it.connectGatt(context, false, gattCallback)
            } ?: run {
                Log.e(TAG, "No se pudo obtener el device remoto")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting: ${e.message}")
        }
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Starting disconnection from sensor CamperGas")
        Log.d(TAG, "🔌 Current connection state: ${_connectionState.value}")

        // Stop periodic reading
        stopPeriodicDataReading()

        // IMPORTANTE: Updatesr el estado inmediatamente
        // No esperar al callback porque a veces no se ejecuta
        _connectionState.value = false
        Log.d(TAG, "🔌 Connection state updated to: false")

        bluetoothGatt?.let { gatt ->
            // Verificar permisos antes de desconectar
            if (bleManager.hasBluetoothConnectPermission()) {
                Log.d(TAG, "🔌 Desconectando GATT...")
                @SuppressLint("MissingPermission")
                gatt.disconnect()

                // Short pause before closing
                serviceScope.launch {
                    delay(100)
                    if (bleManager.hasBluetoothConnectPermission()) {
                        @SuppressLint("MissingPermission")
                        gatt.close()
                    }
                    cleanup()
                }

                Log.d(TAG, "🔌 GATT desconectado")
            } else {
                Log.w(TAG, "🔌 No hay permisos for desconectar, forzando limpieza")
                cleanup()
            }
        } ?: run {
            Log.w(TAG, "🔌 bluetoothGatt es null, limpiando recursos")
            cleanup()
        }

        Log.d(TAG, "🔌 Disconnection completed - Final state: ${_connectionState.value}")
    }

    /**
     * Starts automatic offline data reading on connect
     */
    private fun startAutomaticOfflineDataReading() {
        offlineCharacteristic?.let { characteristic ->
            bluetoothGatt?.let { gatt ->
                // Verify permissions before requesting historical data
                if (!bleManager.hasBluetoothConnectPermission()) {
                    Log.e(TAG, "No permissions for automatic offline data reading")
                    return
                }

                Log.d(TAG, "🔄 Starting automatic offline data reading on connect...")

                // Inicializar la lectura continua of data offline
                startOfflineDataReading()

                // Start first reading after a pause
                serviceScope.launch {
                    delay(500) // Pause to stabilize connection
                    if (isReadingOfflineData) {
                        Log.d(TAG, "Ejecutando primera lectura of data offline...")
                        continueOfflineDataReading()
                    }
                }

            } ?: run {
                Log.e(TAG, "No GATT connection available for automatic reading")
            }
        } ?: run {
            Log.w(TAG, "Offline characteristic not available for automatic reading")
        }
    }

    private fun startOfflineDataReading() {
        isReadingOfflineData = true
        offlineDataCount = 0
        allHistoryData.clear()
        processedOfflineData.clear() // Limpiar data procesados anteriores
        _isLoadingHistory.value = true
        _historyData.value = emptyList()
        Log.d(TAG, "Iniciando lectura continua of data offline")
    }

    private fun continueOfflineDataReading() {
        if (!isReadingOfflineData) {
            Log.d(TAG, "Lectura offline cancelada")
            return
        }

        offlineCharacteristic?.let { characteristic ->
            bluetoothGatt?.let { gatt ->
                if (bleManager.hasBluetoothConnectPermission()) {
                    queueBleRead {
                        @SuppressLint("MissingPermission")
                        val success = gatt.readCharacteristic(characteristic)
                        if (!success) {
                            Log.e(TAG, "Error al continuar lectura of data historical")
                            finishOfflineDataReading()
                            isReadingInProgress = false
                            processNextReadingInQueue()
                        }
                    }
                } else {
                    Log.e(TAG, "No hay permisos for continuar lectura")
                    finishOfflineDataReading()
                }
            } ?: run {
                Log.e(TAG, "No connection GATT for continuar lectura")
                finishOfflineDataReading()
            }
        } ?: run {
            Log.e(TAG, "Offline characteristic not available to continue")
            finishOfflineDataReading()
        }
    }

    private fun finishOfflineDataReading() {
        isReadingOfflineData = false
        _isLoadingHistory.value = false

        Log.d(TAG, "Lectura of data offline completada")
        Log.d(TAG, "Total de lotes procesados: $offlineDataCount")
        Log.d(TAG, "Total de registros historical obtenidos: ${allHistoryData.size}")

        // Ordenar todos los data por timestamp final
        val sortedHistoryData = allHistoryData.sortedBy { it.timestamp }
        _historyData.value = sortedHistoryData

        if (allHistoryData.isNotEmpty()) {
            Log.d(TAG, "✅ Offline synchronization completed successfully")
            Log.d(
                TAG,
                "📊 Rango of data: ${allHistoryData.minOfOrNull { it.timestamp }} - ${allHistoryData.maxOfOrNull { it.timestamp }}"
            )
            Log.d(TAG, "🎯 Offline data synchronized automatically on connect")
        } else {
            Log.d(TAG, "ℹ️ No se encontraron data offline in the sensor")
        }
    }

    fun stopOfflineDataReading() {
        if (isReadingOfflineData) {
            Log.d(TAG, "Deteniendo lectura continua of data offline...")
            finishOfflineDataReading()
        }
    }

    private fun cleanup() {
        Log.d(TAG, "🧹 Iniciando limpieza de recursos BLE")

        // Stop offline reading if in progress
        stopOfflineDataReading()

        // Stop periodic reading if in progress
        stopPeriodicDataReading()

        bluetoothGatt = null
        fuelMeasurementCharacteristic = null
        inclinationCharacteristic = null
        offlineCharacteristic = null

        // Limpiar estado of the cola de readings BLE
        synchronized(readingQueue) {
            readingQueue.clear()
            isReadingInProgress = false
        }
        readingTimeoutJob?.cancel()
        readingTimeoutJob = null

        // Limpiar data when desconecta
        _fuelMeasurementData.value = null
        _fuelData.value = null
        _inclinationData.value = null
        processedOfflineData.clear() // Limpiar data procesados al desconectar

        Log.d(TAG, "🧹 Cleanup completed - Connection state: ${_connectionState.value}")
    }

    fun isConnected(): Boolean = _connectionState.value

    /**
     * Calculates real timestamp of a measurement based on how many milliseconds have passed
     * since measurement was taken until now
     */
    private fun calculateHistoricalTimestamp(millisecondsAgo: Long): Long {
        return System.currentTimeMillis() - millisecondsAgo
    }
}
