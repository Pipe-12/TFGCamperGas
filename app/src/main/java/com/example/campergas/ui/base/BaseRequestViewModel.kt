package com.example.campergas.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for manejar peticiones manuales con cooldown y control de spam
 * Centralizes common logic of ViewModels that make on-demand BLE requests
 */
abstract class BaseRequestViewModel(
    private val checkBleConnectionUseCase: CheckBleConnectionUseCase
) : ViewModel() {

    // Control de peticiones for evitar spam
    private var lastRequestTime = 0L
    private val requestCooldownMs = 2000L // 2 segundos entre peticiones

    private val _isRequestingData = MutableStateFlow(false)
    val isRequestingData: StateFlow<Boolean> = _isRequestingData

    /**
     * Executes a manual request with spam protection
     * @form requestAction The specific action to execute (use case specific)
     * @form logTag Tag for los logs de debug
     * @form dataTypeDescription Description of data type for logs
     */
    protected fun executeManualRequest(
        requestAction: () -> Unit,
        logTag: String,
        dataTypeDescription: String
    ) {
        val currentTime = System.currentTimeMillis()

        // Verify if enough time has passed since the last request
        if (currentTime - lastRequestTime < requestCooldownMs) {
            android.util.Log.d(logTag, "⏱️ Request blocked - cooldown active")
            return
        }

        // Verify if there is already a request in progress
        if (_isRequestingData.value) {
            android.util.Log.d(logTag, "⏱️ Request blocked - one already in progress")
            return
        }

        android.util.Log.d(logTag, "📊 Requesting data de $dataTypeDescription manualmente")
        _isRequestingData.value = true
        lastRequestTime = currentTime

        requestAction()

        // Reset state after a reasonable time
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // 1.5 segundos
            _isRequestingData.value = false
        }
    }

    /**
     * Verifies if there is an active BLE connection
     */
    fun isConnected(): Boolean {
        return checkBleConnectionUseCase.isConnected()
    }

    /**
     * Verifies if a new request can be made (not in cooldown)
     */
    fun canMakeRequest(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastRequestTime >= requestCooldownMs) && !_isRequestingData.value
    }
}