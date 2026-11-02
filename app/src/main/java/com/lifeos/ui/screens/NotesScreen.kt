package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipGroup
import androidx.compose.material3.ExperimentalCheckboxGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.uiunit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Note
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: LifeOSViewModel?) {
    val state = viewModel?.state ?: LifeOSState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Note") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.notes.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = "Nessuna nota. Tocca + per crearne una.",
                        icon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                }
            } else {
                items(state.notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(note.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(checked = note.pinned,
                                    onCheckedChange = { if (viewModel != null) viewModel?.onNotePinToggle(note.id) })
                            }
                            Text(note.body.substring(0, if (note.body.length > 50) 50 else note.body.length)
                                .ifBlank { "Nessun testo" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${note.checklist.size} checklist item", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (note.checklist.isNotEmpty()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(note.checklist) { checklistItem ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("☐ ${checklistItem.text}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (checklistItem.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        OutlinedButton(
                                            onClick = { /* toggle checklist */ },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("") }
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { /* view note details */ },
                            modifier = Modifier.fillMaxWidth().padding(4.dp)
                        ) { Text("Dettagli") }
                        
                        if (note.body.length > 50) {
                            OutlinedButton(
                                onClick = { /* expand note */ },
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            ) { Text("Leggi tutto") }
                        }
                    }
                }
            }
            
            // Aggiungi nota
            androidx.compose.material3.FloatingActionButton(
                onClick = { /* add note */ },
                modifier = androidx.compose.material3.Anchor(alignBottom = true, end = true)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi nota")
            }
        }
    }
}