package com.example.campergas

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.widget.WidgetUpdateManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Main application class for CamperGas.
 *
 * This class is the entry point of the application and is initialized when the application
 * starts. It extends Application and is annotated with @HiltAndroidApp to enable
 * Hilt dependency injection throughout the application.
 *
 * Responsibilities:
 * - Initialize the Hilt dependency injection container
 * - Maintain a reference to the widget update manager
 * - Provide application context throughout the lifecycle
 * - Configure the application language before creating activities
 *
 * @author Felipe García Gómez
 */
@HiltAndroidApp
class CamperGasApplication : Application() {

    /**
     * Widget update manager for the application.
     *
     * Injected via Hilt and used to coordinate updates of home screen widgets
     * with the latest gas cylinder and vehicle stability data.
     */
    @Inject
    lateinit var widgetUpdateManager: WidgetUpdateManager

    /**
     * Preferences DataStore for accessing user configuration settings.
     */
    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    override fun onCreate() {
        super.onCreate()

        // Configure the application language once at startup
        // This prevents activity recreation loops
        kotlinx.coroutines.runBlocking {
            val appLanguage = preferencesDataStore.appLanguage.first()
            AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleList())
        }
    }

}
