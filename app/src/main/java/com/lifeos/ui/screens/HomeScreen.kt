package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
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
import com.lifeos.data.HomePage
import com.lifeos.data.HomeEntry
import com.lifeos.data.AppFolder
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    var showAddFolder by remember { mutableStateOf(false) }
    var showAddPage by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFolder = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova cartella")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Current home pages
            if (state.homePages.isEmpty()) {
                Text("Nessuna pagina home configurata", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.homePages) { page ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(page.title, style = MaterialTheme.typography.titleMedium)
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(page.entries) { entry ->
                                        if (entry.isValid) {
                                            if (entry.appPackage != null) {
                                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(entry.appPackage, style = MaterialTheme.typography.bodyMedium)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(onClick = { /* open app */ }) {
                                                        Icon(Icons.Default.Apps, contentDescription = "Apri app")
                                                    }
                                                }
                                            } else if (entry.folderId != null) {
                                                Text("Cartella: ${entry.folderId}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(onClick = { showAddPage = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova pagina")
            }

            if (showAddFolder) {
                AlertDialog(
                    onDismissRequest = { showAddFolder = false },
                    title = { Text("Nuova cartella") },
                    text = {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Nome") }
                        )
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