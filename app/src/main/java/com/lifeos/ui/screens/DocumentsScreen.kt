package com.lifeos.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Document
import com.lifeos.data.Subscription
import com.lifeos.data.activeSubscriptionsPaying
import com.lifeos.data.documentsFor
import com.lifeos.data.expiredDocuments
import com.lifeos.data.expiringDocuments
import com.lifeos.data.formatCents
import com.lifeos.data.parseCents
import com.lifeos.data.parseTags
import com.lifeos.data.RepeatRule
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.SectionHeader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val documentDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private val documentCategories = listOf("Identità", "Salute", "Casa", "Veicoli", "Lavoro", "Finanze", "Altro")

@Composable
fun DocumentsScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Document?>(null) }
    var creating by remember { mutableStateOf(false) }
    var addingSubscription by remember { mutableStateOf(false) }

    val visible = documentsFor(state.documents, query, category, favoritesOnly)
    val expiring = state.expiringDocuments()
    val expired = state.expiredDocuments()
    val categories = (documentCategories + state.documents.map { it.category }).distinct()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Cerca per titolo o tag") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Preferiti", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("Tutte", style = MaterialTheme.typography.labelSmall) }
                )
                categories.forEach { name ->
                    FilterChip(
                        selected = category == name,
                        onClick = { category = if (category == name) null else name },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (expired.isNotEmpty()) {
            item { SectionHeader("Scaduti (${expired.size})") }
            items(expired, key = { "exp-${it.id}" }) { document ->
                DocumentRow(
                    document = document,
                    expired = true,
                    onEdit = { editing = document },
                    onDelete = { viewModel.removeDocument(document.id) },
                    onFavorite = { viewModel.toggleDocumentFavorite(document.id) },
                    onShare = {
                        context.startActivity(viewModel.documentShareIntent(document.attachmentPath))
                    }
                )
            }
        }

        if (expiring.isNotEmpty()) {
            item { SectionHeader("In scadenza (${expiring.size})") }
            items(expiring, key = { "soon-${it.id}" }) { document ->
                DocumentRow(
                    document = document,
                    expired = false,
                    onEdit = { editing = document },
                    onDelete = { viewModel.removeDocument(document.id) },
                    onFavorite = { viewModel.toggleDocumentFavorite(document.id) },
                    onShare = {
                        context.startActivity(viewModel.documentShareIntent(document.attachmentPath))
                    }
                )
            }
        }

        item { SectionHeader("Documenti (${visible.size})") }
        if (visible.isEmpty()) {
            item {
                Text(
                    text = "Nessun documento. Aggiungi carta d'identità, patente, contratti o ricevute: " +
                        "puoi allegare un file o una foto e impostare la scadenza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        items(visible, key = { it.id }) { document ->
            DocumentRow(
                document = document,
                expired = false,
                onEdit = { editing = document },
                onDelete = { viewModel.removeDocument(document.id) },
                onFavorite = { viewModel.toggleDocumentFavorite(document.id) },
                onShare = { context.startActivity(viewModel.documentShareIntent(document.attachmentPath)) }
            )
        }

        item { SectionHeader("Abbonamenti e pagamenti ricorrenti") }
        if (state.subscriptions.isEmpty()) {
            item {
                Text(
                    text = "Nessun abbonamento registrato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        val dueSubscriptions = state.activeSubscriptionsPaying()
        items(state.subscriptions, key = { "sub-${it.id}" }) { subscription ->
            SubscriptionRow(
                subscription = subscription,
                dueSoon = subscription in dueSubscriptions,
                onToggle = { viewModel.toggleSubscription(subscription.id, it) },
                onDelete = { viewModel.removeSubscription(subscription.id) }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { creating = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Documento")
                }
                OutlinedButton(
                    onClick = { addingSubscription = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Abbonamento") }
            }
        }
    }

    if (creating) {
        DocumentEditor(
            initial = null,
            viewModel = viewModel,
            onDismiss = { creating = false },
            onSave = { document, attachment ->
                viewModel.addDocument(
                    title = document.title,
                    category = document.category,
                    notes = document.notes,
                    expiryMillis = document.expiryMillis,
                    tags = document.tags,
                    attachment = attachment
                )
                creating = false
            }
        )
    }

    editing?.let { document ->
        DocumentEditor(
            initial = document,
            viewModel = viewModel,
            onDismiss = { editing = null },
            onSave = { updated, attachment ->
                viewModel.updateDocument(
                    updated.copy(
                        attachmentPath = attachment?.first ?: updated.attachmentPath,
                        attachmentName = attachment?.second ?: updated.attachmentName
                    )
                )
                editing = null
            }
        )
    }

    if (addingSubscription) {
        SubscriptionEditor(
            onDismiss = { addingSubscription = false },
            onSave = { name, cents, due, repeat ->
                viewModel.addSubscription(name, cents, due, repeat)
                addingSubscription = false
            }
        )
    }
}

@Composable
private fun DocumentRow(
    document: Document,
    expired: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(document.title.ifBlank { "Documento" }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = buildString {
                        append(document.category)
                        if (document.expiryMillis > 0) {
                            append(" · scade ")
                            append(documentDateFormat.format(Date(document.expiryMillis)))
                        }
                        if (document.attachmentName != null) append(" · allegato")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expired) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (document.tags.isNotEmpty()) {
                    Text(
                        document.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (document.notes.isNotBlank()) {
                    Text(
                        document.notes.take(90),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (document.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Preferito",
                    modifier = Modifier.size(16.dp),
                    tint = if (document.favorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (document.attachmentPath != null) {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Condividi allegato", modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Modifica", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: Subscription,
    dueSoon: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subscription.name.ifBlank { "Abbonamento" }, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${formatCents(subscription.amountCents)} · " +
                        documentDateFormat.format(Date(subscription.nextDueMillis)) +
                        if (dueSoon && subscription.active) " · in scadenza" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dueSoon) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = subscription.active, onCheckedChange = onToggle)
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina abbonamento", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentEditor(
    initial: Document?,
    viewModel: LifeOSViewModel,
    onDismiss: () -> Unit,
    onSave: (Document, Pair<String, String>?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "Altro") }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var tags by remember { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var expiry by remember { mutableStateOf(initial?.expiryMillis ?: 0L) }
    var pickDate by remember { mutableStateOf(false) }
    var attachment by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) attachment = viewModel.attachDocument(uri)
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCapture?.let { uri -> attachment = viewModel.attachDocument(uri) }
        }
    }

    fun captureDocument() {
        val directory = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File.createTempFile("scan-", ".jpg", directory)
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
        if (uri != null) {
            pendingCapture = uri
            camera.launch(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuovo documento" else "Modifica documento") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo (es. Carta d'identità)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    documentCategories.forEach { name ->
                        FilterChip(
                            selected = category == name,
                            onClick = { category = name },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tag separati da virgola") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pickDate = true }) {
                        Text(
                            if (expiry > 0) "Scadenza: " + documentDateFormat.format(Date(expiry))
                            else "Imposta scadenza"
                        )
                    }
                    if (expiry > 0) {
                        TextButton(onClick = { expiry = 0 }) { Text("Rimuovi") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Allega file")
                    }
                    OutlinedButton(onClick = { captureDocument() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scatta foto")
                    }
                }
                val currentAttachment = attachment?.second ?: initial?.attachmentName
                currentAttachment?.let {
                    Text(
                        "Allegato: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val document = (initial ?: Document()).copy(
                        title = title.trim(),
                        category = category.trim().ifBlank { "Altro" },
                        notes = notes.trim(),
                        tags = parseTags(tags),
                        expiryMillis = expiry
                    )
                    onSave(document, attachment)
                },
                enabled = title.isNotBlank()
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )

    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiry.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { expiry = it }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annulla") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionEditor(
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, RepeatRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(System.currentTimeMillis()) }
    var repeat by remember { mutableStateOf(RepeatRule.MONTHLY) }
    var pickDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo abbonamento") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (es. Palestra)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importo (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                TextButton(onClick = { pickDate = true }) {
                    Text("Prossimo pagamento: " + documentDateFormat.format(Date(due)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(repeatLabel(rule), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cents = parseCents(amount)
                when {
                    name.isBlank() -> error = "Inserisci un nome"
                    cents == null -> error = "Importo non valido"
                    else -> onSave(name.trim(), cents, due, repeat)
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )

    if (pickDate) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = due)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { due = it }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annulla") } }
        ) { DatePicker(state = pickerState) }
    }
}
