package com.lifeos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.lifeos.data.noteFolders
import com.lifeos.data.notesFiltered
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

private enum class NoteView(val label: String) {
    ACTIVE("Attive"),
    ARCHIVED("Archivio"),
    TRASH("Cestino")
}

@Composable
fun NotesScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf<String?>(null) }
    var view by remember { mutableStateOf(NoteView.ACTIVE) }
    var attachFor by remember { mutableStateOf<String?>(null) }

    val folders = noteFolders(state.notes)
    val filtered = notesFiltered(
        notes = state.notes,
        query = query,
        folder = folder,
        includeArchived = view != NoteView.ACTIVE,
        trash = view == NoteView.TRASH
    )

    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val target = attachFor
        attachFor = null
        if (uri != null && target != null) {
            val path = viewModel.attachNoteFile(uri)
            if (path != null) viewModel.setNoteAttachment(target, path)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cerca nelle note") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(NoteView.entries) { value ->
                FilterChip(
                    selected = view == value,
                    onClick = { view = value },
                    label = { Text(value.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (folders.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = folder == null,
                        onClick = { folder = null },
                        label = { Text("Tutte le cartelle") }
                    )
                }
                items(folders) { value ->
                    FilterChip(
                        selected = folder == value,
                        onClick = { folder = if (folder == value) null else value },
                        label = { Text(value, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = when {
                            query.isNotBlank() -> "Nessun risultato per \"$query\"."
                            view == NoteView.TRASH -> "Il cestino è vuoto."
                            view == NoteView.ARCHIVED -> "Nessuna nota archiviata."
                            else -> "Nessuna nota."
                        },
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
                        trash = view == NoteView.TRASH,
                        onPin = { viewModel.onNotePinToggle(note.id) },
                        onArchive = { viewModel.setNoteArchived(note.id, !note.archived) },
                        onTrash = { viewModel.trashNote(note.id) },
                        onRestore = { viewModel.restoreNote(note.id) },
                        onDeleteForever = { viewModel.deleteNoteForever(note.id) },
                        onAttach = {
                            attachFor = note.id
                            attachmentPicker.launch("*/*")
                        }
                    )
                }
            }
        }

        if (view == NoteView.TRASH && filtered.isNotEmpty()) {
            Button(
                onClick = { viewModel.emptyTrash() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) { Text("Svuota cestino") }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nuova nota")
        }
    }

    if (showAdd) {
        AddNoteDialog(
            folders = folders,
            onDismiss = { showAdd = false },
            onConfirm = { title, body, tags, chosenFolder ->
                viewModel.addNote(title, body, tags)
                if (chosenFolder.isNotBlank()) {
                    val created = viewModel.state.value.notes.maxByOrNull { it.createdAt }
                    if (created != null) viewModel.moveNoteToFolder(created.id, chosenFolder)
                }
                showAdd = false
            }
        )
    }
}

@Composable
private fun NoteRow(
    note: Note,
    trash: Boolean,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onAttach: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifEmpty { "Nota senza titolo" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildString {
                        if (note.folder.isNotBlank()) append(note.folder)
                        if (note.archived) {
                            if (isNotEmpty()) append(" · ")
                            append("Archiviata")
                        }
                        if (note.attachments.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("${note.attachments.size} allegati")
                        }
                    }
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trash) {
                    IconButton(onClick = onRestore, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Ripristina",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDeleteForever, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina definitivamente",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onPin, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (note.pinned) "Rimuovi dai fissati" else "Fissa",
                            tint = if (note.pinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onAttach, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Allega file",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onArchive, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = if (note.archived) "Ripristina" else "Archivia",
                            tint = if (note.archived) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onTrash, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Sposta nel cestino",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (note.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
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
                        AssistChip(
                            onClick = {},
                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                        )
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
private fun AddNoteDialog(
    folders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova nota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Testo") },
                    maxLines = 5
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tag (separati da virgola)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Cartella") },
                    singleLine = true
                )
                if (folders.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(folders) { value ->
                            FilterChip(
                                selected = folder == value,
                                onClick = { folder = value },
                                label = { Text(value, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onConfirm(title.trim(), body.trim(), parsed, folder.trim())
                },
                enabled = title.isNotBlank() || body.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
