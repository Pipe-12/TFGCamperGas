package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.FuelMeasurementRepository
import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.FuelMeasurement
import com.example.campergas.domain.model.GasCylinder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GenerateTestDataUseCaseTest {

    private lateinit var useCase: GenerateTestDataUseCase
    private val fuelMeasurementRepository: FuelMeasurementRepository = mockk(relaxed = true)
    private val gasCylinderRepository: GasCylinderRepository = mockk(relaxed = true)

    companion object {
        /** 30 days in milliseconds for test timestamp validation */
        private val THIRTY_DAYS_IN_MILLIS = TimeUnit.DAYS.toMillis(30)
    }

    @Before
    fun setUp() {
        useCase = GenerateTestDataUseCase(fuelMeasurementRepository, gasCylinderRepository)
    }

    @Test
    fun `invoke creates a new test cylinder with prefix in name`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val cylinderSlot = slot<GasCylinder>()
        
        coEvery { gasCylinderRepository.insertCylinder(capture(cylinderSlot)) } returns testCylinderId

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        
        val capturedCylinder = cylinderSlot.captured
        assertTrue(
            "Cylinder name should start with test prefix",
            capturedCylinder.name.startsWith(GenerateTestDataUseCase.TEST_CYLINDER_NAME_PREFIX)
        )
        assertFalse(
            "Test cylinder should NOT be active",
            capturedCylinder.isActive
        )
    }

    @Test
    fun `invoke creates test cylinder that is NOT active`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val cylinderSlot = slot<GasCylinder>()
        
        coEvery { gasCylinderRepository.insertCylinder(capture(cylinderSlot)) } returns testCylinderId

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(
            "Test cylinder should NOT be active to avoid mixing with real data",
            cylinderSlot.captured.isActive
        )
    }

    @Test
    fun `invoke generates 100 measurements`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val measurementsSlot = slot<List<FuelMeasurement>>()
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId
        coEvery { fuelMeasurementRepository.insertMeasurements(capture(measurementsSlot)) } returns Unit

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
        assertEquals(100, measurementsSlot.captured.size)
    }

    @Test
    fun `invoke creates measurements associated with new test cylinder`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val measurementsSlot = slot<List<FuelMeasurement>>()
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId
        coEvery { fuelMeasurementRepository.insertMeasurements(capture(measurementsSlot)) } returns Unit

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        
        // All measurements should be associated with the test cylinder
        measurementsSlot.captured.forEach { measurement ->
            assertEquals(
                "All measurements should be associated with the test cylinder",
                testCylinderId,
                measurement.cylinderId
            )
            assertTrue(
                "Measurement cylinder name should contain test prefix",
                measurement.cylinderName.startsWith(GenerateTestDataUseCase.TEST_CYLINDER_NAME_PREFIX)
            )
        }
    }

    @Test
    fun `invoke creates measurements marked as historical`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val measurementsSlot = slot<List<FuelMeasurement>>()
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId
        coEvery { fuelMeasurementRepository.insertMeasurements(capture(measurementsSlot)) } returns Unit

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        
        // All measurements should be marked as historical
        measurementsSlot.captured.forEach { measurement ->
            assertTrue(
                "All measurements should be marked as historical",
                measurement.isHistorical
            )
        }
    }

    @Test
    fun `invoke creates measurements with timestamps over last 30 days`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val measurementsSlot = slot<List<FuelMeasurement>>()
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - THIRTY_DAYS_IN_MILLIS
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId
        coEvery { fuelMeasurementRepository.insertMeasurements(capture(measurementsSlot)) } returns Unit

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        
        // Check timestamps are within expected range
        measurementsSlot.captured.forEach { measurement ->
            assertTrue(
                "Measurement timestamp should be after 30 days ago",
                measurement.timestamp >= thirtyDaysAgo - 1000 // Small tolerance
            )
            assertTrue(
                "Measurement timestamp should be before now + small tolerance",
                measurement.timestamp <= now + 1000 // Small tolerance
            )
        }
    }

    @Test
    fun `invoke handles repository exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Database error")
        coEvery { gasCylinderRepository.insertCylinder(any()) } throws exception

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke handles measurement insertion exception`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val exception = RuntimeException("Database error")
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId
        coEvery { fuelMeasurementRepository.insertMeasurements(any()) } throws exception

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `invoke does NOT require active cylinder to exist`() = runTest {
        // Arrange - No active cylinder setup needed
        val testCylinderId = 99L
        
        coEvery { gasCylinderRepository.insertCylinder(any()) } returns testCylinderId

        // Act
        val result = useCase()

        // Assert
        assertTrue(
            "Should succeed without needing an active cylinder",
            result.isSuccess
        )
        assertEquals(100, result.getOrNull())
        
        // Verify that getActiveCylinder was NOT called
        coVerify(exactly = 0) { gasCylinderRepository.getActiveCylinder() }
    }

    @Test
    fun `test cylinder has expected default parameters`() = runTest {
        // Arrange
        val testCylinderId = 99L
        val cylinderSlot = slot<GasCylinder>()
        
        coEvery { gasCylinderRepository.insertCylinder(capture(cylinderSlot)) } returns testCylinderId

        // Act
        val result = useCase()

        // Assert
        assertTrue(result.isSuccess)
        
        val capturedCylinder = cylinderSlot.captured
        assertEquals(12.5f, capturedCylinder.capacity, 0.01f)
        assertEquals(14.0f, capturedCylinder.tare, 0.01f)
    }
}
