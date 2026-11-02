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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Reminder
import com.lifeos.data.RepeatRule
import com.lifeos.data.greetingFor
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.MiniDotLegend
import com.lifeos.ui.components.MonthGrid
import com.lifeos.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val dayFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun CalendarScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var monthOffset by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }
    var showAdd by remember { mutableStateOf(false) }

    val displayed = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = displayed.get(Calendar.YEAR)
    val month = displayed.get(Calendar.MONTH)
    val today = Calendar.getInstance()

    fun millisForDay(day: Int): LongRange {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return start..(start + 24 * 60 * 60 * 1000 - 1)
    }

    val eventDays = remember(state.deadlines, state.reminders, monthOffset) {
        (1..displayed.getActualMaximum(Calendar.DAY_OF_MONTH)).filter { day ->
            val range = millisForDay(day)
            state.deadlines.any { it.dueDateMillis in range } ||
                state.reminders.any { it.triggerAtMillis in range }
        }.toSet()
    }

    val selectedRange = millisForDay(selectedDay)
    val dayDeadlines = state.deadlines.filter { it.dueDateMillis in selectedRange }
    val dayReminders = state.reminders.filter { it.triggerAtMillis in selectedRange }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset -= 1 }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Mese precedente")
            }
            Text(
                text = monthFormat.format(displayed.time),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { monthOffset += 1 }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Mese successivo")
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            MonthGrid(
                month = month,
                year = year,
                today = if (monthOffset == 0) today.get(Calendar.DAY_OF_MONTH) else -1,
                marks = eventDays,
                onDayClick = { selectedDay = it }
            )
            Spacer(modifier = Modifier.height(6.dp))
            MiniDotLegend("Giorni con scadenze o promemoria")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = dayFormat.format(Date(selectedRange.first)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (dayDeadlines.isEmpty() && dayReminders.isEmpty()) {
                item {
                    Text(
                        text = "Nessun evento in questo giorno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(dayDeadlines, key = { "d-${it.id}" }) { deadline ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deadline.title.ifEmpty { "Scadenza" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (deadline.done) "fatto" else "scadenza",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(dayReminders, key = { "r-${it.id}" }) { reminder ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = timeFormat.format(Date(reminder.triggerAtMillis)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (reminder.repeat != RepeatRule.NONE) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Ricorrente",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Promemoria programmati") }
            if (state.reminders.isEmpty()) {
                item {
                    Text(
                        text = "Nessun promemoria. Aggiungine uno con +.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.reminders.sortedBy { it.triggerAtMillis }, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onToggle = { viewModel.toggleReminder(reminder.id, it) },
                        onRemove = { viewModel.removeReminder(reminder.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nuovo promemoria")
        }
    }

    if (showAdd) {
        AddReminderFlow(
            onDismiss = { showAdd = false },
            onConfirm = { title, triggerAt, repeat ->
                viewModel.addReminder(title, triggerAt, repeat)
                showAdd = false
            }
        )
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onToggle: (Boolean) -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title.ifEmpty { "Promemoria" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append(SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(reminder.triggerAtMillis)))
                        if (reminder.repeat != RepeatRule.NONE) append(" · ${repeatLabel(reminder.repeat)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina promemoria",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun repeatLabel(rule: RepeatRule): String = when (rule) {
    RepeatRule.NONE -> "una volta"
    RepeatRule.DAILY -> "ogni giorno"
    RepeatRule.WEEKLY -> "ogni settimana"
    RepeatRule.MONTHLY -> "ogni mese"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderFlow(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, RepeatRule) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo promemoria") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pickDate = true }) {
                        Text("Data: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dateMillis)))
                    }
                    TextButton(onClick = { pickTime = true }) {
                        Text(String.format(Locale.getDefault(), "Ora: %02d:%02d", hour, minute))
                    }
                }
                Text("Ripetizione", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(repeatLabel(rule), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(title.trim(), calendar.timeInMillis, repeat)
                },
                enabled = title.isNotBlank()
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annulla") } }
        ) { DatePicker(state = state) }
    }

    if (pickTime) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Seleziona ora") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Annulla") } }
        )
    }
}
