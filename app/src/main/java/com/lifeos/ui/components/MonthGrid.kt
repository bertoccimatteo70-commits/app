package com.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifeos.data.weekdayLabels
import java.util.Calendar

/**
 * Month grid shared by the dashboard and the calendar tab.
 * [marks] contains days of the month that have events.
 */
@Composable
fun MonthGrid(
    month: Int,
    year: Int,
    today: Int,
    marks: Set<Int> = emptySet(),
    onDayClick: (Int) -> Unit = {}
) {
    val firstDay = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Monday-first index: Calendar.SUNDAY == 1.
    val leading = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val cells = List(leading) { 0 } + (1..daysInMonth).toList()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val isToday = day == today
                    val hasEvent = day in marks
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .background(
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    hasEvent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = day != 0) { if (day != 0) onDayClick(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != 0) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun MiniDotLegend(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
