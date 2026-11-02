package com.lifeos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar

/** The widgets that can be placed on the Widget tab. */
enum class WidgetType(val label: String, val icon: ImageVector) {
    ACTIVITY_LIST("Attività", Icons.Default.CheckCircle),
    RECENT_NOTES("Note", Icons.Default.StickyNote2),
    GOALS("Obiettivi", Icons.Default.Flag),
    CALENDAR("Calendario", Icons.Default.CalendarMonth)
}

/** A widget on the user's dashboard: which widget it is and whether it is shown. */
data class WidgetPlacement(
    val id: String,
    val type: WidgetType,
    val visible: Boolean = true
)

/** The placement shown the first time the Widget tab is opened. */
val defaultPlacements: List<WidgetPlacement> = listOf(
    WidgetPlacement(id = "widget-activity", type = WidgetType.ACTIVITY_LIST),
    WidgetPlacement(id = "widget-notes", type = WidgetType.RECENT_NOTES),
    WidgetPlacement(id = "widget-goals", type = WidgetType.GOALS),
    WidgetPlacement(id = "widget-calendar", type = WidgetType.CALENDAR)
)

/**
 * Moves the widget at [from] to [to], keeping every other widget in order.
 * Returns a new list; the input list is not modified.
 */
fun reorderWidgets(
    placements: List<WidgetPlacement>,
    from: Int,
    to: Int
): List<WidgetPlacement> {
    if (placements.isEmpty()) return placements
    val start = from.coerceIn(0, placements.lastIndex)
    val end = to.coerceIn(0, placements.lastIndex)
    if (start == end) return placements.toList()
    val mutable = placements.toMutableList()
    val moved = mutable.removeAt(start)
    mutable.add(end, moved)
    return mutable
}

/** Start (00:00:00.000) and end (23:59:59.999) of the current day, in millis. */
fun todayMillisRange(): LongRange {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return start..end
}
