package com.example.campergas.widget

import com.example.campergas.data.repository.BleRepository
import com.example.campergas.data.repository.GasCylinderRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point interface for widget providers to access repositories.
 * This is needed because AppWidgetProvider doesn't support direct @Inject reliably.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun gasCylinderRepository(): GasCylinderRepository
    fun bleRepository(): BleRepository
}