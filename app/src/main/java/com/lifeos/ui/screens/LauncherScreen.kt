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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LauncherAppRecord
import com.lifeos.data.AppFolder
import com.lifeos.data.HomeEntry
import com.lifeos.data.HomePage
import com.lifeos.data.HomePage
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    var showAddFolder by remember { mutableStateOf(false) }
    var showAddApp by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var newAppPackage by remember { mutableStateOf("") }

    val favorites = state.settings.launcherFavorites
    val hiddenApps = state.settings.launcherHiddenApps
    val sortMode = state.settings.launcherSort
    val dockSize = state.settings.launcherDockSize.coerceIn(0, 7)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("App") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFolder = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova cartella")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Drawer section
            Text("Drawer", style = MaterialTheme.typography.titleMedium)

            val allApps = state.activities.isNotEmpty() ? state.activities.map { activity ->
                LauncherAppRecord(label = activity.title, packageName = activity.id, installTime = activity.createdAt)
            } : emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize().verticalScroll(Modifier.rememberScrollState()),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (allApps.isEmpty() && state.homePages.isEmpty()) {
                    EmptyPlaceholder(
                        message = "Nessuna applicazione disponibile",
                        icon = { Icon(Icons.Default.Apps, contentDescription = null) }
                    )
                }

                // Apps list
                items(allApps, key = { it.packageName }) { app ->
                    // Check if hidden
                    if (hiddenApps.contains(app.packageName)) {
                        // Show hidden only if revealed
                        if (!state.settings.revealHiddenApps) {
                            Text("App nascosta: ${app.label}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            continue
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium)
                                if (app.installTime > 0) {
                                    Text("Installata il ${java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(app.installTime))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End) {
                                Switch(checked = favorites.contains(app.packageName),
                                    onCheckedChange = { viewModel?.onFavoriteToggle(app.packageName) })
                                IconButton(onClick = { viewModel?.removeHomeEntry("", "app:${app.packageName}") }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi")
                                }
                            }
                        }
                    }
                }

                // Divider
                Divider(height = 16.dp, thickness = 1.dp)

                // Folders section
                Text("Cartelle", style = MaterialTheme.typography.titleMedium)
                items(state.launcherFolders, key = { it.id }) { folder ->
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                                Text("(${folder.packages.size} app)", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(folder.packages) { pkg ->
                                    LauncherAppRecord(label = pkg, packageName = pkg, installTime = 0L).let { app ->
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(app.label, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(checked = favorites.contains(app.packageName),
                                                onCheckedChange = { viewModel?.onFavoriteToggle(app.packageName) })
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { /* edit folder */ }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Modifica")
                                }
                                IconButton(onClick = { /* delete folder */ }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi")
                                }
                            }
                        }
                    }
                }
            }

            if (showAddFolder) {
                AlertDialog(
                    onDismissRequest = { showAddFolder = false },
                    title = { Text("Nuova cartella") },
                    text = {
                        OutlinedTextField(value = newFolderName, onValueChange = { newFolderName = it },
                            label = { Text("Nome") })
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel?.createFolder(newFolderName.trim())
                                newFolderName = ""
                                showAddFolder = false
                            }
                        }) { Text("Salva") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddFolder = false; newFolderName = "" }) { Text("Annulla") }
                    }
                )
            }
        }
    }
}