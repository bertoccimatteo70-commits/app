package com.lifeos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.FinanceReport
import com.lifeos.data.categoryBudgetStatuses
import com.lifeos.data.expensesInMonth
import com.lifeos.data.financeSeries
import com.lifeos.data.formatCents
import com.lifeos.data.incomesInMonth
import com.lifeos.data.monthBalanceCents
import com.lifeos.data.spendingByCategory
import com.lifeos.data.monthExpenseTotalCents
import com.lifeos.data.monthIncomeTotalCents
import com.lifeos.data.monthlySubscriptionCents
import com.lifeos.data.parseCents
import com.lifeos.data.yearlySubscriptionCents
import com.lifeos.ui.LifeOSViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val financeDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@Composable
fun FinanceScreen(viewModel: LifeOSViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addingIncome by remember { mutableStateOf(false) }
    var addingExpense by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val budgets = state.categoryBudgetStatuses()
    val expenses = state.expensesInMonth()
    val incomes = state.incomesInMonth()
    val series = state.financeSeries()
    val maxFlow = series.maxOfOrNull { maxOf(it.incomeCents, it.expenseCents) } ?: 0L

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val csv = "\uFEFF" + viewModel.exportFinanceCsv()
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                ?: error("Stream non disponibile")
        }
        message = if (result.isSuccess) "CSV esportato" else "Esportazione fallita: ${result.exceptionOrNull()?.message}"
    }

    fun sharePdfReport() {
        val uri = FinanceReport.writePdf(context, state)
        if (uri == null) {
            message = "Generazione PDF non riuscita"
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi report PDF"))
        message = "Report PDF generato"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Saldo del mese", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatCents(state.monthBalanceCents()),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FinanceStat(
                            "Entrate",
                            formatCents(state.monthIncomeTotalCents()),
                            Modifier.weight(1f)
                        )
                        FinanceStat(
                            "Uscite",
                            formatCents(state.monthExpenseTotalCents()),
                            Modifier.weight(1f)
                        )
                        FinanceStat(
                            "Abbonamenti",
                            formatCents(state.monthlySubscriptionCents()) + "/mese",
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { addingIncome = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Entrata")
                        }
                        Button(onClick = { addingExpense = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Spesa")
                        }
                    }
                    message?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Budget per categoria",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { editingBudget = true }) { Text("Imposta") }
            }
        }
        if (budgets.isEmpty()) {
            item {
                Text(
                    "Nessun budget per categoria. Il limite mensile globale si imposta nel tool Spese.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(budgets, key = { it.category }) { budget ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (budget.overBudget) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                budget.category,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${formatCents(budget.spentCents)} / ${formatCents(budget.limitCents)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { viewModel.removeCategoryBudget(budget.category) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Rimuovi budget ${budget.category}",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (budget.percent.coerceIn(0, 100)) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (budget.overBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (budget.overBudget) {
                                "Superato di ${formatCents(-budget.remainingCents)}"
                            } else {
                                "Restano ${formatCents(budget.remainingCents)} · ${budget.percent}% usato"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (budget.overBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Andamento ultimi 6 mesi", style = MaterialTheme.typography.titleSmall)
            Text(
                "Barra piena = entrate, barra sottile = uscite. Scala sul mese più alto.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(series, key = { it.label }) { month ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(month.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.size(width = 52.dp, height = 18.dp))
                    Text(
                        "+${formatCents(month.incomeCents)} · -${formatCents(month.expenseCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatCents(month.balanceCents),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (month.balanceCents >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                LinearProgressIndicator(
                    progress = {
                        if (maxFlow <= 0) 0f else (month.incomeCents.toFloat() / maxFlow).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
                LinearProgressIndicator(
                    progress = {
                        if (maxFlow <= 0) 0f else (month.expenseCents.toFloat() / maxFlow).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Abbonamenti attivi", style = MaterialTheme.typography.titleSmall)
            Text(
                "Totale ${formatCents(state.monthlySubscriptionCents())} al mese · " +
                    "${formatCents(state.yearlySubscriptionCents())} all'anno",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.subscriptions.isEmpty()) {
            item {
                Text(
                    "Nessun abbonamento registrato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.subscriptions, key = { it.id }) { subscription ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subscription.name.ifBlank { "Abbonamento" }, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${formatCents(subscription.amountCents)} · " +
                                "rinnovo ${financeDateFormat.format(Date(subscription.nextDueMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = subscription.active,
                        onCheckedChange = { viewModel.toggleSubscription(subscription.id, it) }
                    )
                }
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Movimenti del mese", style = MaterialTheme.typography.titleSmall)
        }
        if (incomes.isEmpty() && expenses.isEmpty()) {
            item {
                Text(
                    "Nessun movimento registrato questo mese.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(incomes, key = { it.id }) { income ->
            MovementRow(
                title = income.label.ifBlank { "Entrata" },
                detail = "${income.category} · ${financeDateFormat.format(Date(income.dateMillis))}",
                amount = "+${formatCents(income.amountCents)}",
                positive = true,
                onRemove = { viewModel.removeIncome(income.id) }
            )
        }
        items(expenses, key = { it.id }) { expense ->
            MovementRow(
                title = expense.label.ifBlank { "Spesa" },
                detail = "${expense.category} · ${financeDateFormat.format(Date(expense.dateMillis))}",
                amount = "-${formatCents(expense.amountCents)}",
                positive = false,
                onRemove = { viewModel.removeExpense(expense.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportCsv.launch("lifeos-finanze.csv") }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("CSV")
                }
                OutlinedButton(onClick = { sharePdfReport() }) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Report PDF")
                }
            }
            Text(
                "Il CSV contiene tutte le entrate e le uscite, non solo quelle del mese.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (addingIncome) {
        AmountDialog(
            title = "Nuova entrata",
            amountLabel = "Importo (€)",
            categoryLabel = "Categoria",
            defaultCategory = "Stipendio",
            onDismiss = { addingIncome = false },
            onConfirm = { label, cents, category ->
                viewModel.addIncome(label, cents, category)
                addingIncome = false
            }
        )
    }

    if (addingExpense) {
        AddExpenseDialog(
            onDismiss = { addingExpense = false },
            onConfirm = { label, cents, category, method ->
                viewModel.addExpenseFull(label, cents, category, method)
                addingExpense = false
            }
        )
    }

    if (editingBudget) {
        var category by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val options = state.spendingByCategory().map { it.first }
        AlertDialog(
            onDismissRequest = { editingBudget = false },
            title = { Text("Budget per categoria") },
            text = {
                Column {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria") },
                        singleLine = true
                    )
                    if (options.isNotEmpty()) {
                        Text(
                            "Categorie già usate: " + options.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Limite mensile (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = parseCents(limit)
                    when {
                        category.isBlank() -> error = "Indica la categoria"
                        cents == null || cents <= 0 -> error = "Limite non valido"
                        else -> {
                            viewModel.setCategoryBudget(category, cents)
                            editingBudget = false
                        }
                    }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editingBudget = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun FinanceStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MovementRow(
    title: String,
    detail: String,
    amount: String,
    positive: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            amount,
            style = MaterialTheme.typography.bodyMedium,
            color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Elimina $title",
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun AmountDialog(
    title: String,
    amountLabel: String,
    categoryLabel: String,
    defaultCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                    label = { Text(amountLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(categoryLabel) },
                    singleLine = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cents = parseCents(amount)
                when {
                    label.isBlank() -> error = "Inserisci una descrizione"
                    cents == null || cents <= 0 -> error = "Importo non valido"
                    else -> onConfirm(label.trim(), cents, category.trim().ifBlank { defaultCategory })
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
