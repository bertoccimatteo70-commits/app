package com.lifeos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.data.LifeOSStorage
import com.lifeos.data.RepeatRule
import com.lifeos.data.nextOccurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifEmpty { "Promemoria LifeOS" }
        val repeat = ReminderScheduler.repeatOf(intent.getStringExtra(EXTRA_REPEAT))

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = LifeOSStorage.flow(context).first().settings
                if (settings.notificationsEnabled) {
                    val text = if (repeat == RepeatRule.NONE) "Promemoria di LifeOS" else "Promemoria ricorrente"
                    LifeOSNotifications.notify(
                        context = context,
                        id = ReminderScheduler.requestCode(id),
                        channel = LifeOSNotifications.CHANNEL_REMINDERS,
                        title = title,
                        text = text,
                        snooze = LifeOSNotifications.Snooze(
                            kind = SnoozeReceiver.KIND_REMINDER,
                            itemId = id,
                            title = title,
                            text = text
                        )
                    )
                }

                if (repeat != RepeatRule.NONE) {
                    val next = nextOccurrence(repeat, System.currentTimeMillis())
                    LifeOSStorage.update(context) { state ->
                        state.copy(reminders = state.reminders.map {
                            if (it.id == id) it.copy(triggerAtMillis = next) else it
                        })
                    }
                    LifeOSStorage.flow(context).let { }
                    ReminderScheduler.schedule(
                        context,
                        com.lifeos.data.Reminder(
                            id = id,
                            title = title,
                            triggerAtMillis = next,
                            repeat = repeat
                        )
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_REPEAT = "reminder_repeat"
    }
}
