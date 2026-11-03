package com.lifeos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.data.LifeOSStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifEmpty { "Sveglia" }
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 30)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = LifeOSStorage.flow(context).first().settings
                if (settings.notificationsEnabled) {
                    val text = String.format(Locale.getDefault(), "Sveglia delle %02d:%02d", hour, minute)
                    LifeOSNotifications.notify(
                        context = context,
                        id = ReminderScheduler.requestCode("alarm-$id"),
                        channel = LifeOSNotifications.CHANNEL_ALARMS,
                        title = label,
                        text = text,
                        snooze = LifeOSNotifications.Snooze(
                            kind = SnoozeReceiver.KIND_ALARM,
                            itemId = id,
                            title = label,
                            text = text
                        )
                    )
                }
                armNext(context, id, label, hour, minute)
            } finally {
                pending.finish()
            }
        }
    }

    private fun armNext(context: Context, id: String, label: String, hour: Int, minute: Int) {
        val next = ReminderScheduler.nextAlarmTime(hour, minute, System.currentTimeMillis())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode("alarm-$id"),
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (ReminderScheduler.canScheduleExact(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pendingIntent)
        }
    }

    companion object {
        const val EXTRA_ID = "alarm_id"
        const val EXTRA_LABEL = "alarm_label"
        const val EXTRA_HOUR = "alarm_hour"
        const val EXTRA_MINUTE = "alarm_minute"
    }
}
