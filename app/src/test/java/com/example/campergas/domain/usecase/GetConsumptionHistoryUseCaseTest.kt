package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.ConsumptionRepository
import com.example.campergas.domain.model.Consumption
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetConsumptionHistoryUseCaseTest {

    private lateinit var consumptionRepository: ConsumptionRepository
    private lateinit var useCase: GetConsumptionHistoryUseCase

    @Before
    fun setUp() {
        consumptionRepository = mockk()
        useCase = GetConsumptionHistoryUseCase(consumptionRepository)
    }

    @Test
    fun `calculateTotalConsumption returns zero for empty list`() {
        // Arrange
        val consumptions = emptyList<Consumption>()

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `calculateTotalConsumption returns zero for single measurement`() {
        // Arrange
        val consumptions = listOf(
            createTestConsumption(id = 1, fuelKilograms = 10f, date = 1000L)
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun `calculateTotalConsumption returns positive value for normal consumption`() {
        // Arrange - Consumption scenario: 10kg -> 8kg (2kg consumed)
        val consumptions = listOf(
            createTestConsumption(id = 1, fuelKilograms = 10f, date = 1000L), // Older measurement
            createTestConsumption(id = 2, fuelKilograms = 8f, date = 2000L)   // Newer measurement
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert
        assertEquals(2f, result, 0.01f) // 10 - 8 = 2 kg consumed
    }

    @Test
    fun `calculateTotalConsumption should not return negative value for refill scenario`() {
        // Arrange - Refill scenario: 5kg -> 15kg (cylinder was refilled)
        // This currently produces negative consumption but should not
        val consumptions = listOf(
            createTestConsumption(
                id = 1,
                fuelKilograms = 5f,
                date = 1000L
            ),  // Older measurement (low fuel)
            createTestConsumption(
                id = 2,
                fuelKilograms = 15f,
                date = 2000L
            )  // Newer measurement (refilled)
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert - Currently this will be negative (-10f), but it should be 0f or positive
        // This test demonstrates the bug we need to fix
        assertTrue("Consumption should not be negative, but was: $result", result >= 0f)
    }

    @Test
    fun `calculateTotalConsumption handles multiple cylinders correctly`() {
        // Arrange - Two cylinders with different consumption patterns
        val consumptions = listOf(
            // Cylinder 1: normal consumption (10kg -> 8kg = 2kg consumed)
            createTestConsumption(id = 1, cylinderId = 1L, fuelKilograms = 10f, date = 1000L),
            createTestConsumption(id = 2, cylinderId = 1L, fuelKilograms = 8f, date = 2000L),
            // Cylinder 2: normal consumption (15kg -> 12kg = 3kg consumed)
            createTestConsumption(id = 3, cylinderId = 2L, fuelKilograms = 15f, date = 1500L),
            createTestConsumption(id = 4, cylinderId = 2L, fuelKilograms = 12f, date = 2500L)
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert
        assertEquals(5f, result, 0.01f) // 2kg + 3kg = 5kg total consumed
    }

    @Test
    fun `calculateTotalConsumption handles cylinder with refill scenario`() {
        // Arrange - One cylinder with refill (should not contribute negative consumption)
        val consumptions = listOf(
            // Cylinder 1: normal consumption (10kg -> 8kg = 2kg consumed)
            createTestConsumption(id = 1, cylinderId = 1L, fuelKilograms = 10f, date = 1000L),
            createTestConsumption(id = 2, cylinderId = 1L, fuelKilograms = 8f, date = 2000L),
            // Cylinder 2: refill scenario (5kg -> 15kg = should be 0kg, not -10kg)
            createTestConsumption(id = 3, cylinderId = 2L, fuelKilograms = 5f, date = 1500L),
            createTestConsumption(id = 4, cylinderId = 2L, fuelKilograms = 15f, date = 2500L)
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert - Total should be 2kg (only from cylinder 1), not -8kg
        assertTrue(
            "Total consumption should not be negative due to refill, but was: $result",
            result >= 0f
        )
        assertEquals(2f, result, 0.01f) // Only cylinder 1's consumption should count
    }

    @Test
    fun `calculateTotalConsumption handles intermediate refill correctly`() {
        // Arrange - Main improvement scenario: Consumption before and after a refill
        // should sum all consumption, not just first-to-last difference
        val consumptions = listOf(
            // Day 1: Start with 10kg
            createTestConsumption(id = 1, fuelKilograms = 10f, date = 1000L),
            // Day 2: 7kg (3kg consumed)
            createTestConsumption(id = 2, fuelKilograms = 7f, date = 2000L),
            // Day 3: 3kg (4kg consumed)
            createTestConsumption(id = 3, fuelKilograms = 3f, date = 3000L),
            // Day 4: REFILL to 10kg (increase should be ignored)
            createTestConsumption(id = 4, fuelKilograms = 10f, date = 4000L),
            // Day 5: 8kg (2kg consumed)
            createTestConsumption(id = 5, fuelKilograms = 8f, date = 5000L),
        )

        // Act
        val result = useCase.calculateTotalConsumption(consumptions)

        // Assert
        // With improved method: 3 + 4 + 0 (refill) + 2 = 9kg total consumed
        // Old method would calculate: 10 - 8 = 2kg (incorrect!)
        assertEquals(9f, result, 0.01f)
    }

    @Test
    fun `prepareChartData should not include negative consumption values`() {
        // Arrange - Mix of normal consumption and refill scenarios
        val consumptions = listOf(
            // Day 1: Normal consumption (10kg -> 8kg = 2kg consumed)
            createTestConsumption(id = 1, fuelKilograms = 10f, date = 86400000L), // Day 1
            createTestConsumption(
                id = 2,
                fuelKilograms = 8f,
                date = 86400000L + 1000
            ), // Day 1 (slightly later)
            // Day 2: Refill scenario (5kg -> 15kg = should be 0kg, not -10kg)
            createTestConsumption(id = 3, fuelKilograms = 5f, date = 172800000L), // Day 2
            createTestConsumption(
                id = 4,
                fuelKilograms = 15f,
                date = 172800000L + 1000
            ) // Day 2 (slightly later)
        )

        // Act
        val chartData = useCase.prepareChartData(consumptions)

        // Assert
        assertEquals(2, chartData.size)
        chartData.forEach { dataPoint ->
            assertTrue(
                "Chart data should not contain negative consumption values, but found: ${dataPoint.kilograms}",
                dataPoint.kilograms >= 0f
            )
        }
    }

    @Test
    fun `integration test - realistic consumption scenario over time`() {
        // Arrange - Realistic scenario with mixed consumption and refill patterns
        val consumptions = listOf(
            // Week 1: Start with full cylinder
            createTestConsumption(id = 1, fuelKilograms = 15f, date = 0L),
            createTestConsumption(
                id = 2,
                fuelKilograms = 12f,
                date = 86400000L
            ), // Day 1: 3kg consumed
            createTestConsumption(
                id = 3,
                fuelKilograms = 9f,
                date = 172800000L
            ),  // Day 2: 3kg consumed
            createTestConsumption(
                id = 4,
                fuelKilograms = 6f,
                date = 259200000L
            ),  // Day 3: 3kg consumed
            createTestConsumption(
                id = 5,
                fuelKilograms = 3f,
                date = 345600000L
            ),  // Day 4: 3kg consumed

            // Week 2: Refill the cylinder
            createTestConsumption(
                id = 6,
                fuelKilograms = 15f,
                date = 432000000L
            ), // Day 5: REFILL (should not count as negative)
            createTestConsumption(
                id = 7,
                fuelKilograms = 13f,
                date = 518400000L
            ), // Day 6: 2kg consumed
            createTestConsumption(
                id = 8,
                fuelKilograms = 11f,
                date = 604800000L
            ), // Day 7: 2kg consumed
        )

        // Act
        val totalConsumption = useCase.calculateTotalConsumption(consumptions)
        val chartData = useCase.prepareChartData(consumptions)

        // Assert
        // Improved calculation sums all decrements:
        // 15->12=3, 12->9=3, 9->6=3, 6->3=3, 3->15=0 (refill ignored), 15->13=2, 13->11=2
        // Total = 3+3+3+3+0+2+2 = 16kg
        assertEquals(16f, totalConsumption, 0.01f)

        // Chart data should have no negative values
        chartData.forEach { dataPoint ->
            assertTrue(
                "Chart data should not contain negative values: ${dataPoint.kilograms}",
                dataPoint.kilograms >= 0f
            )
        }

        // Should have appropriate number of chart points (days with data)
        assertTrue("Should have chart data for multiple days", chartData.size >= 5)
    }

    private fun createTestConsumption(
        id: Long = 1,
        cylinderId: Long = 1,
        fuelKilograms: Float = 10f,
        date: Long = 1000L
    ) = Consumption(
        id = id,
        cylinderId = cylinderId,
        cylinderName = "Test Cylinder $cylinderId",
        date = date,
        fuelKilograms = fuelKilograms,
        fuelPercentage = 75f,
        totalWeight = 15f,
        isCalibrated = true,
        isHistorical = false
    )
}