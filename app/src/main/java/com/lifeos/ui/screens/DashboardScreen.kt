package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
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
fun DashboardScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Widget: Orologio
            if (state.settings.dashboardWidgets.any { it.kind == androidx.lifecycle.compose.rememberAsState(WidgetKind.ACTIVITY_LIST) && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Orologio", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(java.text.DateFormat.HOUR1_MINUTE.format(java.util.Date()), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.End) {
                                Button("Impostazioni", onClick = {}) { Text("") }
                            }
                        }
                    }
                }
            }

            // Widget: Attività imminenti
            if (state.settings.dashboardWidgets.any { it.kind == WidgetKind.RECENT_NOTES && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Attività", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.activities.size} attività", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (state.activities.isNotEmpty()) {
                                Text("Tap per aprire", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Widget: Obiettivi
            if (state.settings.dashboardWidgets.any { it.kind == WidgetKind.GOALS && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Obiettivi", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${state.goals.size} obiettivi", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (state.goals.isNotEmpty()) {
                                Text("Tap per dettagli", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Widget: Calendario
            if (state.settings.dashboardWidgets.any { it.kind == WidgetKind.CALENDAR && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Calendario", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Giorno attivo", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Widget: Promemoria
            if (state.settings.dashboardWidgets.any { it.kind == WidgetKind.REMINDERS && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Promemoria", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text "${state.reminders.count { it.enabled }} attivi", style = MaterialTheme.typography.bodyMedium
                            }
                        }
                    }
                }
            }

            // Widget: Finanze
            if (state.settings.dashboardWidgets.any { it.kind == androidx.lifecycle.compose.rememberAsState(WidgetKind.CALENDAR) && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Finanze", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text "Budget: ${state.budgetPercent}%", style = MaterialTheme.typography.bodyMedium
                            }
                        }
                    }
                }
            }

            // Widget: Abbonamenti
            if (state.settings.dashboardWidgets.any { it.kind == androidx.lifecycle.compose.rememberAsState(WidgetKind.CALENDAR) && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Abbonamenti", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text "${state.activeSubscriptionsPaying().size} attivi", style = MaterialTheme.typography.bodyMedium
                            }
                        }
                    }
                }
            }

            // Widget: Abitudini
            if (state.settings.dashboardWidgets.any { it.kind == androidx.lifecycle.compose.rememberAsState(WidgetKind.CALENDAR) && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Abitudini", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text "${state.habits.size} abitudini", style = MaterialTheme.typography.bodyMedium
                            }
                        }
                    }
                }
            }

            // Widget: Documenti in scadenza
            if (state.settings.dashboardWidgets.any { it.kind == androidx.lifecycle.compose.rememberAsState(WidgetKind.CALENDAR) && it.visible }) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text("Documenti", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text "${state.expiredDocuments().size} scaduti", style = MaterialTheme.typography.bodyMedium
                            }
                        }
                    }
                }
            }
        }
    }
}