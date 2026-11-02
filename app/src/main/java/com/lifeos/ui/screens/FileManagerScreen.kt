package com.lifeos.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.requestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.launchActivityForResult
import androidx.compose.foundation.FileSelectorMode
import androidx.foundation.collection.keys
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selectableStateItem.isSelected
import androidx.compose.foundation.remember
import androidx.compose.foundation.selection
import androidx.compose.foundation.selection.MultiSelection
import androidx.compose.foundation.visualFilter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipGroup
import androidx.compose.material3.Dialog
import androidx.compose.material3.DialogProperties
import androidx.compose.material3.Divider
import androidx.compose.material3.EditableText
import androidx.compose.material3.END
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.NavigationItem
import androidx.compose.ui.ScopeUpdate
import androidx.compose.ui.focus.RequestFocus
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.spawnUri
import androidx.compose.ui.spawnUri.SpawnedUri
import androidx.compose.ui.spawnUri.UriHandler
import androidx.compose.ui.spawnUri.type
import androidx.compose.ui.syntax.toString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.textUnit
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.chaquo.python.android.Android
import com.lifeos.data.Document
import com.lifeos.data.DocumentStorage
import com.lifeos.data.HomePage
import com.lifeos.data LauncherSort
import com.lifeos.data.UserSettings
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.SectionHeader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val documentDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
private val imageMimeTypes = listOf("image/*", "image/jpeg", "image/png", "image/webp")
private val videoMimeTypes = listOf("video/*", "video/mp4", "video/3gpp")
private val audioMimeTypes = listOf("audio/*", "audio/mp3", "audio/wav", "audio/ogg")

