package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AutoCompleteTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.uiunit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.ui.LifeOSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenter(viewModel: LifeOSViewModel?,
                  onDismiss: () -> Unit,
                  onOpen: (SearchDestination, String?) -> Unit) {
    var query by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Cerca in LifeOS") },
                        modifier = Modifier.fillMaxWidth(1f),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; focus = true }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancella")
                                }
                            } else {
                                IconButton(onClick = { /* focus */ }) {
                                    Icon(Icons.Default.Search, contentDescription = "Cerca")
                                }
                            }
                        }
                    )
                },
                optionalAction = {
                    androidx.compose.material3.TextButton(onClick = { /* search */ }) {
                        Text("Cerca")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Risultati per: \"$query\"", style = MaterialTheme.typography.titleMedium)

            if (query.isEmpty()) {
                Text("Inserisci una query per cercare", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Search results
                val state = viewModel?.state ?: com.lifeos.data.LifeOSState()
                val needle = query.trim().lowercase()

                // Note
                if (state.notes.any { n -> n.title.lowercase().contains(needle) || n.body.lowercase().contains(needle) || n.tags.any { t -> t.lowercase().contains(needle) } }) {
                    Chip(label = { Text("Note") }, onClick = { /* navigate */ })
                }

                // Activities
                if (state.activities.any { a -> a.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Attività") }, onClick = { /* navigate */ })
                }

                // Deadlines
                if (state.deadlines.any { d -> d.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Scadenze") }, onClick = { /* navigate */ })
                }

                // Goals
                if (state.goals.any { g -> g.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Obiettivi") }, onClick = { /* navigate */ })
                }

                // Projects
                if (state.projects.any { p -> p.name.lowercase().contains(needle) || p.description.lowercase().contains(needle) }) {
                    Chip(label = { Text("Progetti") }, onClick = { /* navigate */ })
                }

                // Events
                if (state.events.any { e -> e.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Eventi") }, onClick = { /* navigate */ })
                }

                // Documents
                if (state.documents.any { d -> d.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Documenti") }, onClick = { /* navigate */ })
                }

                // Contacts
                if (state.contacts.any { c -> c.name.lowercase().contains(needle) || c.phone.lowercase().contains(needle) || c.email.lowercase().contains(needle) }) {
                    Chip(label = { Text("Contatti") }, onClick = { /* navigate */ })
                }

                // Vault files
                if (state.vaultFiles.any { v -> v.name.lowercase().contains(needle) }) {
                    Chip(label = { Text("Cassaforte") }, onClick = { /* navigate */ })
                }

                // Automations
                if (state.automations.any { a -> a.name.lowercase().contains(needle) }) {
                    Chip(label = { Text("Automazioni") }, onClick = { /* navigate */ })
                }

                // Reminders
                if (state.reminders.any { r -> r.title.lowercase().contains(needle) }) {
                    Chip(label = { Text("Promemoria") }, onClick = { /* navigate */ })
                }

                // Alarms
                if (state.alarms.any { a -> a.label.lowercase().contains(needle) }) {
                    Chip(label = { Text("Sveglie") }, onClick = { /* navigate */ })
                }

                // If no results
                if (/* no chips were added */ true) {
                    Text("Nessun risultato trovato", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (focus) {
            androidx.compose.foundation.layout.Modifier.pleaseFocus()
        }
    }
}

// SearchDestination enum needs to be available
enum class SearchDestination {
    ACTIVITY,
    NOTES,
    DEADLINES,
    GOALS,
    PROJECTS,
    EVENTS,
    DOCUMENTS,
    CONTACTS,
    VAULT,
    AUTOMATIONS,
    REMINDERS,
    ALARMS
}