package com.autoreply.bot.ui.license

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoreply.bot.domain.model.LicensePlan
import com.autoreply.bot.domain.model.LicenseStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LicenseSection(
    viewModel: LicenseViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var codeInput by remember { mutableStateOf("") }
    var editing by remember(state.status is LicenseStatus.Active) {
        mutableStateOf(state.status !is LicenseStatus.Active)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.status) {
                is LicenseStatus.Active -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = if (state.status is LicenseStatus.Active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "Licencia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        statusLabel(state.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ID de este telefono", style = MaterialTheme.typography.bodySmall)
                    Text(
                        state.deviceId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(state.deviceId)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar ID")
                }
            }
            Text(
                "Envia este ID a quien te alquila la app para pedir tu codigo de activacion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!editing) {
                TextButton(onClick = { editing = true }) {
                    Text("Ingresar otro codigo")
                }
            } else {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Codigo de activacion") },
                    placeholder = { Text("XXXXX-XXXXX-XXXXX-XXXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            viewModel.activate(codeInput)
                            codeInput = ""
                        },
                        enabled = !state.isActivating && codeInput.isNotBlank()
                    ) {
                        Text("Activar")
                    }
                    if (state.isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(start = 12.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            state.feedback?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun statusLabel(status: LicenseStatus): String = when (status) {
    LicenseStatus.NotActivated -> "Sin activar. Introduce un codigo para usar la app."
    is LicenseStatus.Active -> {
        val plan = if (status.plan == LicensePlan.DEMO) "Demo" else "Alquiler"
        val dias = if (status.daysLeft == 1) "1 dia" else "${status.daysLeft} dias"
        "$plan activo · quedan $dias · vence ${formatEpochDay(status.expiresEpochDay)}"
    }
    is LicenseStatus.Expired -> "Vencio el ${formatEpochDay(status.expiresEpochDay)}. Pide un codigo nuevo."
}

private fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
