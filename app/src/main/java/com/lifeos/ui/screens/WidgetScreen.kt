package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
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
fun WidgetScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Widget") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sezione: Widget disponibili
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Widget disponibili", style = MaterialTheme.typography.titleMedium)
                        Text("Tap per aggiungere alla dashboard", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Activity List widget
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ACTIVITY_LIST, contentDescription = "Lista attività", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Lista attività", style = MaterialTheme.typography.bodyMedium)
                                            Text("Ultime attività", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.NavigateNext, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    onClick = { /* add widget */ }
                                }
                            }

                            // Recent Notes widget
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.RECENT_NOTES, contentDescription = "Note recenti", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Note recenti", style = MaterialTheme.typography.bodyMedium)
                                            Text("Ultime note", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.NavigateNext, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    onClick = { /* add widget */ }
                                }
                            }

                            // Goals widget
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.GOALS, contentDescription = "Obiettivi", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Obiettivi", style = MaterialTheme.typography.bodyMedium)
                                            Text("Progresso obiettivi", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.NavigateNext, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    onClick = { /* add widget */ }
                                }
                            }

                            // Calendar widget
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CALENDAR, contentDescription = "Calendario", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Calendario", style = MaterialTheme.typography.bodyMedium)
                                            Text("Giorni e date", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.NavigateNext, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    onClick = { /* add widget */ }
                                }
                            }

                            // Reminders widget
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    backgroundColor = MaterialTheme.colorScheme.surface) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.REMINDERS, contentDescription = "Promemoria", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text("Promemoria", style = MaterialTheme.typography.bodyMedium)
                                            Text "Promemoria attivi", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        Icon(Icons.Default.NavigateNext, contentDescription = "Aggiungi", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    onClick = { /* add widget */ }
                                }
                            }
                        }
                    }
                }
            }

            // Sezione: Widget nella dashboard
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Widget nella dashboard", style = MaterialTheme.typography.titleMedium)
                        if (state.settings.dashboardWidgets.isEmpty()) {
                            Text("Nessun widget nella dashboard", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.settings.dashboardWidgets, key = { it.kind }) { widget ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(widget.kind.name, style = MaterialTheme.typography.bodySmall)
                                        Switch(checked = widget.visible,
                                            onCheckedChange = { viewModel?.togglingWidget(widget.kind) })
                                        Text("Visibile", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}