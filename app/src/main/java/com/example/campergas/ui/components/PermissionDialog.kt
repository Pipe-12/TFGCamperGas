package com.example.campergas.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.example.campergas.R

/**
 * Reusable permission request dialog component.
 *
 * Displays a customizable alert dialog with icon, title, message, and action buttons.
 * Used throughout the app for permission requests, confirmations, and important notifications.
 *
 * @param title Dialog title text
 * @param message Dialog message/description text
 * @param icon Icon to display at the top of the dialog, defaults to Warning icon
 * @param onAccept Callback invoked when user accepts/confirms
 * @param onDismiss Callback invoked when user dismisses/cancels
 * @param acceptText Text for the accept/confirm button, defaults to localized "Accept"
 * @param dismissText Text for the dismiss/cancel button, defaults to localized "Cancel"
 * @param dismissible Whether dialog can be dismissed by back press or outside click, defaults to true
 */
@Composable
fun PermissionDialog(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Warning,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    acceptText: String = stringResource(R.string.dialog_accept),
    dismissText: String = stringResource(R.string.dialog_cancel),
    dismissible: Boolean = true
) {
    AlertDialog(
        onDismissRequest = if (dismissible) onDismiss else {
            {}
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onAccept
            ) {
                Text(acceptText)
            }
        },
        dismissButton = if (dismissible) {
            @Composable {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(dismissText)
                }
            }
        } else null,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible
        )
    )
}

/**
 * Specialized dialog for Bluetooth disabled notification.
 *
 * Prompts the user to enable Bluetooth to use BLE features.
 *
 * @param onAccept Callback invoked when user chooses to enable Bluetooth
 * @param onDismiss Callback invoked when user dismisses the dialog
 */
@Composable
fun BluetoothDisabledDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionDialog(
        title = stringResource(R.string.bluetooth_disabled_title),
        message = stringResource(R.string.bluetooth_disabled_message),
        onAccept = onAccept,
        onDismiss = onDismiss,
        acceptText = stringResource(R.string.bluetooth_disabled_enable),
        dismissText = stringResource(R.string.dialog_cancel)
    )
}
