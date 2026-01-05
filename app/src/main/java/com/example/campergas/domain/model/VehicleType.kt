package com.example.campergas.domain.model

/**
 * Types of recreational vehicles supported by the application.
 *
 * This enumeration defines the types of vehicles that can be configured
 * in CamperGas for gas cylinder and stability monitoring.
 *
 * Available types:
 * - CARAVAN: Towed caravan (trailer) without its own engine
 * - AUTOCARAVANA: Motorized vehicle with integrated living space (motorhome)
 *
 * The vehicle type can affect:
 * - Gas consumption calculations
 * - Stability and leveling configuration
 * - Recommended sensor placement
 * - Wheel elevation calculations
 *
 * @author Felipe García Gómez
 */
enum class VehicleType {
    /** Towed caravan without its own engine */
    CARAVAN,

    /** Motorized motorhome with integrated living space */
    AUTOCARAVANA
}
