package com.example.campergas.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.campergas.R
import com.example.campergas.ui.theme.CamperGasTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Configuration activity for home screen widgets.
 *
 * Launched when user adds a CamperGas widget to their home screen.
 * Provides a simple configuration UI where users can confirm widget placement.
 * Handles widget ID validation and result reporting to the launcher.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure result as canceled by default
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If no valid ID, close
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CamperGasTheme {
                WidgetConfigScreen(
                    onConfirm = {
                        // Update widget and confirm configuration
                        val appWidgetManager =
                            AppWidgetManager.getInstance(this@WidgetConfigActivity)

                        // Determine widget type and update it
                        updateWidget(appWidgetManager)

                        // Configure successful result
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        finish()
                    }
                )
            }
        }
    }

    private fun updateWidget(appWidgetManager: AppWidgetManager) {
        // Try to update both widget types
        // In a real implementation, you could determine the specific type
        try {
            GasCylinderWidgetProvider().onUpdate(
                this,
                appWidgetManager,
                intArrayOf(appWidgetId)
            )
        } catch (_: Exception) {
            try {
                VehicleStabilityWidgetProvider().onUpdate(
                    this,
                    appWidgetManager,
                    intArrayOf(appWidgetId)
                )
            } catch (_: Exception) {
                // If both fail, at least try to update all widgets
                GasCylinderWidgetProvider.updateAllWidgets(this)
                VehicleStabilityWidgetProvider.updateAllWidgets(this)
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.widget_config_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.widget_config_ready_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Tap the widget to open the application\n• Tap the refresh button to request new data\n• The widget updates automatically when there are changes",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}