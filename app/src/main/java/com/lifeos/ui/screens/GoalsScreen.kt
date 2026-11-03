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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.GoalKind
import com.lifeos.data.goalAverageProgress
import com.lifeos.data.goalHistory
import com.lifeos.data.goalMilestonePercent
import com.lifeos.data.label
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.EmptyPlaceholder
import com.lifeos.ui.components.GoalBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val goalDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
fun GoalsScreen(viewModel: LifeOSViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var kindFilter by remember { mutableStateOf<GoalKind?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val visible = state.goals.filter { kindFilter == null || it.kind == kindFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalKind.entries.forEach { kind ->
                FilterChip(
                    selected = kindFilter == kind,
                    onClick = { kindFilter = if (kindFilter == kind) null else kind },
                    label = { Text(kind.label(), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        message = "Nessun obiettivo. Tocca + per impostarne uno.",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(visible, key = { it.id }) { goal ->
                    var milestoneText by remember { mutableStateOf("") }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            GoalBar(goal = goal)
                            Text(
                                text = "${goal.kind.label()} · scade il ${
                                    goalDateFormat.format(Date(goal.dueDateMillis))
                                } · media ${goalAverageProgress(goal)}% · ${goalHistory(goal).size} rilevazioni",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (goal.milestones.isNotEmpty()) {
                                Text(
                                    text = "Traguardi ${goalMilestonePercent(goal)}%",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            goal.milestones.forEach { milestone ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = milestone.done,
                                        onCheckedChange = {
                                            viewModel.toggleGoalMilestone(goal.id, milestone.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = milestone.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeGoalMilestone(goal.id, milestone.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Rimuovi traguardo",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = milestoneText,
                                    onValueChange = { milestoneText = it },
                                    label = { Text("Nuovo traguardo") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (milestoneText.isNotBlank()) {
                                            viewModel.addGoalMilestone(goal.id, milestoneText.trim())
                                            milestoneText = ""
                                        }
                                    },
                                    enabled = milestoneText.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aggiungi traguardo")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.onGoalProgress(
                                            goal.id,
                                            (goal.progressPercent - 10).coerceAtLeast(0)
                                        )
                                    }
                                ) { Text("-10") }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.onGoalProgress(
                                            goal.id,
                                            (goal.progressPercent + 10).coerceAtMost(100)
                                        )
                                    }
                                ) { Text("+10") }
                                OutlinedButton(
                                    onClick = { viewModel.onGoalProgress(goal.id, 100) }
                                ) { Text("Completa") }
                                IconButton(onClick = { viewModel.onGoalRemove(goal.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina obiettivo")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Aggiungi obiettivo")
        }

        if (showAdd) {
            AddGoalFullDialog(
                onDismiss = { showAdd = false },
                onConfirm = { title, kind, days ->
                    viewModel.addGoalFull(
                        title = title,
                        kind = kind,
                        dueDateMillis = System.currentTimeMillis() + days * 86_400_000L
                    )
                    showAdd = false
                }
            )
        }
    }
}

@Composable
private fun AddGoalFullDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, GoalKind, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(GoalKind.LONG_TERM) }
    var days by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo obiettivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = { text -> days = text.filter { it.isDigit() }.take(3) },
                    label = { Text("Scade tra (giorni)") },
                    singleLine = true
                )
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalKind.entries.forEach { value ->
                        FilterChip(
                            selected = kind == value,
                            onClick = { kind = value },
                            label = { Text(value.label(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), kind, days.toIntOrNull() ?: 30) },
                enabled = title.isNotBlank()
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
