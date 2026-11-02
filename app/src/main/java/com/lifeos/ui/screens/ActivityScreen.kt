package com.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.Activity
import com.lifeos.data.RepeatRule
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val taskDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

private fun RepeatRule.label(): String = when (this) {
    RepeatRule.NONE -> "Una volta"
    RepeatRule.DAILY -> "Ogni giorno"
    RepeatRule.WEEKLY -> "Ogni settimana"
    RepeatRule.MONTHLY -> "Ogni mese"
}

@Composable
fun ActivityScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.activities.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = "Nessuna attività. Crea la prima con +.",
                        icon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(state.activities.sortedWith(compareBy<Activity> { it.done }.thenBy { it.createdAt }), key = { it.id }) { activity ->
                    TaskCard(
                        activity = activity,
                        expanded = expandedId == activity.id,
                        onExpand = { expandedId = if (expandedId == activity.id) null else activity.id },
                        onToggle = viewModel::onActivityToggle,
                        onRemove = viewModel::onActivityRemove,
                        onAddStep = { viewModel.addChecklistItem(activity.id, it) },
                        onToggleStep = { viewModel.toggleChecklistItem(activity.id, it) },
                        onRemoveStep = { viewModel.removeChecklistItem(activity.id, it) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nuova attività")
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, repeat ->
                viewModel.addActivity(title, repeat)
                showAdd = false
            }
        )
    }
}

@Composable
private fun TaskCard(
    activity: Activity,
    expanded: Boolean,
    onExpand: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onAddStep: (String) -> Unit,
    onToggleStep: (String) -> Unit,
    onRemoveStep: (String) -> Unit
) {
    var stepText by remember { mutableStateOf("") }
    val doneSteps = activity.checklist.count { it.done }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (activity.done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = activity.done,
                    onCheckedChange = { onToggle(activity.id, it) }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpand() }
                ) {
                    Text(
                        text = activity.title.ifEmpty { "Attività senza titolo" },
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (activity.done) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val details = buildString {
                        if (activity.repeat != RepeatRule.NONE) {
                            append(activity.repeat.label())
                            if (activity.nextOccurrenceMillis > 0) {
                                append(" · prossima ")
                                append(taskDateFormat.format(Date(activity.nextOccurrenceMillis)))
                            }
                        }
                        if (activity.checklist.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("$doneSteps/${activity.checklist.size} passi")
                        }
                    }
                    if (details.isNotEmpty()) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (activity.repeat != RepeatRule.NONE) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Ricorrente",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = { onRemove(activity.id) }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onExpand, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Chiudi" else "Checklist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                activity.checklist.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp)
                    ) {
                        Checkbox(
                            checked = item.done,
                            onCheckedChange = { onToggleStep(item.id) },
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveStep(item.id) }, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Rimuovi passo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = stepText,
                        onValueChange = { stepText = it },
                        label = { Text("Nuovo passo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (stepText.isNotBlank()) {
                                onAddStep(stepText.trim())
                                stepText = ""
                            }
                        },
                        enabled = stepText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi passo")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, RepeatRule) -> Unit) {
    var title by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova attività") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Ripetizione", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatRule.entries.forEach { rule ->
                        FilterChip(
                            selected = repeat == rule,
                            onClick = { repeat = rule },
                            label = { Text(rule.label(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), repeat) },
                enabled = title.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
