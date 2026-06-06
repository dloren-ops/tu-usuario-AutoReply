package com.autoreply.bot.ui.update

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoreply.bot.update.ApkInstaller

/**
 * Tarjeta de actualizaciones para la pantalla de inicio.
 * Comprueba GitHub al entrar y permite descargar e instalar la nueva version.
 */
@Composable
fun UpdateSection(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Comprobacion silenciosa automatica al mostrar la pantalla.
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(silent = true)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)
            Text(
                "Version instalada: ${viewModel.currentVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val s = state) {
                is UpdateUiState.Checking ->
                    Text("Buscando actualizaciones...", style = MaterialTheme.typography.bodyMedium)

                is UpdateUiState.UpToDate ->
                    Text("Ya tienes la ultima version.", style = MaterialTheme.typography.bodyMedium)

                is UpdateUiState.Available ->
                    Text(
                        "Disponible la version ${s.info.versionName}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                is UpdateUiState.Downloading -> {
                    Text("Descargando ${s.info.versionName}...")
                    if (s.progress >= 0) {
                        LinearProgressIndicator(
                            progress = { s.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${s.progress}%", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                is UpdateUiState.Error ->
                    Text(
                        s.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                else -> { /* Idle / ReadyToInstall: gestionado abajo */ }
            }

            OutlinedButton(
                onClick = { viewModel.checkForUpdates(silent = false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buscar actualizaciones")
            }
        }
    }

    // Dialogo: actualizacion disponible.
    (state as? UpdateUiState.Available)?.let { available ->
        AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            title = { Text("Nueva version ${available.info.versionName}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        available.info.releaseNotes.ifBlank { "Hay una nueva version disponible." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.download(available.info) }) {
                    Text("Descargar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }) { Text("Ahora no") }
            }
        )
    }

    // Estado: APK descargado, listo para instalar.
    (state as? UpdateUiState.ReadyToInstall)?.let { ready ->
        LaunchedEffect(ready.apk.absolutePath) {
            if (ApkInstaller.canInstall(context)) {
                ApkInstaller.install(context, ready.apk)
                viewModel.dismiss()
            }
        }
        if (!ApkInstaller.canInstall(context)) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Permiso necesario") },
                text = {
                    Text(
                        "Para instalar la actualizacion, permite que AutoReply " +
                            "instale apps desconocidas. Luego vuelve y se instalara."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        context.startActivity(
                            ApkInstaller.unknownSourcesSettingsIntent(context)
                        )
                    }) { Text("Abrir ajustes") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Reintentar instalacion (por si ya concedio el permiso).
                        if (ApkInstaller.canInstall(context)) {
                            ApkInstaller.install(context, ready.apk)
                            viewModel.dismiss()
                        } else {
                            Toast.makeText(
                                context,
                                "Aun sin permiso para instalar.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) { Text("Ya lo permiti") }
                }
            )
        }
    }
}
