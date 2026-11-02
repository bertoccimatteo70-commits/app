package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Finanze") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Entrate
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Entrate", style = MaterialTheme.typography.titleMedium)
                        if (state.incomes.isEmpty()) {
                            Text("Nessuna entrata registrata", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.incomes, key = { it.id }) { income ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(income.label, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).format(income.amountCents.toDouble()), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Uscite
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Uscite", style = MaterialTheme.typography.titleMedium)
                        if (state.expenses.isEmpty()) {
                            Text("Nessuna spesa registrata", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.expenses, key = { it.id }) { expense ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(expense.label, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault()).format(expense.amountCents.toDouble()), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Budget
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Budget", style = MaterialTheme.typography.titleMedium)
                        Text("Budget mensile: ${state.settings.monthlyBudgetCents} centesimi", style = MaterialTheme.typography.bodySmall)
                        Text("Spesa totale mese: ${state.monthExpenseTotalCents} centesimi", style = MaterialTheme.typography.bodySmall)
                        val percent = state.budgetPercent
                        if (percent != null) {
                            Chip(
                                label = { Text("${percent}%") },
                                selected = false,
                                onClick = {}
                            )
                            androidx.compose.material3.ProgressBarIndicator(
                                value = percent / 100f
                            )
                        } else {
                            Text("Nessun budget impostato", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Abbonamenti
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Abbonamenti", style = MaterialTheme.typography.titleMedium)
                        if (state.subscriptions.isEmpty()) {
                            Text("Nessun abbonamento registrato", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.subscriptions, key = { it.id }) { subscription ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(subscription.name, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("€${(subscription.amountCents / 100).toString()}/mese", style = MaterialTheme.typography.bodySmall)
                                        Text("${if (subscription.active) "Attivo" else "Scaduto"}", style = MaterialTheme.typography.labelSmall,
                                            color = if (subscription.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Esportazione CSV
            item {
                androidx.compose.material3.Button(
                    onClick = { viewModel?.exportFinanceCsv() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("Esporta CSV") }
            }
        }
    }
}