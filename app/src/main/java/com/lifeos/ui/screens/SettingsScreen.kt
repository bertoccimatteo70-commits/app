package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipGroup
import androidx.compose.material3.ExperimentalCheckboxGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun SettingsScreen(viewModel: LifeOSViewModel?, query: String = "", onQueryChange: (String) -> Unit) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                optionalAction = {
                    if (query.isNotEmpty()) {
                        androidx.compose.material3.TextButton(onClick = { onQueryChange("") }) {
                            Text("Chiudi")
                        }
                    } else {
                        androidx.compose.material3.TextButton(onClick = { /* search action */ }) {
                            Text("Cerca")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Aspetto
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aspetto", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.settings.animationsEnabled,
                            onCheckedChange = { viewModel?.setAnimationsEnabled(it) }
                        ) { Text("Animazioni") }
                        Switch(
                            checked = state.settings.compactLayout,
                            onCheckedChange = { viewModel?.setCompactLayout(it) }
                        ) { Text("Layout compatto") }
                        OutlinedTextField(
                            value = state.settings.fontScale.toString(),
                            onValueChange = { viewModel?.setFontScale(it.toFloat()) },
                            label = { Text("Scala testo") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                        ChipGroup(
                            selectedIndex = 0,
                            onSelectIndex = { index -> /* handled via theme preset selection */ },
                            orientation = androidx.compose.material3.Orientation.Horizontal,
                            contentPadding = PaddingValues(0.dp) {
                                item { Chip(label = { Text("Classico") }, selected = true, onClick = { viewModel?.saveThemePreset("Classico") } ) }
                                item { Chip(label = { Text("Moderno") }, selected = false, onClick = { viewModel?.saveThemePreset("Moderno") } ) }
                                item { Chip(label = { Text("Material You") }, selected = false, onClick = { viewModel?.saveThemePreset("Material You") } ) }
                            }
                        )
                    }
                }
            }

            // Launcher
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Launcher", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.settings.launcherColumns.toString(),
                            onValueChange = { viewModel?.setLauncherColumns(it.toInt()) },
                            label = { Text("Colonne griglia") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.settings.launcherRows.toString(),
                            onValueChange = { viewModel?.setLauncherRows(it.toInt()) },
                            label = { Text("Righe griglia") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.settings.launcherIconSizeDp.toString(),
                            onValueChange = { viewModel?.setLauncherIconSize(it.toInt()) },
                            label = { Text("Dimensione icone") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.settings.launcherSpacingDp.toString(),
                            onValueChange = { viewModel?.setLauncherSpacing(it.toInt()) },
                            label = { Text("Spaziatura") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                        Switch(
                            checked = state.settings.launcherShowDock,
                            onCheckedChange = { viewModel?.setLauncherShowDock(it) }
                        ) { Text("Mostra dock") }
                        Switch(
                            checked = state.settings.launcherDockTransparent,
                            onCheckedChange = { viewModel?.setLauncherDockTransparent(it) }
                        ) { Text("Dock trasparente") }
                        Switch(
                            checked = state.settings.showAppLabels,
                            onCheckedChange = { viewModel?.setShowAppLabels(it) }
                        ) { Text("Etichette app") }ChipGroup(
                            selectedIndex = 0,
                            onSelectIndex = { index -> /* sorting mode selection */ },
                            orientation = androidx.compose.material3.Orientation.Horizontal,
                            contentPadding = PaddingValues(0.dp) {
                                item { Chip(label = { Text("A-Z") }, selected = true, onClick = { viewModel?.setLauncherSort(LauncherSort.AZ) } ) }
                                item { Chip(label = { Text("Recenti") }, selected = false, onClick = { viewModel?.setLauncherSort(LauncherSort.Recenti) } ) }
                                item { Chip(label = { Text("Personalizzato") }, selected = false, onClick = { viewModel?.setLauncherSort(LauncherSort.Personalizzato) } ) }
                            }
                        )

                    }
                }
            }

            // Launcher predefinito
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Launcher predefinito", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { viewModel?.setDefaultHomeLauncher() },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Usa LifeOS come launcher predefinito") }
                        OutlinedButton(
                            onClick = { viewModel?.clearDefaultHomeLauncher() },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warning)
                        ) { Text("Torna al launcher originale") }
                        OutlinedButton(
                            onClick = { viewModel?.openAndroidHomeChooser() },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Scegli launcher da Android") }
                        Text("Attualmente: ${state.settings.defaultHomeLauncher ? "LifeOS" : "Launcher originale"}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Schermo
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Schermo", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.settings.immersiveMode,
                            onCheckedChange = { viewModel?.setImmersiveMode(it) }
                        ) { Text("Modalità immersiva") }
                        Switch(
                            checked = state.settings.lockOrientation,
                            onCheckedChange = { viewModel?.setLockOrientation(it) }
                        ) { Text("Blocco orientamento") }
                        OutlinedTextField(
                            value = state.settings.fontScale.toString(),
                            onValueChange = { viewModel?.setFontScale(it.toFloat()) },
                            label = { Text("Dimensione interfaccia") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            // Privacy
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Privacy", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.settings.screenshotProtection,
                            onCheckedChange = { viewModel?.setScreenshotProtection(it) }
                        ) { Text("Protezione screenshot") }
                        OutlinedButton(
                            onClick = { /* privacy center */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Centro privacy") }
                    }
                }
            }

            // Sicurezza
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sicurezza", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { viewModel?.setPin("") }
                            /* TODO: implement PIN change UI */,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Cambia PIN") }
                        OutlinedButton(
                            onClick = { viewModel?.removePin() }
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) { Text("Rimuovi PIN") }
                        Switch(
                            checked = state.settings.biometricEnabled,
                            onCheckedChange = { viewModel?.setBiometricEnabled(it) }
                        ) { Text("Biometria") }
                        OutlinedTextField(
                            value = "${state.settings.lockTimeoutMinutes} min",
                            onValueChange = { viewModel?.setLockTimeoutMinutes(it.toInt()) },
                            label = { Text("Timeout blocco") },
                            keyboardOptions = androidx.compose.material3.KeyboardOptions(keyboardType = androidx.compose.material3.KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            // Documenti
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Documenti", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { /* documents */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Gestisci documenti") }
                        OutlinedButton(
                            onClick = { /* export backup */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Backup ed esportazione") }
                    }
                }
            }

            // Produttività
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Produttività", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { /* projects */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Progetti") }
                        OutlinedButton(
                            onClick = { /* habits */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Abitudini") }
                        OutlinedButton(
                            onClick = { /* pomodoro */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Pomodoro") }
                    }
                }
            }

            // Strumenti
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Strumenti", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { /* tools */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Strumenti") }
                    }
                }
            }

            // Backup
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Backup", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { /* export */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Esporta backup") }
                        OutlinedButton(
                            onClick = { /* import */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Importa backup") }
                    }
                }
            }

            // Accessibilità
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Accessibilità", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.settings.highContrast,
                            onCheckedChange = { viewModel?.setHighContrast(it) }
                        ) { Text("Contrasto elevato") }
                        Switch(
                            checked = state.settings.reduceMotion,
                            onCheckedChange = { viewModel?.setReduceMotion(it) }
                        ) { Text("Riduzione animazioni") }
                        Switch(
                            checked = state.settings.largeFont,
                            onCheckedChange = { viewModel?.setLargeFont(it) }
                        ) { Text("Font grandi") }
                    }
                }
            }

            // Informazioni app
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Informazioni app", style = MaterialTheme.typography.titleMedium)
                        Text("Versione: 3.0", style = MaterialTheme.typography.bodySmall)
                        Text("Sviluppatore: LifeOS", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = { /* share app info */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Condividi") }
                    }
                }
            }

            // Diagnostica
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Diagnostica", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { /* diagnostics */ },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) { Text("Esegui diagnostica") }
                    }
                }
            }
        }
    }
}