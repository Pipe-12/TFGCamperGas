package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.GasCylinder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetActiveCylinderUseCaseTest {

    private lateinit var useCase: GetActiveCylinderUseCase
    private val repository: GasCylinderRepository = mockk()

    @Before
    fun setUp() {
        useCase = GetActiveCylinderUseCase(repository)
    }

    @Test
    fun `invoke returns flow from repository`() {
        // Arrange
        val testCylinder = GasCylinder(
            id = 1L,
            name = "Test Cylinder",
            tare = 5.0f,
            capacity = 10.0f,
            isActive = true
        )
        val flow = flowOf(testCylinder)
        every { repository.getActiveCylinder() } returns flow

        // Act
        val result = useCase()

        // Assert
        assertEquals(flow, result)
        verify { repository.getActiveCylinder() }
    }

    @Test
    fun `invoke returns flow with null when no active cylinder`() {
        // Arrange
        val flow = flowOf<GasCylinder?>(null)
        every { repository.getActiveCylinder() } returns flow

        // Act
        val result = useCase()

        // Assert
        assertEquals(flow, result)
        verify { repository.getActiveCylinder() }
    }

    @Test
    fun `getActiveCylinderSync returns cylinder from repository`() = runTest {
        // Arrange
        val testCylinder = GasCylinder(
            id = 1L,
            name = "Test Cylinder",
            tare = 5.0f,
            capacity = 10.0f,
            isActive = true
        )
        coEvery { repository.getActiveCylinderSync() } returns testCylinder

        // Act
        val result = useCase.getActiveCylinderSync()

        // Assert
        assertEquals(testCylinder, result)
        coVerify { repository.getActiveCylinderSync() }
    }

    @Test
    fun `getActiveCylinderSync returns null when no active cylinder`() = runTest {
        // Arrange
        coEvery { repository.getActiveCylinderSync() } returns null

        // Act
        val result = useCase.getActiveCylinderSync()

        // Assert
        assertNull(result)
        coVerify { repository.getActiveCylinderSync() }
    }

    @Test
    fun `getActiveCylinderSync handles repository exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Database error")
        coEvery { repository.getActiveCylinderSync() } throws exception

        // Act & Assert
        try {
            useCase.getActiveCylinderSync()
            // Should not reach here
            throw AssertionError("Expected exception to be thrown")
        } catch (e: Exception) {
            assertEquals(exception, e)
        }

        coVerify { repository.getActiveCylinderSync() }
    }
}