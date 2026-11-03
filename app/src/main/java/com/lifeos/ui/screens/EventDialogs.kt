package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.lifeos.data.RepeatRule
import java.util.Calendar

@Composable
fun AddEventDialog(
    defaultStartMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Int, RepeatRule) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("9") }
    var minute by remember { mutableStateOf("0") }
    var duration by remember { mutableStateOf("60") }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { text -> hour = text.filter { it.isDigit() }.take(2) },
                        label = { Text("Ora") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { text -> minute = text.filter { it.isDigit() }.take(2) },
                        label = { Text("Minuti") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = { text -> duration = text.filter { it.isDigit() }.take(4) },
                    label = { Text("Durata (minuti)") },
                    singleLine = true
                )
                Text("Ripetizione", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(rule.name.lowercase(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = defaultStartMillis
                        set(Calendar.HOUR_OF_DAY, (hour.toIntOrNull() ?: 9).coerceIn(0, 23))
                        set(Calendar.MINUTE, (minute.toIntOrNull() ?: 0).coerceIn(0, 59))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(
                        title.trim(),
                        calendar.timeInMillis,
                        (duration.toIntOrNull() ?: 60).coerceAtLeast(1),
                        repeat
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
