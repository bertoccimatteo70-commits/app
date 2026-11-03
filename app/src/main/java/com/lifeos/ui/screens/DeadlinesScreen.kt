package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Deadline
import com.lifeos.data.RepeatRule
import com.lifeos.data.TaskPriority
import com.lifeos.data.label
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val deadlineDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

private fun RepeatRule.label(): String = when (this) {
    RepeatRule.NONE -> "Una volta"
    RepeatRule.DAILY -> "Ogni giorno"
    RepeatRule.WEEKLY -> "Ogni settimana"
    RepeatRule.MONTHLY -> "Ogni mese"
}

@Composable
fun DeadlinesScreen(viewModel: LifeOSViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf<TaskPriority?>(null) }
    var onlyPending by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val visible = state.deadlines
        .filter { priority == null || it.priority == priority }
        .filter { !onlyPending || !it.done }
        .sortedWith(compareBy<Deadline> { it.done }.thenBy { it.dueDateMillis })

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = onlyPending,
                    onClick = { onlyPending = !onlyPending },
                    label = { Text("Da completare") }
                )
            }
            items(TaskPriority.entries) { value ->
                FilterChip(
                    selected = priority == value,
                    onClick = { priority = if (priority == value) null else value },
                    label = { Text(value.label(), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = if (state.deadlines.isEmpty()) {
                            "Nessuna scadenza. Tocca + per registrarne una."
                        } else {
                            "Nessuna scadenza con questi filtri."
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(visible, key = { it.id }) { deadline ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = deadline.done,
                                onCheckedChange = {
                                    if (deadline.repeat == RepeatRule.NONE) {
                                        viewModel.onDeadlineToggle(deadline.id, it)
                                    } else {
                                        viewModel.completeDeadline(deadline.id)
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deadline.title.ifEmpty { "Scadenza senza titolo" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (deadline.done) TextDecoration.LineThrough else null
                                )
                                val details = buildString {
                                    append(deadlineDateFormat.format(Date(deadline.dueDateMillis)))
                                    if (deadline.priority != TaskPriority.NORMAL) {
                                        append(" · ")
                                        append(deadline.priority.label())
                                    }
                                    if (deadline.repeat != RepeatRule.NONE) {
                                        append(" · ")
                                        append(deadline.repeat.label())
                                    }
                                    if (deadline.reminderMinutes > 0) {
                                        append(" · promemoria ")
                                        append(deadline.reminderMinutes)
                                        append(" min prima")
                                    }
                                }
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.onDeadlineRemove(deadline.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Elimina",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi scadenza")
        }

        if (showAdd) {
            AddDeadlineFullDialog(
                onDismiss = { showAdd = false },
                onConfirm = { title, days, chosenPriority, repeat, reminder ->
                    viewModel.addDeadlineFull(
                        title = title,
                        dueDateMillis = System.currentTimeMillis() + days * 86_400_000L,
                        priority = chosenPriority,
                        repeat = repeat,
                        reminderMinutes = reminder
                    )
                    showAdd = false
                }
            )
        }
    }
}

@Composable
private fun AddDeadlineFullDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, TaskPriority, RepeatRule, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    var priority by remember { mutableStateOf(TaskPriority.NORMAL) }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }
    var reminder by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova scadenza") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = { text -> days = text.filter { it.isDigit() }.take(3) },
                    label = { Text("Scade tra (giorni)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reminder,
                    onValueChange = { text -> reminder = text.filter { it.isDigit() }.take(4) },
                    label = { Text("Promemoria (minuti prima, 0 = no)") },
                    singleLine = true
                )
                Text("Priorità", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { value ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(value.label(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text("Ripetizione", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(rule.label(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.trim(),
                        (days.toIntOrNull() ?: 7).coerceAtLeast(0),
                        priority,
                        repeat,
                        reminder.toIntOrNull() ?: 0
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
