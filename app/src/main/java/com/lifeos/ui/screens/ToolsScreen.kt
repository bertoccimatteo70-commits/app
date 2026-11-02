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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Calculator
import com.lifeos.notifications.LifeOSNotifications
import com.lifeos.ui.LifeOSViewModel
import kotlinx.coroutines.delay
import java.util.Locale

private val tools = listOf(
    "Calcolatrice",
    "Convertitori",
    "Timer",
    "Cronometro",
    "Sveglie",
    "Spese",
    "Abitudini",
    "Spesa",
    "Password"
)

@Composable
fun ToolsScreen(viewModel: LifeOSViewModel, initialTab: Int = 0) {
    var tab by remember { mutableStateOf(initialTab) }

    LaunchedEffect(initialTab) { tab = initialTab }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            tools.forEachIndexed { index, label ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        when (tab) {
            0 -> CalculatorTool()
            1 -> ConverterTool()
            2 -> TimerTool()
            3 -> StopwatchTool()
            4 -> AlarmTool(viewModel)
            5 -> ExpensesTool(viewModel)
            6 -> HabitsTool(viewModel)
            7 -> ShoppingTool(viewModel)
            else -> PasswordTool(viewModel)
        }
    }
}

// ----------------------------------------------------------------- calculator

@Composable
private fun CalculatorTool() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    val keys = listOf(
        listOf("C", "(", ")", "/"),
        listOf("7", "8", "9", "*"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = error ?: result,
                    style = MaterialTheme.typography.displaySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                        when (key) {
                            "=" -> Button(
                                onClick = {
                                    if (expression.isBlank()) return@Button
                                    try {
                                        result = Calculator.format(Calculator.evaluate(expression))
                                        expression = result
                                        error = null
                                    } catch (e: IllegalArgumentException) {
                                        error = e.message ?: "Errore"
                                    }
                                },
                                modifier = modifier,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("=") }

                            "C" -> OutlinedButton(
                                onClick = { expression = ""; result = "0"; error = null },
                                modifier = modifier,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("C") }

                            "⌫" -> OutlinedButton(
                                onClick = { expression = expression.dropLast(1); error = null },
                                modifier = modifier,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("⌫") }

                            else -> FilledTonalButton(
                                onClick = {
                                    expression += key
                                    error = null
                                },
                                modifier = modifier,
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(key) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- timer

@Composable
private fun TimerTool() {
    val context = LocalContext.current
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && remainingMs > 0) {
            delay(1000)
            remainingMs = (remainingMs - 1000).coerceAtLeast(0)
            if (remainingMs == 0L) {
                running = false
                LifeOSNotifications.notify(
                    context = context,
                    id = 4242,
                    channel = LifeOSNotifications.CHANNEL_TIMERS,
                    title = "Timer concluso",
                    text = "Il timer di LifeOS è terminato."
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = formatDuration(remainingMs),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        if (!running && remainingMs == 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Minuti") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = seconds,
                    onValueChange = { seconds = it.filter { c -> c.isDigit() } },
                    label = { Text("Secondi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (running) {
                        running = false
                    } else {
                        if (remainingMs == 0L) {
                            val total = (minutes.toIntOrNull() ?: 0) * 60L + (seconds.toIntOrNull() ?: 0)
                            remainingMs = total * 1000
                        }
                        running = remainingMs > 0
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (running) "Pausa" else "Avvia") }

            OutlinedButton(
                onClick = { running = false; remainingMs = 0 },
                modifier = Modifier.weight(1f)
            ) { Text("Azzera") }
        }

        Text(
            text = "Il timer usa AlarmManager solo per le sveglie; qui il conto alla rovescia gira nell'app e notifica alla fine.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

// ------------------------------------------------------------------ cronometro

@Composable
private fun StopwatchTool() {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(running) {
        var last = System.currentTimeMillis()
        while (running) {
            delay(50)
            val now = System.currentTimeMillis()
            elapsedMs += now - last
            last = now
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = formatStopwatch(elapsedMs),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { running = !running },
                modifier = Modifier.weight(1f)
            ) { Text(if (running) "Pausa" else "Avvia") }
            OutlinedButton(
                onClick = { laps.add(0, elapsedMs) },
                enabled = elapsedMs > 0,
                modifier = Modifier.weight(1f)
            ) { Text("Giro") }
            OutlinedButton(
                onClick = { running = false; elapsedMs = 0; laps.clear() },
                modifier = Modifier.weight(1f)
            ) { Text("Azzera") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(laps) { lap ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Giro ${laps.indexOf(lap) + 1}", style = MaterialTheme.typography.bodyMedium)
                    Text(formatStopwatch(lap), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun formatStopwatch(ms: Long): String {
    val minutes = ms / 60000
    val seconds = (ms / 1000) % 60
    val hundredths = (ms % 1000) / 10
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths)
}

// -------------------------------------------------------------------- sveglie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTool(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.alarms.isEmpty()) {
                item {
                    Text(
                        text = "Nessuna sveglia. Aggiungine una con +.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.alarms, key = { it.id }) { alarm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = alarm.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { viewModel.toggleAlarm(alarm.id, it) }
                        )
                        IconButton(onClick = { viewModel.removeAlarm(alarm.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Elimina sveglia",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Nuova sveglia")
        }
    }

    if (showAdd) {
        val timeState = rememberTimePickerState(initialHour = 7, initialMinute = 30, is24Hour = true)
        var label by remember { mutableStateOf("Sveglia") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nuova sveglia") },
            text = {
                Column {
                    TimePicker(state = timeState)
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Etichetta") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addAlarm(timeState.hour, timeState.minute, label.ifBlank { "Sveglia" })
                    showAdd = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Annulla") } }
        )
    }
}
