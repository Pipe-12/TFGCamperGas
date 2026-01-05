package com.example.campergas.ui.components.gas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.campergas.R

/**
 * Dialog for adding a new gas cylinder to the system.
 *
 * Collects cylinder information including name, tare weight (empty weight),
 * fuel capacity, and whether to set it as the active cylinder. Validates
 * input before allowing confirmation.
 *
 * @param onDismiss Callback invoked when dialog is dismissed without adding
 * @param onConfirm Callback invoked with cylinder details when user confirms (name, tare, capacity, setAsActive)
 */
@Composable
fun AddGasCylinderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float, Float, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tare by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var setAsActive by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf(false) }
    var tareError by remember { mutableStateOf(false) }
    var capacityError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.add_cylinder_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.add_cylinder_name_label)) },
                    placeholder = { Text(stringResource(R.string.add_cylinder_name_placeholder)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.add_cylinder_name_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tare,
                    onValueChange = {
                        tare = it
                        tareError = it.toFloatOrNull() == null || it.toFloatOrNull()!! < 0
                    },
                    label = { Text(stringResource(R.string.add_cylinder_tare_label)) },
                    placeholder = { Text(stringResource(R.string.add_cylinder_tare_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = tareError,
                    supportingText = if (tareError) {
                        { Text(stringResource(R.string.add_cylinder_tare_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = {
                        capacity = it
                        capacityError = it.toFloatOrNull() == null || it.toFloatOrNull()!! <= 0
                    },
                    label = { Text(stringResource(R.string.add_cylinder_capacity_label)) },
                    placeholder = { Text(stringResource(R.string.add_cylinder_capacity_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = capacityError,
                    supportingText = if (capacityError) {
                        { Text(stringResource(R.string.add_cylinder_capacity_error)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = setAsActive,
                        onCheckedChange = { setAsActive = it }
                    )
                    Text(
                        text = stringResource(R.string.add_cylinder_set_active),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tareValue = tare.toFloatOrNull()
                    val capacityValue = capacity.toFloatOrNull()

                    if (name.isNotBlank() && tareValue != null && tareValue >= 0 &&
                        capacityValue != null && capacityValue > 0
                    ) {
                        onConfirm(name, tareValue, capacityValue, setAsActive)
                    }
                },
                enabled = name.isNotBlank() &&
                        tare.toFloatOrNull() != null && tare.toFloatOrNull()!! >= 0 &&
                        capacity.toFloatOrNull() != null && capacity.toFloatOrNull()!! > 0
            ) {
                Text(stringResource(R.string.add_cylinder_add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_cylinder_cancel_button))
            }
        }
    )
}
