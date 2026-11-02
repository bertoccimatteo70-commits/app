package com.lifeos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifeos.data.Alarm
import com.lifeos.data.LifeOSState
import com.lifeos.data.Reminder
import com.lifeos.data.RepeatRule
import java.util.Calendar

/**
 * Schedules reminders and alarms with AlarmManager so they fire even when the
 * app is closed. Exact alarms are used when the platform allows it, otherwise
 * AlarmManager falls back to an inexact (but still wake-up) alarm.
 */
object ReminderScheduler {

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    fun requestCode(id: String): Int = id.hashCode() and 0x7FFFFFFF

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.enabled || reminder.triggerAtMillis <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_REPEAT, reminder.repeat.name)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, reminder.triggerAtMillis, pending)
    }

    fun cancel(context: Context, reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pending)
        pending.cancel()
    }

    fun scheduleAlarm(context: Context, alarm: Alarm, from: Long = System.currentTimeMillis()) {
        if (!alarm.enabled) return
        val trigger = nextAlarmTime(alarm.hour, alarm.minute, from)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_MINUTE, alarm.minute)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode("alarm-${alarm.id}"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, trigger, pending)
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode("alarm-$alarmId"),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)?.cancel(pending)
        pending.cancel()
    }

    /** Re-arms everything enabled; used at boot and after a backup import. */
    fun rescheduleAll(context: Context, state: LifeOSState) {
        state.reminders.filter { it.enabled }.forEach { schedule(context, it) }
        state.alarms.filter { it.enabled }.forEach { scheduleAlarm(context, it) }
    }

    fun nextAlarmTime(hour: Int, minute: Int, from: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= from) calendar.add(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }

    private fun setAlarm(context: Context, triggerAt: Long, pending: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (canScheduleExact(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun repeatOf(name: String?): RepeatRule =
        name?.let { runCatching { RepeatRule.valueOf(it) }.getOrNull() } ?: RepeatRule.NONE
}
