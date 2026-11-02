package com.lifeos.data

import java.util.Calendar

/** Start (00:00:00.000) and end (23:59:59.999) of the current day, in millis. */
fun todayMillisRange(now: Long = System.currentTimeMillis()): LongRange {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val end = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    return start..end
}

/** Next time [hour]:[minute] occurs at or after [from]. */
fun nextDailyTrigger(hour: Int, minute: Int, from: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = from
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (calendar.timeInMillis <= from) calendar.add(Calendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}

/** Italian greeting for the given hour of day. */
fun greetingFor(hour: Int): String = when (hour) {
    in 0..5 -> "Buonanotte"
    in 6..11 -> "Buongiorno"
    in 12..17 -> "Buon pomeriggio"
    else -> "Buonasera"
}

fun greetingFor(calendar: Calendar): String = greetingFor(calendar.get(Calendar.HOUR_OF_DAY))

/** Weekday labels Monday-first, used by the calendar screen. */
val weekdayLabels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")
