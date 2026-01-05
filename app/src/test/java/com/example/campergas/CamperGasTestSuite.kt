package com.example.campergas

import com.example.campergas.domain.model.AppLanguageTest
import com.example.campergas.domain.model.FuelMeasurementTest
import com.example.campergas.domain.usecase.AddGasCylinderUseCaseTest
import com.example.campergas.domain.usecase.GetActiveCylinderUseCaseTest
import com.example.campergas.domain.usecase.GetConsumptionHistoryUseCaseTest
import com.example.campergas.domain.usecase.SaveFuelMeasurementUseCaseTest
import com.example.campergas.ui.SystemUIThemingTest
import com.example.campergas.ui.components.gas.GasCylinderViewModelTest
import com.example.campergas.ui.screens.bleconnect.BleConnectViewModelTest
import com.example.campergas.ui.screens.caravanconfig.CaravanConfigViewModelTest
import com.example.campergas.ui.screens.consumption.ConsumptionViewModelTest
import com.example.campergas.ui.screens.home.HomeViewModelTest
import com.example.campergas.ui.screens.inclination.InclinationViewModelTest
import com.example.campergas.ui.screens.settings.SettingsViewModelTest
import com.example.campergas.ui.screens.weight.WeightViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test Suite para toda la aplicación CamperGas
 *
 * Esta suite ejecuta todos los tests unitarios importantes de la aplicación:
 * - Tests de casos de uso (Use Cases)
 * - Tests de modelos de dominio
 * - Tests de ViewModels
 * - Tests de lógica de negocio
 */
@ExperimentalCoroutinesApi
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Use Cases Tests
    SaveFuelMeasurementUseCaseTest::class,
    GetActiveCylinderUseCaseTest::class,
    AddGasCylinderUseCaseTest::class,
    GetConsumptionHistoryUseCaseTest::class,
    // Domain Models Tests
    FuelMeasurementTest::class,
    AppLanguageTest::class,
    // ViewModels Tests
    GasCylinderViewModelTest::class,
    BleConnectViewModelTest::class,
    CaravanConfigViewModelTest::class,
    ConsumptionViewModelTest::class,
    HomeViewModelTest::class,
    InclinationViewModelTest::class,
    SettingsViewModelTest::class,
    WeightViewModelTest::class,
    SystemUIThemingTest::class
)
class CamperGasTestSuite {

    // This class acts as an entry point to run
    // all CamperGas application tests at once
}
