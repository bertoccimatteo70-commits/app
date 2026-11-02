package com.lifeos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifeos.data.LifeOSStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** AlarmManager alarms are dropped on reboot; re-arm the persisted ones. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LifeOSNotifications.ensureChannels(context)
                val state = LifeOSStorage.flow(context).first()
                ReminderScheduler.rescheduleAll(context, state)
            } finally {
                pending.finish()
            }
        }
    }
}
