package com.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.data.LifeOSState
import com.lifeos.ui.LifeOSViewModel
import com.lifeos.ui.components.SectionHeader
import com.lifeos.ui.components.WidgetPlacement
import com.lifeos.ui.components.WidgetType
import com.lifeos.ui.components.defaultPlacements
import com.lifeos.ui.components.reorderWidgets

@Composable
fun WidgetScreen(viewModel: LifeOSViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var placements by remember { mutableStateOf(defaultPlacements) }
    val visibleWidgets = placements.filter { it.visible }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "I tuoi widget",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { placements = reorderWidgets(placements, 0, placements.lastIndex) }) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Ripristina ordine",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            placements.chunked(2).forEach { rowPlacements ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowPlacements.forEach { placement ->
                        WidgetToggle(
                            placement = placement,
                            modifier = Modifier.weight(1f),
                            onToggle = {
                                placements = placements.map {
                                    if (it.id == placement.id) it.copy(visible = !it.visible) else it
                                }
                            }
                        )
                    }
                    if (rowPlacements.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        SectionHeader("Dashboard")

        if (visibleWidgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun widget visibile.\nAttiva almeno un widget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                visibleWidgets.forEach { placement ->
                    WidgetCard(
                        placement = placement,
                        state = state,
                        onToggleActivity = viewModel::onActivityToggle,
                        onRemoveFromScreen = {
                            placements = placements.map {
                                if (it.id == placement.id) it.copy(visible = false) else it
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WidgetToggle(
    placement: WidgetPlacement,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (placement.visible) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = placement.type.icon,
                contentDescription = null,
                tint = if (placement.visible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = placement.type.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (placement.visible) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun WidgetCard(
    placement: WidgetPlacement,
    state: LifeOSState,
    onToggleActivity: (String, Boolean) -> Unit,
    onRemoveFromScreen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = placement.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = placement.type.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onRemoveFromScreen, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rimuovi widget",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            when (placement.type) {
                WidgetType.ACTIVITY_LIST -> ActivityListContent(state, onToggleActivity)
                WidgetType.RECENT_NOTES -> RecentNotesContent(state)
                WidgetType.GOALS -> GoalsContent(state)
                WidgetType.CALENDAR -> CalendarContent()
            }
        }
    }
}

@Composable
private fun ActivityListContent(
    state: LifeOSState,
    onToggleActivity: (String, Boolean) -> Unit
) {
    val active = state.activities.filter { !it.done }.take(3)
    if (active.isEmpty()) {
        WidgetEmptyText("Nessuna attività in attesa.")
        return
    }
    active.forEach { activity ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleActivity(activity.id, true) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = activity.done,
                onCheckedChange = { onToggleActivity(activity.id, it) },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = activity.title.ifEmpty { "Attività" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentNotesContent(state: LifeOSState) {
    val recent = state.notes.sortedByDescending { it.createdAt }.take(3)
    if (recent.isEmpty()) {
        WidgetEmptyText("Nessuna nota.")
        return
    }
    recent.forEach { note ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                text = note.title.ifEmpty { "Nota" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GoalsContent(state: LifeOSState) {
    if (state.goals.isEmpty()) {
        WidgetEmptyText("Nessun obiettivo.")
        return
    }
    state.goals.take(2).forEach { goal ->
        Text(
            text = goal.title.ifEmpty { "Obiettivo" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LinearProgressIndicator(
            progress = { goal.progressPercent.toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(top = 2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun CalendarContent() {
    val calendar = java.util.Calendar.getInstance()
    val monthLabel = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        .format(java.util.Date())
    Text(
        text = monthLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))
    MonthGrid(
        month = calendar.get(java.util.Calendar.MONTH),
        year = calendar.get(java.util.Calendar.YEAR),
        today = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
}

@Composable
private fun MonthGrid(month: Int, year: Int, today: Int) {
    val firstDay = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, year)
        set(java.util.Calendar.MONTH, month)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val leadingBlanks = firstDay.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val cells = List(leadingBlanks) { 0 } + (1..daysInMonth).toList()

    cells.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(1.dp)
                        .background(
                            color = if (day == today) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (day == 0) "" else day.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day == today) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun WidgetEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
