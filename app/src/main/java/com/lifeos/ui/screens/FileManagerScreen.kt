package com.lifeos.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifeos.data.FileCategory
import com.lifeos.data.FileSortMode
import com.lifeos.data.SafEntry
import com.lifeos.data.SafFileManager
import com.lifeos.data.filterEntries
import com.lifeos.data.formatFileSize
import com.lifeos.data.sortEntries
import com.lifeos.ui.LifeOSViewModel

private val fileCategories = FileCategory.entries
private val fileSortModes = FileSortMode.entries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(viewModel: LifeOSViewModel? = null) {
    val context = LocalContext.current
    val manager = remember { SafFileManager(context) }

    var treeUri by remember { mutableStateOf<Uri?>(null) }
    var stack by remember { mutableStateOf<List<SafEntry>>(emptyList()) }
    var entries by remember { mutableStateOf<List<SafEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FileCategory.ALL) }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME) }
    var ascending by remember { mutableStateOf(true) }
    var grid by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var renaming by remember { mutableStateOf<SafEntry?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var creatingFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<List<SafEntry>>(emptyList()) }
    var bulkKind by remember { mutableStateOf<String?>(null) }
    var pendingEntries by remember { mutableStateOf<List<SafEntry>>(emptyList()) }

    fun reload() {
        val tree = treeUri ?: return
        val current = stack.lastOrNull() ?: return
        entries = manager.list(tree, Uri.parse(current.uri))
    }

    fun report(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val persisted = manager.takePermission(uri)
            treeUri = uri
            stack = listOfNotNull(manager.rootDocument(uri))
            selection = emptySet()
            bulkKind = null
            reload()
            report(
                if (persisted) "Cartella collegata e accesso memorizzato"
                else "Cartella collegata (accesso valido per questa sessione)"
            )
        }
    }

    // Restores the previously granted tree, so the manager opens where the user left it.
    LaunchedEffect(Unit) {
        val saved = manager.persistedTreeUri()
        if (saved != null && treeUri == null) {
            treeUri = saved
            stack = listOfNotNull(manager.rootDocument(saved))
        }
    }

    LaunchedEffect(treeUri, stack) { reload() }

    val visible = remember(entries, query, category, sortMode, ascending) {
        sortEntries(filterEntries(entries, query, category), sortMode, ascending)
    }
    val currentDirectory = stack.lastOrNull()
    val currentPath = stack.joinToString(" / ") { it.name }

    fun openEntry(entry: SafEntry) {
        if (entry.isDirectory) {
            stack = stack + entry
            selection = emptySet()
            return
        }
        runCatching { context.startActivity(manager.openIntent(entry)) }
            .onFailure { report("Nessuna app installata per aprire ${entry.name}") }
    }

    fun deleteEntries(targets: List<SafEntry>) {
        val deleted = targets.count { manager.delete(it) }
        selection = emptySet()
        reload()
        report("Eliminati $deleted di ${targets.size}")
    }

    fun applyBulk(kind: String, targets: List<SafEntry>) {
        if (treeUri == null) return
        val sourceParent = currentDirectory ?: return
        val targetParent = Uri.parse(sourceParent.uri)
        var done = 0
        targets.forEach { entry ->
            val result = if (kind == MOVED) {
                manager.move(entry, Uri.parse(sourceParent.uri), targetParent)
            } else {
                manager.copy(entry, targetParent)
            }
            if (result != null) done++
        }
        bulkKind = null
        pendingEntries = emptyList()
        selection = emptySet()
        reload()
        report(if (kind == MOVED) "Spostati $done di ${targets.size}" else "Copiati $done di ${targets.size}")
    }

    if (treeUri == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text("Nessuna cartella collegata", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "LifeOS usa lo Storage Access Framework: scegli una cartella e potrai sfogliarla, " +
                    "cercare, rinominare, copiare, spostare ed eliminare file reali senza chiedere " +
                    "permessi di storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { picker.launch(null) }) { Text("Scegli cartella") }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar: current path, refresh, view mode, new folder, change tree.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentPath.ifBlank { "Archivio" },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${visible.size} elementi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (stack.size > 1) {
                IconButton(onClick = { stack = stack.dropLast(1); selection = emptySet() }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Cartella superiore")
                }
            }
            IconButton(onClick = { reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
            }
            IconButton(onClick = { grid = !grid }) {
                Icon(
                    if (grid) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = if (grid) "Vista elenco" else "Vista griglia"
                )
            }
            IconButton(onClick = { creatingFolder = true; folderName = "" }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Nuova cartella")
            }
            IconButton(onClick = { picker.launch(null) }) {
                Icon(Icons.Default.Folder, contentDescription = "Cambia cartella")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Cerca in questa cartella") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fileCategories) { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option.label) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(fileSortModes) { option ->
                FilterChip(
                    selected = sortMode == option,
                    onClick = { sortMode = option },
                    label = { Text(option.label) }
                )
            }
            item {
                FilterChip(
                    selected = !ascending,
                    onClick = { ascending = !ascending },
                    label = { Text(if (ascending) "Crescente" else "Decrescente") }
                )
            }
        }

        if (bulkKind != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scegli la destinazione, poi ${if (bulkKind == MOVED) "sposta" else "copia"} ${pendingEntries.size} elementi",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    bulkKind = null
                    pendingEntries = emptyList()
                }) { Text("Annulla") }
                TextButton(onClick = { applyBulk(bulkKind ?: COPIED, pendingEntries) }) {
                    Text(if (bulkKind == MOVED) "Sposta qui" else "Copia qui")
                }
            }
        }

        if (visible.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (query.isBlank()) "Cartella vuota" else "Nessun file corrisponde alla ricerca",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (grid) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible.chunked(2)) { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEntries.forEach { entry ->
                            Box(modifier = Modifier.weight(1f)) {
                                FileEntryTile(
                                    entry = entry,
                                    selected = selection.contains(entry.uri),
                                    selectionMode = selection.isNotEmpty(),
                                    onToggle = {
                                        selection = if (selection.contains(entry.uri)) {
                                            selection - entry.uri
                                        } else {
                                            selection + entry.uri
                                        }
                                    },
                                    onOpen = { openEntry(entry) }
                                )
                            }
                        }
                        if (rowEntries.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(visible, key = { it.uri }) { entry ->
                    FileEntryRow(
                        entry = entry,
                        selected = selection.contains(entry.uri),
                        selectionMode = selection.isNotEmpty(),
                        onToggle = {
                            selection = if (selection.contains(entry.uri)) {
                                selection - entry.uri
                            } else {
                                selection + entry.uri
                            }
                        },
                        onOpen = { openEntry(entry) },
                        onRename = { renaming = entry; renameValue = entry.name },
                        onDelete = { deleting = listOf(entry) },
                        onShare = {
                            runCatching { context.startActivity(manager.shareIntent(entry)) }
                                .onFailure { report("Impossibile condividere ${entry.name}") }
                        }
                    )
                }
            }
        }

        if (selection.isNotEmpty()) {
            val selectedEntries = visible.filter { selection.contains(it.uri) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedEntries.size} selezionati",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    bulkKind = COPIED
                    pendingEntries = selectedEntries
                }) { Icon(Icons.Default.ContentCopy, contentDescription = "Copia") }
                IconButton(onClick = {
                    bulkKind = MOVED
                    pendingEntries = selectedEntries
                }) { Icon(Icons.Default.DriveFileMove, contentDescription = "Sposta") }
                IconButton(onClick = { deleting = selectedEntries }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina")
                }
                IconButton(onClick = { selection = emptySet() }) {
                    Icon(Icons.Default.Close, contentDescription = "Annulla selezione")
                }
            }
        }
    }

    if (creatingFolder) {
        AlertDialog(
            onDismissRequest = { creatingFolder = false },
            title = { Text("Nuova cartella") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parent = currentDirectory
                        val name = folderName.trim()
                        creatingFolder = false
                        if (parent != null && name.isNotEmpty()) {
                            val created = manager.createFolder(Uri.parse(parent.uri), name)
                            reload()
                            report(if (created != null) "Cartella creata" else "Creazione non riuscita")
                        }
                    }
                ) { Text("Crea") }
            },
            dismissButton = { TextButton(onClick = { creatingFolder = false }) { Text("Annulla") } }
        )
    }

    renaming?.let { target ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rinomina") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Nuovo nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameValue.trim()
                        renaming = null
                        if (name.isNotEmpty() && manager.rename(target, name)) {
                            reload()
                            report("Rinominato")
                        } else {
                            report("Rinomina non riuscita")
                        }
                    }
                ) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Annulla") } }
        )
    }

    if (deleting.isNotEmpty()) {
        val targets = deleting
        AlertDialog(
            onDismissRequest = { deleting = emptyList() },
            title = { Text("Eliminare?") },
            text = {
                Text(
                    if (targets.size == 1) "Eliminare \"${targets.first().name}\"?"
                    else "Eliminare ${targets.size} elementi?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleting = emptyList()
                    deleteEntries(targets)
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { deleting = emptyList() }) { Text("Annulla") } }
        )
    }
}

