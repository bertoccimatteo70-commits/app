package com.lifeos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector
import com.lifeos.data.WidgetKind

fun WidgetKind.label(): String = when (this) {
    WidgetKind.ACTIVITY_LIST -> "Attività"
    WidgetKind.RECENT_NOTES -> "Note"
    WidgetKind.GOALS -> "Obiettivi"
    WidgetKind.CALENDAR -> "Calendario"
    WidgetKind.REMINDERS -> "Promemoria"
}

fun WidgetKind.icon(): ImageVector = when (this) {
    WidgetKind.ACTIVITY_LIST -> Icons.Default.CheckCircle
    WidgetKind.RECENT_NOTES -> Icons.Default.StickyNote2
    WidgetKind.GOALS -> Icons.Default.Flag
    WidgetKind.CALENDAR -> Icons.Default.CalendarMonth
    WidgetKind.REMINDERS -> Icons.Default.Notifications
}