@Composable
fun FileManagerScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf<LauncherSort>(LauncherSort.NAME) }
    var viewMode by remember { mutableStateOf("grid") } // "grid" or "list"
    var multiSelect by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showingPermissionDialog by remember { mutableStateOf(false) }

    // Request storage permissions
    val storagePermissions = remember {
        mutableStateMapOf(
            "read" to false,
            "write" to false
        )
    }

    // If permissions not yet requested, ask
    ContextThemeWrapper(context) {
        if (storagePermissions["read"] != true || storagePermissions["write"] != true) {
            showingPermissionDialog = true
        }
    }

    // Permission result handling
    var readGranted by remember { mutableStateOf(false) }
    var writeGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Check and request storage permissions
        val readPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val writePermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (readPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            readGranted = true
        }
        if (writePermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            writeGranted = true
        }

        if (!readGranted || !writeGranted) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) { granted ->
                readGranted = granted[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                writeGranted = granted[1] == android.content.pm.PackageManager.PERMISSION_GRANTED
                showingPermissionDialog = false
            }
        }
    }

    // File categories with their icons and mime type filters
    private val fileCategories = listOf(
        ("Recenti", Icons.Default.History, null),
        ("Immagini", Icons.Default.Image, imageMimeTypes),
        ("Video", Icons.Default.Video, videoMimeTypes),
        ("Audio", Icons.Default.Audio, audioMimeTypes),
        ("Documenti", Icons.Default.AttachFile, listOf("application/pdf", "application/msword", "application/vnd.ms-word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "application/json")),
        ("Download", Icons.Default.CloudDownload, null),
        ("Cartelle", Icons.Default.Folder, null)
    )

    // Get files for a category
    fun getFilesForCategory(category: String): List<File> {
        val root = File(Environment.getExternalStorageDirectory(), "LifeOS")
        return if (root.exists()) root.listFiles()?.filter { it.name.contains(category.lowercase()) }?.take(50) ?: emptyList()
    }

    // Format file size
    fun formatSize(size: Long): String {
        if (size >= 1024L * 1024 * 1024) {
            "${((size / (1024L * 1024 * 1024)) * 10f / 10f)} GB"
        } else if (size >= 1024L * 1024) {
            "${((size / (1024L * 1024)) * 10f / 10f)} MB"
        } else if (size >= 1024L) {
            "${((size / 1024) * 10f / 10f)} KB"
        } else {
            "${size} bytes"
        }
    }

    // Get file icon based on type
    private fun getFileIcon(file: File): String {
        val name = file.name.lowercase()
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
            "image"
        } else if (name.endsWith(".mp4") || name.endsWith(".3gp") || name.endsWith(".mov")) {
            "video"
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg")) {
            "audio"
        } else if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")) {
            "document"
        } else if (name.endsWith(".zip") || name.endsWith(".rar")) {
            "archive"
        } else {
            "file"
        }
    }

    // Get file size
    private fun getFileSize(file: File): String {
        if (file.length() == -1L) return "Dimensione sconosciuta"
        return formatSize(file.length())
    }

    // Get modification date
    private fun getModificationDate(file: File): String {
        val lastModified = file.lastModified()
        return documentDateFormat.format(Date(lastModified))
    }

    // Open file (placeholder - actual intent handling would need proper MIME type detection)
    private fun openFile(file: File) {
        // Use SAF or appropriate intent to open the file
        // This is a placeholder for the actual implementation
        Toast(
            text = "Apri: ${file.name}",
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Share file via SAF
    private fun shareFile(file: File) {
        viewModel.documentShareIntent(file.absolutePath)?.let { intent ->
            context.startActivity(intent)
        }
    }

    // Rename file (placeholder)
    private fun renameFile(file: File, newName: String) {
        val newFile = File(file.parentFile, newName)
        if (file.renameTo(newFile)) {
            // Success
        } else {
            // Failed
        }
    }

    // Delete file
    private fun deleteFile(file: File) {
        viewModel.documentStorage.delete(file.absolutePath)
        // Refresh file list
    }

    // Copy file
    private fun copyFile(file: File, destination: File?) {
        if (destination != null) {
            viewModel.documentStorage.copyIn(/* SAF URI */)
        }
    }

    // Create new folder
    fun createNewFolder(folderName: String) {
        val folder = File(Environment.getExternalStorageDirectory(), "LifeOS", folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    // Toggle multi-select for a file
    private fun toggleSelection(fileId: String) {
        if (multiSelect) {
            if (selectedFiles.contains(fileId)) {
                selectedFiles = selectedFiles - fileId
            } else {
                selectedFiles = selectedFiles + fileId
            }
        }
    }

    // Action bar when in multi-select mode
    val isMultiSelectMode = multiSelect && selectedFiles.isNotEmpty()

    // File item component
    @Composable
    fun FileItem(
        file: File,
        fileId: String,
        onToggleSelection: (String) -> Unit,
        onOpen: () -> Unit,
        onShare: () -> Unit,
        onRename: (String) -> Unit,
        onDelete: () -> Unit,
        onSelectAll: () -> Unit
    ) {
        var renamedByUser by remember { mutableStateOf("") }
        val isSelected = selectedFiles.contains(fileId)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewMode == "grid" ? 120.dp : 60.dp)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
                .padding(4.dp)
        ) {
            if (viewMode == "grid") {
                // Grid view
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // File icon
                    val iconName = getFileIcon(file)
                    Icon(
                        if (iconName == "image") Icons.Default.Image else
                        if (iconName == "video") Icons.Default.Video else
                        if (iconName == "audio") Icons.Default.Audio else
                        if (iconName == "document") Icons.Default.AttachFile else
                        Icons.Default.File,
                        contentDescription = file.name,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    // File name
                    Text(
                        file.name,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                    // File size
                    Text(
                        getFileSize(file),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                // Selection check
                if (multiSelect) {
                    Checkbox(
                        selected = isSelected,
                        onCheckedChange = { onToggleSelection(fileId) },
                        modifier = Modifier.align(Alignment.End),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            } else {
                // List view
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection checkbox
                    if (multiSelect) {
                        Checkbox(
                            selected = isSelected,
                            onCheckedChange = { onToggleSelection(fileId) },
                            modifier = Modifier
                                .align(Alignment.Start)
                                .size(20.dp)
                        )
                    }

                    // File icon
                    val iconName = getFileIcon(file)
                    Icon(
                        if (iconName == "image") Icons.Default.Image else
                        if (iconName == "video") Icons.Default.Video else
                        if (iconName == "audio") Icons.Default.Audio else
                        if (iconName == "document") Icons.Default.AttachFile else
                        Icons.Default.File,
                        contentDescription = file.name,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )

                    // File details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodySmall,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            getFileSize(file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                        Text(
                            getModificationDate(file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    // Action buttons (more options)
                    if (!multiSelect) {
                        OutlinedButton(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(4.dp),
                            onClick = { /* show action menu */ },
                            style = OutlinedButtonStyle.outlinedSmall
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    // Main content
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Gestione File",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            ButtonGroup(
                selected = viewMode == "grid",
                onSelectedChange = { viewMode = if (it) "grid" else "list" }
            ) {
                button("Griglia") { modifier = Modifier.padding(4.dp) }
                button("Elenco") { modifier = Modifier.padding(4.dp) }
            }
        }

        // Search and filters
        if (!multiSelect) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Cerca file...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }

        // File categories tabs
        ChipGroup(
            selectedIndex = 0,
            onSelectIndex = { index -> sort = fileCategories[index].let { (_, _, _) -> it } },
            orientation = androidx.compose.material3.Orientation.Horizontal,
            contentPadding = PaddingValues(0.dp)
        ) {
            fileCategories.forEachIndexed { index, (category, icon, mimeTypes) ->
                item {
                    Chip(
                        label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                        selected = sort == category,
                        onClick = { sort = category }
                    )
                }
            }
        }

        // Files list
        val rootDir = File(Environment.getExternalStorageDirectory(), "LifeOS")
        val allFiles = rootDir?.listFiles()?.filter { it.name.startsWith("LifeOS") } ?: emptyList()
        
        // Sort files
        val sortedFiles = when (sort) {
            LauncherSort.NAME -> allFiles.sortedBy { it.name.lowercase() }
            LauncherSort.RECENT -> allFiles.sortedByDescending { it.lastModified() }.take(50)
            else -> allFiles
        }

        if (sortedFiles.isEmpty()) {
            Text(
                "Nessun file trovato. Aggiungi contenuti tramite le varie sezioni di LifeOS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortedFiles.forEach { file ->
                    // Skip . and .. 
                    if (file.name in listOf(".", "..")) return@LazyColumn

                    var fileId by remember { mutableStateOf(file.absolutePath) }

                    // Handle multi-select toggle
                    LaOnTouchArea(
                        onClick = { toggleSelection(fileId) },
                        onLongPress = { /* enter multi-select mode */ }
                    ) {
                        FileItem(
                            file = file,
                            fileId = fileId,
                            onToggleSelection = { toggleSelection(it) },
                            onOpen = { openFile(file) },
                            onShare = { shareFile(file) },
                            onRename = { newName -> renameFile(file, newName) },
                            onDelete = { deleteFile(file) },
                            onSelectAll = { /* select all files */ }
                        )
                    }
                }
            }
        }

        // Multi-select actions bar (shown when multi-select is active)
        if (multiSelect) {
            Divider(modifier = Modifier.fillMaxHeight())
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { selectedFiles.clear(); multiSelect = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annulla")
                }
                OutlinedButton(
                    onClick = { /* perform batch action */ },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warning)
                ) {
                    if (selectedFiles.isEmpty()) {
                        Text("Nessun elemento selezionato")
                    } else if (selectedFiles.size == sortedFiles.size) {
                        Text("Deseleziona tutti")
                    } else {
                        Text("Elimina ${selectedFiles.size}")
                    }
                }
            }
            Divider(modifier = Modifier.fillMaxHeight())
        }
    }
}