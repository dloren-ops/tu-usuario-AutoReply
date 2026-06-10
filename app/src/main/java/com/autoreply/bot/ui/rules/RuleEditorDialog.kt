package com.autoreply.bot.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.autoreply.bot.domain.model.MatchType
import com.autoreply.bot.domain.model.ReplyFrequency
import com.autoreply.bot.domain.model.ReplyScope
import com.autoreply.bot.domain.model.Rule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorDialog(
    initial: Rule?,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var keyword by remember { mutableStateOf(initial?.keyword ?: "") }
    var response by remember { mutableStateOf(initial?.response ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var matchType by remember { mutableStateOf(initial?.matchType ?: MatchType.CONTAINS) }
    var scope by remember { mutableStateOf(initial?.scope ?: ReplyScope.ALL) }
    var frequency by remember { mutableStateOf(initial?.frequency ?: ReplyFrequency.ALWAYS) }
    var everyHours by remember { mutableStateOf((initial?.everyHours ?: 24).toString()) }

    val hoursValid = frequency != ReplyFrequency.EVERY_HOURS ||
        (everyHours.toIntOrNull()?.let { it in 1..720 } == true)
    val isValid = response.isNotBlank() &&
        (matchType == MatchType.ANY || keyword.isNotBlank()) &&
        hoursValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva regla" else "Editar regla") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre de la regla (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de coincidencia")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MatchType.entries.forEach { type ->
                        FilterChip(
                            selected = matchType == type,
                            onClick = { matchType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                if (matchType != MatchType.ANY) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Palabra clave") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("Respuesta") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Alcance: a que conversaciones aplica ---
                Text("Aplicar a")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReplyScope.entries.forEach { sc ->
                        FilterChip(
                            selected = scope == sc,
                            onClick = { scope = sc },
                            label = { Text(sc.label) }
                        )
                    }
                }

                // --- Frecuencia: cuantas veces responder por conversacion ---
                Text("Frecuencia de respuesta")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReplyFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.label) }
                        )
                    }
                }

                if (frequency == ReplyFrequency.EVERY_HOURS) {
                    OutlinedTextField(
                        value = everyHours,
                        onValueChange = { everyHours = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Cada cuantas horas (1-720)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (frequency == ReplyFrequency.ONCE) {
                    Text(
                        "Respondera una sola vez a cada chat/grupo, aunque sigan " +
                            "llegando mensajes."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val rule = (initial ?: Rule(keyword = "", response = "")).copy(
                        title = title.trim(),
                        keyword = if (matchType == MatchType.ANY) "" else keyword.trim(),
                        response = response.trim(),
                        matchType = matchType,
                        scope = scope,
                        frequency = frequency,
                        everyHours = everyHours.toIntOrNull()?.coerceIn(1, 720) ?: 24
                    )
                    onSave(rule)
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
