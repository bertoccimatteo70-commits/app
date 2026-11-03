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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.lifeos.data.TaskPriority
import com.lifeos.data.activityCategories
import com.lifeos.data.activityProgress
import com.lifeos.data.filteredActivities
import com.lifeos.data.projectProgress
import com.lifeos.data.label
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
    var showAddProject by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf<TaskPriority?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var onlyOpen by remember { mutableStateOf(false) }
    var projectFilter by remember { mutableStateOf<String?>(null) }

    val categories = activityCategories(state.activities)
    val visible = filteredActivities(state.activities, query, priority, category, onlyOpen)
        .filter { projectFilter == null || it.projectId == projectFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cerca attività") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = onlyOpen,
                    onClick = { onlyOpen = !onlyOpen },
                    label = { Text("Da fare") }
                )
            }
            items(TaskPriority.entries) { value ->
                FilterChip(
                    selected = priority == value,
                    onClick = { priority = if (priority == value) null else value },
                    label = { Text(value.label(), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { value ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = if (category == value) null else value },
                        label = { Text(value, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Projects with their real completion percentage.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = projectFilter == null,
                    onClick = { projectFilter = null },
                    label = { Text("Tutte") }
                )
            }
            items(state.projects, key = { it.id }) { project ->
                FilterChip(
                    selected = projectFilter == project.id,
                    onClick = { projectFilter = if (projectFilter == project.id) null else project.id },
                    label = {
                        Text(
                            "${project.name} ${projectProgress(state.activities, project.id)}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
            item {
                TextButton(onClick = { showAddProject = true }) { Text("+ Progetto") }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = if (state.activities.isEmpty()) {
                            "Nessuna attività. Crea la prima con +."
                        } else {
                            "Nessuna attività corrisponde ai filtri."
                        },
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
                items(visible, key = { it.id }) { activity ->
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
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Nuova attività")
        }
    }

    if (showAdd) {
        AddTaskDialog(
            categories = categories,
            projects = state.projects.map { it.id to it.name },
            defaultProjectId = projectFilter,
            onDismiss = { showAdd = false },
            onConfirm = { title, repeat, chosenPriority, chosenCategory, dueDays, projectId ->
                viewModel.addTask(
                    title = title,
                    repeat = repeat,
                    priority = chosenPriority,
                    category = chosenCategory,
                    dueDateMillis = if (dueDays > 0) {
                        System.currentTimeMillis() + dueDays * 86_400_000L
                    } else 0L,
                    projectId = projectId
                )
                showAdd = false
            }
        )
    }

    if (showAddProject) {
        var projectName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddProject = false },
            title = { Text("Nuovo progetto") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nome progetto") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = projectName.trim()
                        if (name.isNotEmpty()) viewModel.addProject(name)
                        showAddProject = false
                    }
                ) { Text("Crea") }
            },
            dismissButton = { TextButton(onClick = { showAddProject = false }) { Text("Annulla") } }
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
    val progress = activityProgress(activity)

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
                    modifier = Modifier.weight(1f).clickable { onExpand() }
                ) {
                    Text(
                        text = activity.title.ifEmpty { "Attività senza titolo" },
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (activity.done) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val details = buildString {
                        if (activity.priority != TaskPriority.NORMAL) {
                            append(activity.priority.label())
                        }
                        if (activity.category.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(activity.category)
                        }
                        if (activity.dueDateMillis > 0) {
                            if (isNotEmpty()) append(" · ")
                            append(taskDateFormat.format(Date(activity.dueDateMillis)))
                        }
                        if (activity.repeat != RepeatRule.NONE) {
                            if (isNotEmpty()) append(" · ")
                            append(activity.repeat.label())
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
                    if (activity.checklist.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
private fun AddTaskDialog(
    categories: List<String>,
    projects: List<Pair<String, String>>,
    defaultProjectId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, RepeatRule, TaskPriority, String, Int, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }
    var priority by remember { mutableStateOf(TaskPriority.NORMAL) }
    var category by remember { mutableStateOf("") }
    var dueDays by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf(defaultProjectId) }

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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDays,
                    onValueChange = { text -> dueDays = text.filter { it.isDigit() }.take(3) },
                    label = { Text("Scade tra (giorni, vuoto = nessuna)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Priorità", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { value ->
                        FilterChip(
                            selected = priority == value,
                            onClick = { priority = value },
                            label = { Text(value.label(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
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
                if (categories.isNotEmpty()) {
                    Text("Categorie esistenti", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { value ->
                            FilterChip(
                                selected = category.equals(value, ignoreCase = true),
                                onClick = { category = value },
                                label = { Text(value, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                if (projects.isNotEmpty()) {
                    Text("Progetto", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(projects) { pair ->
                            FilterChip(
                                selected = projectId == pair.first,
                                onClick = {
                                    projectId = if (projectId == pair.first) null else pair.first
                                },
                                label = { Text(pair.second, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.trim(),
                        repeat,
                        priority,
                        category.trim(),
                        dueDays.toIntOrNull() ?: 0,
                        projectId
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
