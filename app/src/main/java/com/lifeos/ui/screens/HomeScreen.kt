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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LifeOSState
import com.lifeos.data.greetingFor
import com.lifeos.data.todayMillisRange
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.ActivityRow
import com.lifeos.ui.components.DeadlineChip
import com.lifeos.ui.components.EmptyPlaceholder
import com.lifeos.ui.components.GoalBar
import com.lifeos.ui.components.NoteCard
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.theme.LocalCompactLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val homeDateFormat = SimpleDateFormat("EEEE dd MMMM", Locale.getDefault())
private val homeTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun HomeScreen(viewModel: LifeOSViewModel, onOpenTab: (Tab) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = LocalCompactLayout.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (compact) 8.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        item {
            TodaySummaryCard(
                greeting = greetingFor(java.util.Calendar.getInstance()),
                dateText = homeDateFormat.format(Date()),
                timeText = homeTimeFormat.format(Date())
            )
        }

        item { QuickAccessRow(onOpenTab = onOpenTab) }

        item { DailyProgressCard(state = state) }

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
            items(openActivities.take(4), key = { it.id }) { activity ->
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
            items(openDeadlines.take(4), key = { it.id }) { deadline ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    DeadlineChip(
                        deadline = deadline,
                        onToggle = viewModel::onDeadlineToggle,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        val todayReminders = state.reminders.filter {
            it.enabled && it.triggerAtMillis in todayMillisRange()
        }
        if (todayReminders.isNotEmpty()) {
            item { SectionHeader("Promemoria di oggi") }
            items(todayReminders, key = { it.id }) { reminder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${homeTimeFormat.format(Date(reminder.triggerAtMillis))} · ${reminder.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
            items(
                state.notes.sortedWith(compareByDescending<com.lifeos.data.Note> { it.pinned }
                    .thenByDescending { it.createdAt }).take(3),
                key = { it.id }
            ) { note ->
                NoteCard(note = note, onRemove = { viewModel.onNoteRemove(note.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
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
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun QuickAccessRow(onOpenTab: (Tab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { QuickTile("Launcher", Icons.Default.Home) { onOpenTab(Tab.Launcher) } }
        item { QuickTile("Attività", Icons.Default.CheckCircle) { onOpenTab(Tab.Activity) } }
        item { QuickTile("Note", Icons.Default.Description) { onOpenTab(Tab.Notes) } }
        item { QuickTile("Calendario", Icons.Default.Schedule) { onOpenTab(Tab.Calendar) } }
        item { QuickTile("Obiettivi", Icons.Default.Flag) { onOpenTab(Tab.Goals) } }
        item { QuickTile("Strumenti", Icons.Default.Notifications) { onOpenTab(Tab.Tools) } }
    }
}

@Composable
private fun QuickTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
private fun DailyProgressCard(state: LifeOSState) {
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

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Promemoria attivi: ${state.reminders.count { it.enabled }} · " +
                    "Sveglie: ${state.alarms.count { it.enabled }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
