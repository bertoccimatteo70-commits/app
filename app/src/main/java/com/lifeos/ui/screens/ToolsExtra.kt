package com.lifeos.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Habit
import com.lifeos.data.MeasureUnit
import com.lifeos.data.PasswordGenerator
import com.lifeos.data.budgetPercent
import com.lifeos.data.convertMeasure
import com.lifeos.data.epochDay
import com.lifeos.data.expensesInMonth
import com.lifeos.data.formatCents
import com.lifeos.data.habitBestStreak
import com.lifeos.data.habitRecentDays
import com.lifeos.data.habitStreak
import com.lifeos.data.measureFamilies
import com.lifeos.data.measureUnits
import com.lifeos.data.monthExpenseTotalCents
import com.lifeos.data.parseCents
import com.lifeos.ui.LifeOSViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val expenseDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

// --------------------------------------------------------------------- convertitori

@Composable
fun ConverterTool() {
    var family by remember { mutableStateOf(measureFamilies.first()) }
    var from by remember { mutableStateOf(measureUnits.first { it.family == family }) }
    var to by remember { mutableStateOf(measureUnits.first { it.family == family && it.label == "km" }) }
    var input by remember { mutableStateOf("1") }

    val units = measureUnits.filter { it.family == family }
    val value = input.replace(',', '.').toDoubleOrNull()
    val result = value?.let { runCatching { convertMeasure(it, from, to) }.getOrNull() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            measureFamilies.forEach { name ->
                FilterChip(
                    selected = family == name,
                    onClick = {
                        family = name
                        from = unitsFor(name).first()
                        to = unitsFor(name).first()
                    },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Valore") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UnitPicker(
                label = "Da",
                selected = from,
                units = units,
                onSelect = { from = it },
                modifier = Modifier.weight(1f)
            )
            UnitPicker(
                label = "A",
                selected = to,
                units = units,
                onSelect = { to = it },
                modifier = Modifier.weight(1f)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = result?.let { formatNumber(it) + " " + to.label } ?: "Valore non valido",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = value?.let { formatNumber(it) + " " + from.label + " = " + to.label } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            "Conversioni calcolate localmente. Le unità di temperatura usano le formule con offset, " +
                "non un semplice fattore di scala.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun unitsFor(family: String) = measureUnits.filter { it.family == family }

@Composable
private fun UnitPicker(
    label: String,
    selected: MeasureUnit,
    units: List<MeasureUnit>,
    onSelect: (MeasureUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.label, style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label) },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    val format = NumberFormat.getNumberInstance(Locale.ITALY)
    format.maximumFractionDigits = 6
    return format.format(value)
}

// -------------------------------------------------------------------------- spese

@Composable
fun ExpensesTool(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val expenses = state.expensesInMonth()
    val total = state.monthExpenseTotalCents()
    val percent = state.budgetPercent()
    var adding by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Speso questo mese", style = MaterialTheme.typography.labelMedium)
                    Text(formatCents(total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    if (percent != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Budget ${formatCents(state.settings.monthlyBudgetCents)} · $percent% usato",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Nessun budget impostato per questo mese.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { adding = true }) { Text("Aggiungi spesa") }
                        OutlinedButton(onClick = { editingBudget = true }) { Text("Budget") }
                    }
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Text(
                    "Nessuna spesa registrata questo mese.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(expenses, key = { it.id }) { expense ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.label.ifBlank { "Spesa" }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${expense.category} · ${expenseDateFormat.format(Date(expense.dateMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(formatCents(expense.amountCents), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { viewModel.removeExpense(expense.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina spesa", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (adding) {
        var label by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Generale") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuova spesa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Descrizione") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Importo (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria") },
                        singleLine = true
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = parseCents(amount)
                    when {
                        label.isBlank() -> error = "Inserisci una descrizione"
                        cents == null || cents <= 0 -> error = "Importo non valido"
                        else -> {
                            viewModel.addExpense(label.trim(), cents, category.ifBlank { "Generale" })
                            adding = false
                        }
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Annulla") } }
        )
    }

    if (editingBudget) {
        var budget by remember { mutableStateOf(if (state.settings.monthlyBudgetCents > 0) formatCents(state.settings.monthlyBudgetCents) else "") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editingBudget = false },
            title = { Text("Budget mensile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Importo (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = parseCents(budget) ?: 0L
                    viewModel.setMonthlyBudget(cents)
                    editingBudget = false
                }) { Text("Salva") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.setMonthlyBudget(0)
                        editingBudget = false
                    }) { Text("Rimuovi") }
                    TextButton(onClick = { editingBudget = false }) { Text("Annulla") }
                }
            }
        )
    }
}

// ---------------------------------------------------------------------- abitudini

@Composable
fun HabitsTool(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    val today = epochDay()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Segna ogni giorno in cui mantieni l'abitudine: la serie viene calcolata dalle date salvate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.habits.isEmpty()) {
            item {
                Text(
                    "Nessuna abitudine. Aggiungi la prima con il pulsante in basso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.habits, key = { it.id }) { habit: Habit ->
            val doneToday = today in habit.completedDays
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = doneToday,
                            onCheckedChange = { viewModel.toggleHabitToday(habit.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Serie attuale ${habitStreak(habit)} · record ${habitBestStreak(habit)} · ${habit.completedDays.size} giorni",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeHabit(habit.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina abitudine", modifier = Modifier.size(16.dp))
                        }
                    }
                    // Ultimi sette giorni: un giorno saltato si può ancora segnare.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        habitRecentDays(habit, today).forEach { (day, done) ->
                            FilterChip(
                                selected = done,
                                onClick = { viewModel.toggleHabitDay(habit.id, day) },
                                label = {
                                    Text(
                                        habitDayInitial(day),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuova abitudine")
            }
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuova abitudine") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (es. Camminare 30 min)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addHabit(name.trim())
                        adding = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Annulla") } }
        )
    }
}

// ------------------------------------------------------------------ lista spesa

@Composable
fun ShoppingTool(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Articolo") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Q.tà") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (name.isNotBlank()) {
                    viewModel.addShoppingItem(name.trim(), quantity.trim())
                    name = ""
                    quantity = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi articolo")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${state.shopping.count { !it.done }} da comprare · ${state.shopping.count { it.done }} completati",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.clearDoneShopping() }) { Text("Pulisci completati") }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.shopping, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.done, onCheckedChange = { viewModel.toggleShoppingItem(item.id) })
                        Text(
                            text = item.name + if (item.quantity.isNotBlank()) " · ${item.quantity}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeShoppingItem(item.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Rimuovi", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------- password

@Composable
fun PasswordTool(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Le password sono cifrate con una chiave AES-256 generata nell'Android Keystore e non " +
                    "esportabile: nel backup finiscono solo come testo cifrato.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.vault.isEmpty()) {
            item {
                Text(
                    "Nessuna credenziale salvata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.vault, key = { it.id }) { entry ->
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
                        Text(entry.title.ifBlank { "Credenziale" }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            entry.username.ifBlank { "nessun utente" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        val secret = viewModel.revealPassword(entry.id)
                        if (secret == null) {
                            revealed = Triple(entry.title, entry.username, "Impossibile decifrare: chiave non valida")
                        } else {
                            revealed = Triple(entry.title, entry.username, secret)
                        }
                    }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Mostra password")
                    }
                    IconButton(onClick = { viewModel.removePasswordEntry(entry.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        item {
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuova credenziale")
            }
        }
    }

    if (adding) {
        var title by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var generatedLength by remember { mutableStateOf(20) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Nuova credenziale") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Servizio (es. Banca)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Utente o email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(12, 16, 20, 24).forEach { size ->
                            FilterChip(
                                selected = generatedLength == size,
                                onClick = { generatedLength = size },
                                label = { Text("$size", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        TextButton(onClick = {
                            password = PasswordGenerator.generate(generatedLength)
                            error = null
                        }) { Text("Genera") }
                    }
                    Text(
                        "Il generatore esclude i caratteri ambigui (l, I, O, 1, 0).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        title.isBlank() -> error = "Inserisci il servizio"
                        password.isEmpty() -> error = "Inserisci la password"
                        viewModel.addPassword(title.trim(), username.trim(), password) -> adding = false
                        else -> error = "Crittografia non disponibile su questo dispositivo"
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Annulla") } }
        )
    }

    revealed?.let { (title, username, secret) ->
        AlertDialog(
            onDismissRequest = { revealed = null },
            title = { Text(title.ifBlank { "Credenziale" }) },
            text = {
                Column {
                    Text("Utente: $username", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Password: $secret", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "La password resta visibile solo finché chiudi questa finestra.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(secret))
                    revealed = null
                }) { Text("Copia") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, secret)
                    }
                    runCatching {
                        context.startActivity(
                            android.content.Intent.createChooser(share, "Condividi password")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    revealed = null
                }) { Text("Condividi") }
            }
        )
    }
}

/** Iniziale del giorno della settimana per un epoch-day (lun-dom). */
private fun habitDayInitial(epochDayValue: Long): String {
    val zone = java.util.TimeZone.getDefault()
    val millis = epochDayValue * 86_400_000L - zone.getOffset(epochDayValue * 86_400_000L)
    val format = java.text.SimpleDateFormat("EE", java.util.Locale.ITALIAN)
    return format.format(java.util.Date(millis)).take(1).uppercase()
}