private const val COPIED = "copy"
private const val MOVED = "move"

private fun iconFor(entry: SafEntry): ImageVector {
    if (entry.isDirectory) return Icons.Default.Folder
    val mime = entry.mimeType.orEmpty()
    return when {
        mime.startsWith("image/") -> Icons.Default.Image
        mime.startsWith("video/") -> Icons.Default.Movie
        mime.startsWith("audio/") -> Icons.Default.MusicNote
        mime.startsWith("text/") -> Icons.Default.Description
        mime.contains("pdf") -> Icons.Default.Description
        mime.contains("word") -> Icons.Default.Description
        mime.contains("officedocument") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun FileEntryRow(
    entry: SafEntry,
    selected: Boolean,
    selectionMode: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
            }
            Icon(
                imageVector = iconFor(entry),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (entry.isDirectory) "Cartella" else formatFileSize(entry.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpen) { Text(if (entry.isDirectory) "Apri" else "Visualizza") }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, contentDescription = "Rinomina") }
            if (!entry.isDirectory) {
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Condividi") }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Elimina") }
        }
    }
}

@Composable
private fun FileEntryTile(
    entry: SafEntry,
    selected: Boolean,
    selectionMode: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
            }
            Icon(
                imageVector = iconFor(entry),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (entry.isDirectory) "Cartella" else formatFileSize(entry.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onOpen) { Text(if (entry.isDirectory) "Apri" else "Vedi") }
        }
    }
}
