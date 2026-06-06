package com.autoreply.bot.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoreply.bot.ui.util.TimeFormat
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Mensaje de ausencia ---
        SettingCard(title = "Mensaje de ausencia") {
            ToggleRow(
                label = "Responder cuando ninguna regla coincide",
                checked = settings.awayMessageEnabled,
                onCheckedChange = viewModel::setAwayEnabled
            )
            OutlinedTextField(
                value = settings.awayMessage,
                onValueChange = viewModel::setAwayMessage,
                label = { Text("Texto del mensaje") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- Grupos ---
        SettingCard(title = "Chats de grupo") {
            ToggleRow(
                label = "Responder tambien en grupos",
                checked = settings.replyToGroups,
                onCheckedChange = viewModel::setReplyToGroups
            )
        }

        // --- Anti-spam ---
        SettingCard(title = "Anti-spam") {
            Text("Tiempo minimo entre respuestas al mismo contacto: ${settings.cooldownSeconds} s")
            Slider(
                value = settings.cooldownSeconds.toFloat(),
                onValueChange = { viewModel.setCooldownSeconds(it.roundToInt()) },
                valueRange = 0f..600f,
                steps = 0
            )
        }

        // --- Horario ---
        ScheduleCard(
            enabled = settings.scheduleEnabled,
            startMinutes = settings.startMinutes,
            endMinutes = settings.endMinutes,
            activeDays = settings.activeDays,
            onEnabledChange = viewModel::setScheduleEnabled,
            onScheduleChange = viewModel::setSchedule,
            onToggleDay = viewModel::toggleDay
        )
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ScheduleCard(
    enabled: Boolean,
    startMinutes: Int,
    endMinutes: Int,
    activeDays: Set<Int>,
    onEnabledChange: (Boolean) -> Unit,
    onScheduleChange: (Int, Int) -> Unit,
    onToggleDay: (Int) -> Unit
) {
    var pickerFor by remember { mutableStateOf<String?>(null) } // "start" | "end" | null

    SettingCard(title = "Horario de actividad") {
        ToggleRow(
            label = "Responder solo en un horario",
            checked = enabled,
            onCheckedChange = onEnabledChange
        )

        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = { pickerFor = "start" }) {
                    Text("Inicio: ${TimeFormat.minutesToHHmm(startMinutes)}")
                }
                TextButton(onClick = { pickerFor = "end" }) {
                    Text("Fin: ${TimeFormat.minutesToHHmm(endMinutes)}")
                }
            }

            Text("Dias activos", style = MaterialTheme.typography.bodyMedium)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeFormat.dayLabels.forEachIndexed { index, label ->
                    val day = index + 1 // 1=Lun .. 7=Dom
                    FilterChip(
                        selected = day in activeDays,
                        onClick = { onToggleDay(day) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }

    if (pickerFor != null) {
        val initialMinutes = if (pickerFor == "start") startMinutes else endMinutes
        val state = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = state.hour * 60 + state.minute
                    if (pickerFor == "start") {
                        onScheduleChange(minutes, endMinutes)
                    } else {
                        onScheduleChange(startMinutes, minutes)
                    }
                    pickerFor = null
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { pickerFor = null }) { Text("Cancelar") }
            },
            text = { TimePicker(state = state) }
        )
    }
}
