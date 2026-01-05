package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddGasCylinderUseCaseTest {

    private lateinit var useCase: AddGasCylinderUseCase
    private val repository: GasCylinderRepository = mockk()

    @Before
    fun setUp() {
        useCase = AddGasCylinderUseCase(repository)
    }

    @Test
    fun `invoke with valid parameters returns success with generated id`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 10.0f
        val setAsActive = false
        val expectedId = 123L

        coEvery { repository.insertCylinder(any()) } returns expectedId

        // Act
        val result = useCase(name, tare, capacity, setAsActive)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedId, result.getOrNull())

        coVerify {
            repository.insertCylinder(
                match { cylinder ->
                    cylinder.name == name.trim() &&
                            cylinder.tare == tare &&
                            cylinder.capacity == capacity &&
                            cylinder.isActive == setAsActive
                }
            )
        }
    }

    @Test
    fun `invoke with setAsActive true calls setActiveCylinder`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 10.0f
        val setAsActive = true
        val expectedId = 123L

        coEvery { repository.insertCylinder(any()) } returns expectedId
        coEvery { repository.setActiveCylinder(expectedId) } returns Unit

        // Act
        val result = useCase(name, tare, capacity, setAsActive)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedId, result.getOrNull())

        coVerify { repository.insertCylinder(any()) }
        coVerify { repository.setActiveCylinder(expectedId) }
    }

    @Test
    fun `invoke with setAsActive false does not call setActiveCylinder`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 10.0f
        val setAsActive = false
        val expectedId = 123L

        coEvery { repository.insertCylinder(any()) } returns expectedId

        // Act
        val result = useCase(name, tare, capacity, setAsActive)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedId, result.getOrNull())

        coVerify { repository.insertCylinder(any()) }
        coVerify(exactly = 0) { repository.setActiveCylinder(any()) }
    }

    @Test
    fun `invoke with blank name returns failure`() = runTest {
        // Arrange
        val name = "   "
        val tare = 5.0f
        val capacity = 10.0f

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Name cannot be empty", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke with empty name returns failure`() = runTest {
        // Arrange
        val name = ""
        val tare = 5.0f
        val capacity = 10.0f

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Name cannot be empty", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke with negative tare returns failure`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = -1.0f
        val capacity = 10.0f

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Tare cannot be negative", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke with zero capacity returns failure`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 0.0f

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Capacity must be greater than zero", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke with negative capacity returns failure`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = -5.0f

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Capacity must be greater than zero", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke trims name whitespace`() = runTest {
        // Arrange
        val name = "  Test Cylinder  "
        val tare = 5.0f
        val capacity = 10.0f
        val expectedId = 123L

        coEvery { repository.insertCylinder(any()) } returns expectedId

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isSuccess)

        coVerify {
            repository.insertCylinder(
                match { cylinder ->
                    cylinder.name == "Test Cylinder" // Trimmed
                }
            )
        }
    }

    @Test
    fun `invoke handles repository exception`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 10.0f
        val exception = RuntimeException("Database error")

        coEvery { repository.insertCylinder(any()) } throws exception

        // Act
        val result = useCase(name, tare, capacity)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())

        coVerify { repository.insertCylinder(any()) }
    }

    @Test
    fun `invoke handles setActiveCylinder exception`() = runTest {
        // Arrange
        val name = "Test Cylinder"
        val tare = 5.0f
        val capacity = 10.0f
        val setAsActive = true
        val expectedId = 123L
        val exception = RuntimeException("Database error")

        coEvery { repository.insertCylinder(any()) } returns expectedId
        coEvery { repository.setActiveCylinder(expectedId) } throws exception

        // Act
        val result = useCase(name, tare, capacity, setAsActive)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())

        coVerify { repository.insertCylinder(any()) }
        coVerify { repository.setActiveCylinder(expectedId) }
    }
}