package com.lifeos.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Note
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

@Composable
fun NotesScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val filtered = state.notes
        .filter { note ->
            query.isBlank() ||
                note.title.contains(query, true) ||
                note.body.contains(query, true) ||
                note.tags.any { it.contains(query, true) }
        }
        .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt })

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cerca nelle note") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = if (query.isBlank()) "Nessuna nota." else "Nessun risultato per \"$query\".",
                        icon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onPin = { viewModel.onNotePinToggle(note.id) },
                        onRemove = { viewModel.onNoteRemove(note.id) }
                    )
                }
            }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nuova nota")
        }
    }

    if (showAdd) {
        AddNoteDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, body, tags ->
                viewModel.addNote(title, body, tags)
                showAdd = false
            }
        )
    }
}

@Composable
private fun NoteRow(note: Note, onPin: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (note.pinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title.ifEmpty { "Nota senza titolo" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = if (note.pinned) "Rimuovi dai fissati" else "Fissa",
                        tint = if (note.pinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.tags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
            Text(
                text = noteDateFormat.format(Date(note.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AddNoteDialog(onDismiss: () -> Unit, onConfirm: (String, String, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova nota") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Testo") },
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tag (separati da virgola)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(title.trim(), body.trim(), parsed)
                },
                enabled = title.isNotBlank() || body.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
