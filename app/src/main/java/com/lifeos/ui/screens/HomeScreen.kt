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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lifeos.data.Activity
import com.lifeos.data.Deadline
import com.lifeos.data.Goal
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.ActivityRow
import com.lifeos.ui.components.DeadlineChip
import com.lifeos.ui.components.EmptyPlaceholder
import com.lifeos.ui.components.GoalBar
import com.lifeos.ui.components.NoteCard
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.components.todayMillisRange

private val dateFormatter = java.text.SimpleDateFormat("EEEE dd MMMM", java.util.Locale.getDefault())
private val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

@Composable
fun HomeScreen(viewModel: LifeOSViewModel, state: LifeOSState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TodaySummaryCard(
                greeting = greetingFor(java.util.Calendar.getInstance()),
                dateText = dateFormatter.format(java.util.Date()),
                timeText = timeFormatter.format(java.util.Date())
            )
        }

        item { QuickAccessRow() }

        item {
            DailyProgressCard(
                state = state,
                onGoalProgress = viewModel::onGoalProgress
            )
        }

        val openActivities = state.activities.filter { !it.done }
        item { SectionHeader("Attività") }
        if (openActivities.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nessuna attività in sospeso.",
                    icon = { PlaceholderIcon(Icons.Default.CheckCircle) }
                )
            }
        } else {
            items(openActivities, key = { it.id }) { activity ->
                ActivityRow(
                    activity = activity,
                    onToggle = viewModel::onActivityToggle,
                    onRemove = viewModel::onActivityRemove
                )
            }
        }

        val openDeadlines = state.deadlines.filter { !it.done }.sortedBy { it.dueDateMillis }
        item { SectionHeader("Scadenze") }
        if (openDeadlines.isEmpty()) {
            item {
                EmptyPlaceholder(
                    message = "Nessuna scadenza in arrivo.",
                    icon = { PlaceholderIcon(Icons.Default.Schedule) }
                )
            }
        } else {
            items(openDeadlines, key = { it.id }) { deadline ->
                DeadlineRow(deadline = deadline, onToggle = viewModel::onDeadlineToggle)
            }
        }

        if (state.goals.isNotEmpty()) {
            item { SectionHeader("Obiettivi in corso") }
            items(state.goals, key = { it.id }) { goal ->
                GoalBar(goal = goal)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (state.notes.isNotEmpty()) {
            item { SectionHeader("Ultime note") }
            items(state.notes.sortedByDescending { it.createdAt }.take(3), key = { it.id }) { note ->
                NoteCard(note = note, onRemove = { viewModel.onNoteRemove(note.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** Hour-of-day greeting; kept pure so it can be tested easily. */
fun greetingFor(calendar: java.util.Calendar): String {
    return when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..5 -> "Buonanotte"
        in 6..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }
}

@Composable
private fun PlaceholderIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(32.dp)
    )
}

@Composable
private fun TodaySummaryCard(greeting: String, dateText: String, timeText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun QuickAccessRow() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { QuickTile("Attività", Icons.Default.CheckCircle) }
        item { QuickTile("Note", Icons.Default.Description) }
        item { QuickTile("Scadenze", Icons.Default.Schedule) }
        item { QuickTile("Obiettivi", Icons.Default.Flag) }
    }
}

@Composable
private fun QuickTile(label: String, icon: ImageVector) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        ),
        modifier = Modifier.width(112.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DailyProgressCard(
    state: LifeOSState,
    onGoalProgress: (String, Int) -> Unit
) {
    val total = state.activities.size
    val done = state.activities.count { it.done }
    val progress = if (total == 0) 0f else done.toFloat() / total

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Riepilogo giornaliero",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Attività completate: $done / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            val todayDeadlines = state.deadlines.filter { it.dueDateMillis in todayMillisRange() && !it.done }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Scadenze di oggi",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (todayDeadlines.isEmpty()) {
                Text(
                    text = "Nessuna scadenza oggi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                todayDeadlines.forEach { deadline ->
                    Text(
                        text = "• ${deadline.title.ifEmpty { "Scadenza" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Obiettivi",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                state.goals.forEach { goal ->
                    GoalSliderRow(goal = goal, onProgress = onGoalProgress)
                }
            }
        }
    }
}

@Composable
private fun GoalSliderRow(goal: Goal, onProgress: (String, Int) -> Unit) {
    var sliderValue by remember(goal.progressPercent) { mutableFloatStateOf(goal.progressPercent.toFloat()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = goal.title.ifEmpty { "Obiettivo" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(96.dp)
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onProgress(goal.id, sliderValue.toInt()) },
            valueRange = 0f..100f,
            steps = 9,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${sliderValue.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DeadlineRow(deadline: Deadline, onToggle: (String, Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        DeadlineChip(deadline = deadline, onToggle = onToggle, modifier = Modifier.padding(8.dp))
    }
}

@Suppress("unused")
private fun openActivitiesOf(state: LifeOSState): List<Activity> = state.activities.filter { !it.done }
