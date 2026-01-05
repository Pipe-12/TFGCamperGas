package com.example.campergas.ui.screens.settings

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.domain.model.AppLanguage
import com.example.campergas.domain.model.ThemeMode
import com.example.campergas.domain.usecase.ConfigureReadingIntervalsUseCase
import com.example.campergas.domain.usecase.DeleteNonActiveCylindersUseCase
import com.example.campergas.domain.usecase.GenerateTestDataUseCase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: SettingsViewModel
    private val preferencesDataStore: PreferencesDataStore = mockk(relaxed = true)
    private val configureReadingIntervalsUseCase: ConfigureReadingIntervalsUseCase =
        mockk(relaxed = true)
    private val generateTestDataUseCase: GenerateTestDataUseCase = mockk(relaxed = true)
    private val deleteNonActiveCylindersUseCase: DeleteNonActiveCylindersUseCase = 
        mockk(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    // Flows to simulate preferences
    private val themeModeFlow = MutableStateFlow(ThemeMode.DARK)
    private val notificationsEnabledFlow = MutableStateFlow(true)
    private val gasLevelThresholdFlow = MutableStateFlow(15.0f)
    private val appLanguageFlow = MutableStateFlow(AppLanguage.SYSTEM)
    private val weightIntervalFlow = MutableStateFlow(60) // 60 seconds = 1 minute
    private val inclinationIntervalFlow = MutableStateFlow(15) // 15 seconds

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        // Setup mock responses
        every { preferencesDataStore.themeMode } returns themeModeFlow
        every { preferencesDataStore.areNotificationsEnabled } returns notificationsEnabledFlow
    every { preferencesDataStore.gasLevelThreshold } returns gasLevelThresholdFlow
        every { preferencesDataStore.appLanguage } returns appLanguageFlow

        coEvery { preferencesDataStore.setThemeMode(any()) } coAnswers {
            themeModeFlow.value = firstArg()
        }

        coEvery { preferencesDataStore.setNotificationsEnabled(any()) } coAnswers {
            notificationsEnabledFlow.value = firstArg()
        }

        coEvery { preferencesDataStore.setGasLevelThreshold(any()) } coAnswers {
            gasLevelThresholdFlow.value = firstArg()
        }

        coEvery { preferencesDataStore.setAppLanguage(any()) } coAnswers {
            appLanguageFlow.value = firstArg()
        }

        every { configureReadingIntervalsUseCase.getWeightReadIntervalSeconds() } returns weightIntervalFlow
        every { configureReadingIntervalsUseCase.getInclinationReadIntervalSeconds() } returns inclinationIntervalFlow
        coEvery { configureReadingIntervalsUseCase.setWeightReadInterval(any()) } returns Unit
        coEvery { configureReadingIntervalsUseCase.setInclinationReadInterval(any()) } returns Unit

        viewModel = SettingsViewModel(
            preferencesDataStore,
            configureReadingIntervalsUseCase,
            generateTestDataUseCase,
            deleteNonActiveCylindersUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `initial state has default values from preferences`() = runTest {
        // Assert
        val state = viewModel.uiState.value
        assertEquals(ThemeMode.DARK, state.themeMode) // The mock returns DARK
        assertTrue(state.notificationsEnabled)
        assertEquals(15.0f, state.gasLevelThreshold, 0.01f)
        assertFalse(state.isLoading)
        assertNull(state.error)

        // Verify weight and inclination intervals
        assertEquals(1, viewModel.weightInterval.value) // 60s converted to 1 min
        assertEquals(15, viewModel.inclinationInterval.value)
    }



    @Test
    fun `setThemeMode updates preferences`() = runTest {
        // Act - Set to LIGHT
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setThemeMode(ThemeMode.LIGHT) }
        assertEquals(ThemeMode.LIGHT, themeModeFlow.value)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        // Act - Set to DARK
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setThemeMode(ThemeMode.DARK) }
        assertEquals(ThemeMode.DARK, themeModeFlow.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `setThemeMode supports SYSTEM for backward compatibility`() = runTest {
        // Although UI doesn't offer SYSTEM option, ViewModel should still support it
        // for backward compatibility with existing stored preferences
        
        // Act - Set to SYSTEM
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setThemeMode(ThemeMode.SYSTEM) }
        assertEquals(ThemeMode.SYSTEM, themeModeFlow.value)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `toggleNotifications inverts notification state`() = runTest {
        // Act - Toggle from true to false
        viewModel.toggleNotifications()
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setNotificationsEnabled(false) }
        assertFalse(viewModel.uiState.value.notificationsEnabled)

        // Act - Toggle back to true
        viewModel.toggleNotifications()
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setNotificationsEnabled(true) }
        assertTrue(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `setGasLevelThreshold updates preferences`() = runTest {
        // Act
        viewModel.setGasLevelThreshold(20.0f)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setGasLevelThreshold(20.0f) }
        assertEquals(20.0f, gasLevelThresholdFlow.value, 0.01f)
        assertEquals(20.0f, viewModel.uiState.value.gasLevelThreshold, 0.01f)
    }

    @Test
    fun `setWeightInterval converts minutes to seconds and updates use case`() = runTest {
        // Act
        viewModel.setWeightInterval(5) // 5 minutes

        // Assert - Check the status message is set before delay
        assertEquals("Weight interval configured: 5 min", viewModel.operationStatus.value)
        coVerify { configureReadingIntervalsUseCase.setWeightReadInterval(300) } // 5*60 = 300s

        // Verify message is cleared after delay
        advanceTimeBy(2100)
        assertNull(viewModel.operationStatus.value)
    }

    @Test
    fun `setInclinationInterval updates use case directly with seconds`() = runTest {
        // Act
        viewModel.setInclinationInterval(30) // 30 seconds

        // Assert - Check the status message is set before delay
        assertEquals("Inclination interval configured: 30s", viewModel.operationStatus.value)
        coVerify { configureReadingIntervalsUseCase.setInclinationReadInterval(30) }

        // Verify message is cleared after delay
        advanceTimeBy(2100)
        assertNull(viewModel.operationStatus.value)
    }

    @Test
    fun `setWeightInterval handles exceptions`() = runTest {
        // Arrange
        coEvery { configureReadingIntervalsUseCase.setWeightReadInterval(any()) } throws
                Exception("Connection error")

        // Act
        viewModel.setWeightInterval(5)

        // Assert - Check the error message is set before delay
        assertEquals(
            "Error configuring weight interval: Connection error",
            viewModel.operationStatus.value
        )

        // Verify message is cleared after delay
        advanceTimeBy(2100)
        assertNull(viewModel.operationStatus.value)
    }

    @Test
    fun `setInclinationInterval handles exceptions`() = runTest {
        // Arrange
        coEvery { configureReadingIntervalsUseCase.setInclinationReadInterval(any()) } throws
                Exception("Connection error")

        // Act
        viewModel.setInclinationInterval(10)

        // Assert - Check the error message is set before delay
        assertEquals(
            "Error configuring inclination interval: Connection error",
            viewModel.operationStatus.value
        )

        // Verify message is cleared after delay
        advanceTimeBy(2100)
        assertNull(viewModel.operationStatus.value)
    }

    @Test
    fun `setAppLanguage updates preferences`() = runTest {
        // Act - Set to English
        viewModel.setAppLanguage(AppLanguage.ENGLISH)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setAppLanguage(AppLanguage.ENGLISH) }
        assertEquals(AppLanguage.ENGLISH, appLanguageFlow.value)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)

        // Act - Set to Spanish
        viewModel.setAppLanguage(AppLanguage.SPANISH)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setAppLanguage(AppLanguage.SPANISH) }
        assertEquals(AppLanguage.SPANISH, appLanguageFlow.value)
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.language)

        // Act - Set to Catalan
        viewModel.setAppLanguage(AppLanguage.CATALAN)
        advanceUntilIdle()

        // Assert
        coVerify { preferencesDataStore.setAppLanguage(AppLanguage.CATALAN) }
        assertEquals(AppLanguage.CATALAN, appLanguageFlow.value)
        assertEquals(AppLanguage.CATALAN, viewModel.uiState.value.language)
    }

    @Test
    fun `initial language state is loaded from preferences`() = runTest {
        // Assert - Initial state should have SYSTEM language
        assertEquals(AppLanguage.SYSTEM, viewModel.uiState.value.language)

        // Change the flow value
        appLanguageFlow.value = AppLanguage.ENGLISH
        advanceUntilIdle()

        // Assert - UI state should update to English
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.language)
    }
}
