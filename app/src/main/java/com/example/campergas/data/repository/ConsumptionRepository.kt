package com.example.campergas.data.repository

import com.example.campergas.domain.model.Consumption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing fuel consumption data.
 *
 * This repository acts as an abstraction layer over FuelMeasurementRepository,
 * transforming fuel measurements (FuelMeasurement) to the simpler domain model
 * of Consumption, which focuses specifically on consumption data for display
 * and analysis.
 *
 * Responsibilities:
 * - Provide access to consumption records in different formats (all, by cylinder, by date range)
 * - Convert between FuelMeasurement and Consumption
 * - Filter and sort consumption data
 *
 * The conversion from FuelMeasurement to Consumption simplifies the data,
 * extracting only the relevant information for consumption analysis.
 *
 * @property fuelMeasurementRepository Source repository for fuel measurements
 * @author Felipe García Gómez
 */
@Singleton
class ConsumptionRepository @Inject constructor(
    private val fuelMeasurementRepository: FuelMeasurementRepository
) {

    /**
     * Gets all consumption records ordered by descending date.
     *
     * Retrieves all fuel measurements and converts them to the
     * Consumption model for analysis and historical consumption display.
     *
     * @return Flow that emits the complete list of consumption records
     */
    fun getAllConsumptions(): Flow<List<Consumption>> {
        return fuelMeasurementRepository.getAllMeasurements().map { measurements ->
            measurements.map { Consumption.fromFuelMeasurement(it) }
        }
    }

    /**
     * Gets consumption records within a date range.
     *
     * Filters measurements to show only those within the specified
     * period, useful for consumption analysis by time periods.
     *
     * @param startDate Unix timestamp of the period start
     * @param endDate Unix timestamp of the period end
     * @return Flow that emits the list of records within the date range
     */
    fun getConsumptionsByDateRange(startDate: Long, endDate: Long): Flow<List<Consumption>> {
        return fuelMeasurementRepository.getMeasurementsByTimeRange(startDate, endDate)
            .map { measurements ->
                measurements.map { Consumption.fromFuelMeasurement(it) }
            }
    }

}
