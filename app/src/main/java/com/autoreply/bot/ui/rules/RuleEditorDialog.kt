package com.autoreply.bot.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.unit.dp
import com.autoreply.bot.domain.model.MatchType
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
    var matchType by remember { mutableStateOf(initial?.matchType ?: MatchType.CONTAINS) }

    val isValid = response.isNotBlank() &&
        (matchType == MatchType.ANY || keyword.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva regla" else "Editar regla") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val rule = (initial ?: Rule(keyword = "", response = "")).copy(
                        keyword = if (matchType == MatchType.ANY) "" else keyword.trim(),
                        response = response.trim(),
                        matchType = matchType
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
