package com.autoreply.bot.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoreply.bot.ui.update.UpdateSection
import com.autoreply.bot.ui.update.UpdateViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    permissionGranted: Boolean,
    onOpenPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionCard(
            granted = permissionGranted,
            onOpenSettings = onOpenPermissionSettings
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Respuestas automaticas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (state.settings.masterEnabled) "Activadas" else "Desactivadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.settings.masterEnabled,
                    onCheckedChange = { viewModel.setMasterEnabled(it) },
                    enabled = permissionGranted
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${state.totalReplies}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Respuestas enviadas (recientes)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!permissionGranted) {
            Text(
                "Concede el permiso de acceso a notificaciones para empezar a responder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        UpdateSection(viewModel = updateViewModel)
    }
}

@Composable
private fun PermissionCard(
    granted: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (granted) "Permiso concedido" else "Permiso requerido",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (granted) {
                        "La app puede leer y responder notificaciones."
                    } else {
                        "Necesita acceso a las notificaciones."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!granted) {
                Button(onClick = onOpenSettings) { Text("Activar") }
            }
        }
    }
    Spacer(Modifier.size(0.dp))
}
