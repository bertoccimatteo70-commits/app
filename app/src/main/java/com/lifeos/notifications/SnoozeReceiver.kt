package com.lifeos.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Handles the "postpone" action of a reminder or alarm notification: the first
 * broadcast schedules the same notification ten minutes later, the second one
 * (action [ACTION_FIRE]) posts it again with the action still available.
 */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val itemId = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            .ifEmpty { "LifeOS" }
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val snooze = LifeOSNotifications.Snooze(kind, itemId, title, text)

        when (intent.action) {
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context)
                    .cancel(ReminderScheduler.requestCode(notificationIdFor(kind, itemId)))
                val fire = Intent(context, SnoozeReceiver::class.java).apply {
                    action = ACTION_FIRE
                    putExtra(EXTRA_KIND, kind)
                    putExtra(EXTRA_ID, itemId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_TEXT, text)
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    ReminderScheduler.requestCode("snooze-$kind-$itemId"),
                    fire,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                ReminderScheduler.scheduleAt(
                    context,
                    System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
                    pending
                )
            }
            ACTION_FIRE -> {
                // Re-posts the same notification; the alarm/reminder keeps its own schedule.
                LifeOSNotifications.notify(
                    context = context,
                    id = ReminderScheduler.requestCode(notificationIdFor(kind, itemId)),
                    channel = if (kind == KIND_ALARM) LifeOSNotifications.CHANNEL_ALARMS
                    else LifeOSNotifications.CHANNEL_REMINDERS,
                    title = snooze.title,
                    text = snooze.text,
                    snooze = snooze
                )
            }
        }
    }

    companion object {
        const val KIND_ALARM = "alarm"
        const val KIND_REMINDER = "reminder"
        const val SNOOZE_MINUTES = 10

        const val ACTION_SNOOZE = "com.lifeos.notifications.SNOOZE"
        const val ACTION_FIRE = "com.lifeos.notifications.SNOOZE_FIRE"

        const val EXTRA_KIND = "snooze_kind"
        const val EXTRA_ID = "snooze_id"
        const val EXTRA_TITLE = "snooze_title"
        const val EXTRA_TEXT = "snooze_text"

        /** Matches the notification id used by the reminder/alarm receivers. */
        fun notificationIdFor(kind: String, itemId: String): String =
            if (kind == KIND_ALARM) "alarm-$itemId" else itemId

        fun pendingIntent(context: Context, snooze: LifeOSNotifications.Snooze): PendingIntent {
            val intent = Intent(context, SnoozeReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_KIND, snooze.kind)
                putExtra(EXTRA_ID, snooze.itemId)
                putExtra(EXTRA_TITLE, snooze.title)
                putExtra(EXTRA_TEXT, snooze.text)
            }
            return PendingIntent.getBroadcast(
                context,
                ReminderScheduler.requestCode("snooze-${snooze.kind}-${snooze.itemId}"),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
