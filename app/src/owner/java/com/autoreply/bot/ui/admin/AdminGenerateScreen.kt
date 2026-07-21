package com.autoreply.bot.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autoreply.bot.domain.model.LicensePlan
import com.autoreply.bot.license.LicenseCode
import com.autoreply.bot.license.LicenseManager
import com.autoreply.bot.license.LicensePrivateKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class PlanChoice { DEMO, RENTAL }

/**
 * Pantalla SOLO de la variante "owner": genera codigos de activacion para el
 * ID de un cliente. No requiere red; usa la misma clave que embebe la app
 * para verificar (ver [LicenseCode]).
 */
@Composable
fun AdminGenerateScreen(modifier: Modifier = Modifier) {
    var deviceIdInput by remember { mutableStateOf("") }
    var planChoice by remember { mutableStateOf(PlanChoice.DEMO) }
    var months by remember { mutableIntStateOf(1) }
    var result by remember { mutableStateOf<GeneratedCode?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Generar codigo de activacion",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Pega el ID que te paso el cliente (lo ve en Inicio, tarjeta Licencia).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = deviceIdInput,
            onValueChange = { deviceIdInput = it.trim() },
            label = { Text("ID del telefono del cliente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = planChoice == PlanChoice.DEMO,
                onClick = { planChoice = PlanChoice.DEMO },
                label = { Text("Demo (1 dia)") }
            )
            FilterChip(
                selected = planChoice == PlanChoice.RENTAL,
                onClick = { planChoice = PlanChoice.RENTAL },
                label = { Text("Alquiler") }
            )
        }

        if (planChoice == PlanChoice.RENTAL) {
            OutlinedTextField(
                value = months.toString(),
                onValueChange = { text ->
                    months = text.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 24) ?: 1
                },
                label = { Text("Meses (30 dias cada uno)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Button(
            onClick = {
                error = null
                result = null
                if (deviceIdInput.isBlank()) {
                    error = "Pega primero el ID del telefono."
                    return@Button
                }
                val plan = if (planChoice == PlanChoice.DEMO) LicensePlan.DEMO else LicensePlan.RENTAL
                val days = if (planChoice == PlanChoice.DEMO) 1 else months * 30
                val today = LicenseManager.todayEpochDay()
                val expiresEpochDay = today + days - 1
                val deviceHash = LicenseManager.deviceHash(deviceIdInput)
                val code = LicenseCode.generate(deviceHash, expiresEpochDay, plan, LicensePrivateKey.bytes)
                result = GeneratedCode(code, expiresEpochDay, days)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar codigo")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        result?.let { generated ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Codigo generado", style = MaterialTheme.typography.titleMedium)
                    Text(
                        generated.code,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Vence el ${formatEpochDay(generated.expiresEpochDay)} (${generated.days} dia(s))",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { clipboard.setText(AnnotatedString(generated.code)) }) {
                        Text("Copiar codigo")
                    }
                }
            }
        }
    }
}

private data class GeneratedCode(val code: String, val expiresEpochDay: Long, val days: Int)

private fun formatEpochDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
