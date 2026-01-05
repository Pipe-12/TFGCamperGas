package com.example.campergas.ui.screens.consumption

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.Consumption
import com.example.campergas.domain.usecase.ChartDataPoint
import com.example.campergas.domain.usecase.GetConsumptionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ConsumptionViewModel @Inject constructor(
    private val getConsumptionHistoryUseCase: GetConsumptionHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsumptionUiState())
    val uiState: StateFlow<ConsumptionUiState> = _uiState.asStateFlow()

    // Job for cancelar la corrutina de carga whena necesario
    private var loadingJob: Job? = null

    init {
        loadConsumptionHistory()
        loadConsumptionSummaries()
    }

    private fun loadConsumptionHistory() {
        // Cancelar job anterior si existe
        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentState = _uiState.value
                val startDate = currentState.startDate
                val endDate = currentState.endDate

                getConsumptionHistoryUseCase(startDate, endDate).collect { consumptions ->
                    _uiState.value = _uiState.value.copy(
                        consumptions = consumptions,
                        isLoading = false,
                        error = null
                    )

                    // Update chart data for current period
                    updateChartData(consumptions)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setDateRange(startDate: Long?, endDate: Long?) {
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            endDate = endDate
        )
        loadConsumptionHistory()
        updateCustomPeriodSummary(startDate, endDate)
    }

    fun clearDateFilter() {
        _uiState.value = _uiState.value.copy(
            startDate = null,
            endDate = null
        )
        loadConsumptionHistory()
    }

    fun setLastWeekFilter() {
        setDateRangeFromCalendar(Calendar.DAY_OF_YEAR, -7)
    }

    fun setLastMonthFilter() {
        setDateRangeFromCalendar(Calendar.MONTH, -1)
    }

    fun setLastDayFilter() {
        setDateRangeFromCalendar(Calendar.DAY_OF_YEAR, -1)
    }

    /**
     * Helper method to reduce code duplication en filtros de date
     */
    private fun setDateRangeFromCalendar(calendarField: Int, amount: Int) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.add(calendarField, amount)
        val startDate = calendar.timeInMillis

        setDateRange(startDate, endDate)
    }

    private fun loadConsumptionSummaries() {
        viewModelScope.launch {
            try {
                // Load last day summary
                getConsumptionHistoryUseCase.getLastDayConsumption().collect { dayConsumptions ->
                    val dayTotal =
                        getConsumptionHistoryUseCase.calculateTotalConsumption(dayConsumptions)
                    _uiState.value = _uiState.value.copy(lastDayConsumption = dayTotal)
                }
            } catch (_: Exception) {
                // Silently handle summary loading errors
            }
        }

        viewModelScope.launch {
            try {
                // Load last week summary
                getConsumptionHistoryUseCase.getLastWeekConsumption().collect { weekConsumptions ->
                    val weekTotal =
                        getConsumptionHistoryUseCase.calculateTotalConsumption(weekConsumptions)
                    _uiState.value = _uiState.value.copy(lastWeekConsumption = weekTotal)
                }
            } catch (_: Exception) {
                // Silently handle summary loading errors
            }
        }

        viewModelScope.launch {
            try {
                // Load last month summary
                getConsumptionHistoryUseCase.getLastMonthConsumption()
                    .collect { monthConsumptions ->
                        val monthTotal =
                            getConsumptionHistoryUseCase.calculateTotalConsumption(monthConsumptions)
                        _uiState.value = _uiState.value.copy(lastMonthConsumption = monthTotal)
                    }
            } catch (_: Exception) {
                // Silently handle summary loading errors
            }
        }
    }

    private fun updateCustomPeriodSummary(startDate: Long?, endDate: Long?) {
        if (startDate != null && endDate != null) {
            viewModelScope.launch {
                try {
                    getConsumptionHistoryUseCase(startDate, endDate).collect { consumptions ->
                        val customTotal =
                            getConsumptionHistoryUseCase.calculateTotalConsumption(consumptions)
                        _uiState.value = _uiState.value.copy(customPeriodConsumption = customTotal)
                    }
                } catch (_: Exception) {
                    // Silently handle custom period summary errors
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(customPeriodConsumption = 0f)
        }
    }

    private fun updateChartData(consumptions: List<Consumption>) {
        val chartData = getConsumptionHistoryUseCase.prepareChartData(consumptions)
        _uiState.value = _uiState.value.copy(chartData = chartData)
    }

    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
    }
}

data class ConsumptionUiState(
    val consumptions: List<Consumption> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    // Consumption summaries
    val lastDayConsumption: Float = 0f,
    val lastWeekConsumption: Float = 0f,
    val lastMonthConsumption: Float = 0f,
    val customPeriodConsumption: Float = 0f,
    // Chart data
    val chartData: List<ChartDataPoint> = emptyList()
)
