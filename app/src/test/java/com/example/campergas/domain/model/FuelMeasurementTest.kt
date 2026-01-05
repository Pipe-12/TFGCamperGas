package com.example.campergas.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FuelMeasurementTest {

    @Test
    fun `getFormattedFuelKilograms returns correctly formatted string`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.456f,
            fuelPercentage = 50.0f,
            totalWeight = 12.456f,
            isCalibrated = true
        )

        // Act
        val formatted = measurement.getFormattedFuelKilograms()

        // Assert
        assertEquals("7.46 kg", formatted)
    }

    @Test
    fun `getFormattedPercentage returns correctly formatted string`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.456f,
            fuelPercentage = 74.56f,
            totalWeight = 12.456f,
            isCalibrated = true
        )

        // Act
        val formatted = measurement.getFormattedPercentage()

        // Assert
        assertEquals("74.6%", formatted)
    }

    @Test
    fun `getFormattedTotalWeight returns correctly formatted string`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.456f,
            fuelPercentage = 74.56f,
            totalWeight = 12.456f,
            isCalibrated = true
        )

        // Act
        val formatted = measurement.getFormattedTotalWeight()

        // Assert
        assertEquals("12.46 kg", formatted)
    }

    @Test
    fun `getFormattedTimestamp returns time in HH_mm_ss format`() {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.set(2023, 11, 25, 14, 30, 45) // December 25, 2023, 14:30:45
        val timestamp = calendar.timeInMillis

        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = timestamp,
            fuelKilograms = 7.456f,
            fuelPercentage = 74.56f,
            totalWeight = 12.456f,
            isCalibrated = true
        )

        // Act
        val formatted = measurement.getFormattedTimestamp()

        // Assert
        assertEquals("14:30:45", formatted)
    }

    @Test
    fun `isValid returns true for valid measurement`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.5f,
            fuelPercentage = 75.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertTrue(isValid)
    }

    @Test
    fun `isValid returns false for NaN fuel kilograms`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = Float.NaN,
            fuelPercentage = 75.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for infinite fuel kilograms`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = Float.POSITIVE_INFINITY,
            fuelPercentage = 75.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for negative fuel kilograms`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = -1.0f,
            fuelPercentage = 75.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for NaN fuel percentage`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.5f,
            fuelPercentage = Float.NaN,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for infinite fuel percentage`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.5f,
            fuelPercentage = Float.POSITIVE_INFINITY,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for negative fuel percentage`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.5f,
            fuelPercentage = -5.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns false for fuel percentage over 100`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 7.5f,
            fuelPercentage = 105.0f,
            totalWeight = 12.5f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `isValid returns true for zero fuel kilograms`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 0.0f,
            fuelPercentage = 0.0f,
            totalWeight = 5.0f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertTrue(isValid)
    }

    @Test
    fun `isValid returns true for 100 percent fuel`() {
        // Arrange
        val measurement = FuelMeasurement(
            id = 1L,
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 10.0f,
            fuelPercentage = 100.0f,
            totalWeight = 15.0f,
            isCalibrated = true
        )

        // Act
        val isValid = measurement.isValid()

        // Assert
        assertTrue(isValid)
    }
}