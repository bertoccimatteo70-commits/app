package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Privacy") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sezione: Dati conservati
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dati conservati localmente", style = MaterialTheme.typography.titleMedium)
                        Text("Note: ${state.notes.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Attività: ${state.activities.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Scadenze: ${state.deadlines.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Obiettivi: ${state.goals.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Promemoria: ${state.reminders.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Sveglie: ${state.alarms.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Documenti: ${state.documents.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Password: ${state.vault.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Abbonamenti: ${state.subscriptions.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Spese: ${state.expenses.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Vault file: ${state.vaultFiles.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Progetti: ${state.projects.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Eventi: ${state.events.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Contatti: ${state.contacts.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Notifiche: ${state.notifications.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Sessioni Pomodoro: ${state.pomodoroSessions.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Timer: ${state.timerEntries.size}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Sezione: Impostazioni privacy
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Impostazioni privacy", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.settings.screenshotProtection,
                            onCheckedChange = { viewModel?.setScreenshotProtection(it) }
                        ) {
                            Text("Protezione screenshot", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = state.settings.notificationsEnabled,
                            onCheckedChange = { viewModel?.setNotificationsEnabled(it) }
                        ) {
                            Text("Notifiche", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = state.settings.offlineMode,
                            onCheckedChange = { viewModel?.setOfflineMode(it) }
                        ) {
                            Text("Modalità offline", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Sezione: Sicurezza
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sicurezza", style = MaterialTheme.typography.titleMedium)
                        Text("PIN impostato: ${state.settings.pinHash.isNotEmpty()}", style = MaterialTheme.typography.bodySmall)
                        Text("Blocco biometrico: ${state.settings.biometricEnabled}", style = MaterialTheme.typography.bodySmall)
                        Text("Timeout blocco: ${state.settings.lockTimeoutMinutes} min", style = MaterialTheme.typography.bodySmall)
                        Text("Fallimenti sblocco: ${state.settings.failedUnlocks}", style = MaterialTheme.typography.bodySmall)
                        if (isLockedOut(state.settings)) {
                            Text("Blocco attivo tra ${lockoutSecondsLeft(state.settings)} secondi", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
                        }
                        Button("Cancella tutti i dati", onClick = { viewModel?.resetAllData() }) {
                            Text("Eliminazione definitiva", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
                        }
                    }
                }
            }

            // Sezione: Registro eventi
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Registro eventi", style = MaterialTheme.typography.titleMedium)
                        if (state.settings.securityEvents.isEmpty()) {
                            Text("Nessun evento registrato", style = MaterialTheme.typography.bodySmall)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(state.settings.securityEvents.takeLast(10)) { event ->
                                    Text("• ${event.kind.name} alle ${java.text.DateFormat.SHORT.format(java.util.Date(event.atMillis))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}