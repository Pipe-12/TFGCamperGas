package com.example.campergas.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Domain model and database entity representing a gas cylinder.
 *
 * This data class represents an LPG (Liquefied Petroleum Gas) cylinder used
 * in caravans and motorhomes. It stores both the physical characteristics of the cylinder
 * (empty weight, capacity) and its state in the system (active/inactive).
 *
 * Key functionality:
 * - Only one cylinder can be active at a time in the system
 * - The active cylinder is used for current weight measurements
 * - The tare (empty weight) is used to calculate available gas by subtracting from total weight
 * - The capacity defines 100% gas for percentage calculations
 *
 * Use cases:
 * - Managing multiple cylinders (butane, propane, different sizes)
 * - Switching between cylinders without losing measurement history
 * - Precise gas calculation through tare calibration
 *
 * Persisted in Room database with table name "gas_cylinders".
 *
 * @property id Auto-generated unique identifier of the cylinder in the database
 * @property name Descriptive name of the cylinder (e.g., "Butane 12.5kg", "Propane 11kg")
 * @property tare Weight of the empty cylinder in kilograms (subtracted from total weight to get available gas)
 * @property capacity Maximum gas capacity of the cylinder in kilograms
 * @property isActive Indicates if this cylinder is currently in use (only one can be active)
 * @property createdAt Unix timestamp (milliseconds) of when the cylinder record was created
 * @author Felipe García Gómez
 */
@Entity(tableName = "gas_cylinders")
data class GasCylinder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tare: Float,
    val capacity: Float,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
