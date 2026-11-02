package com.lifeos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LifeOSState
import com.lifeos.data.WidgetKind
import com.lifeos.data.WidgetSetting
import com.lifeos.data.greetingFor
import com.lifeos.data.todayMillisRange
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.MonthGrid
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.components.StatCard
import com.lifeos.ui.components.icon
import com.lifeos.ui.components.label
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun DashboardScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ClockCard(now = now, greeting = greetingFor(java.util.Calendar.getInstance())) }
        item { StatsRow(state = state) }

        item { SectionHeader("Widget") }
        val widgets = state.settings.dashboardWidgets
        items(widgets, key = { it.kind.name }) { setting ->
            WidgetPanel(
                setting = setting,
                state = state,
                index = widgets.indexOf(setting),
                total = widgets.size,
                onMoveUp = { viewModel.onWidgetReorder(widgets.indexOf(setting), widgets.indexOf(setting) - 1) },
                onMoveDown = { viewModel.onWidgetReorder(widgets.indexOf(setting), widgets.indexOf(setting) + 1) },
                onToggle = { viewModel.onWidgetToggle(setting.kind) }
            )
        }

        item { SectionHeader("Attività di oggi") }
        val openTasks = state.activities.filter { !it.done }.take(5)
        if (openTasks.isEmpty()) {
            item { EmptyLine("Nessuna attività aperta.") }
        } else {
            items(openTasks, key = { it.id }) { activity ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = activity.title.ifEmpty { "Attività" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item { SectionHeader("Scadenze di oggi") }
        val today = todayMillisRange(now)
        val todayDeadlines = state.deadlines.filter { it.dueDateMillis in today }
        if (todayDeadlines.isEmpty()) {
            item { EmptyLine("Nessuna scadenza oggi.") }
        } else {
            items(todayDeadlines, key = { it.id }) { deadline ->
                EmptyLine("• ${deadline.title.ifEmpty { "Scadenza" }} · ${timeFormat.format(Date(deadline.dueDateMillis))}")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ClockCard(now: Long, greeting: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = clockFormat.format(Date(now)),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dateFormat.format(Date(now)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(state: LifeOSState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile("Note", state.notes.size.toString(), Icons.Default.Description)
        StatTile("Da fare", state.activities.count { !it.done }.toString(), Icons.Default.CheckCircle)
        StatTile("Oggi", state.deadlines.count { it.dueDateMillis in todayMillisRange() && !it.done }.toString(), Icons.Default.Schedule)
        StatTile("Goal", state.goals.size.toString(), Icons.Default.Flag)
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String, icon: ImageVector) {
    StatCard(
        label = label,
        value = value,
        modifier = Modifier.weight(1f),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun WidgetPanel(
    setting: WidgetSetting,
    state: LifeOSState,
    index: Int,
    total: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = setting.kind.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = setting.kind.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Su", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Giù", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (setting.visible) "Nascondi" else "Mostra",
                        tint = if (setting.visible) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (setting.visible) {
                Spacer(modifier = Modifier.height(8.dp))
                when (setting.kind) {
                    WidgetKind.ACTIVITY_LIST -> state.activities.filter { !it.done }.take(4).forEach {
                        EmptyLine("• ${it.title.ifEmpty { "Attività" }}")
                    }.also { if (state.activities.none { !it.done }) EmptyLine("Nessuna attività aperta.") }

                    WidgetKind.RECENT_NOTES -> state.notes.sortedByDescending { it.createdAt }.take(3).forEach {
                        EmptyLine("• ${it.title.ifEmpty { "Nota" }}")
                    }.also { if (state.notes.isEmpty()) EmptyLine("Nessuna nota.") }

                    WidgetKind.GOALS -> state.goals.take(3).forEach { goal ->
                        Text(
                            text = goal.title.ifEmpty { "Obiettivo" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { goal.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }.also { if (state.goals.isEmpty()) EmptyLine("Nessun obiettivo.") }

                    WidgetKind.REMINDERS -> {
                        val upcoming = state.reminders.filter { it.enabled }.sortedBy { it.triggerAtMillis }.take(3)
                        if (upcoming.isEmpty()) EmptyLine("Nessun promemoria attivo.")
                        upcoming.forEach {
                            EmptyLine("• ${it.title} · ${dateFormat.format(Date(it.triggerAtMillis))} ${timeFormat.format(Date(it.triggerAtMillis))}")
                        }
                    }

                    WidgetKind.CALENDAR -> MiniCalendar()
                }
            } else {
                EmptyLine("Widget nascosto.")
            }
        }
    }
}

@Composable
private fun MiniCalendar() {
    val calendar = java.util.Calendar.getInstance()
    val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    Text(
        text = month,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    MonthGrid(
        month = calendar.get(java.util.Calendar.MONTH),
        year = calendar.get(java.util.Calendar.YEAR),
        today = calendar.get(java.util.Calendar.DAY_OF_MONTH),
        marks = emptySet()
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
