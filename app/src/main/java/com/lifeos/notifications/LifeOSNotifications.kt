package com.lifeos.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lifeos.MainActivity
import com.lifeos.R

object LifeOSNotifications {

    const val CHANNEL_REMINDERS = "lifeos_reminders"
    const val CHANNEL_ALARMS = "lifeos_alarms"
    const val CHANNEL_TIMERS = "lifeos_timers"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Promemoria",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Promemoria e attività ricorrenti di LifeOS" }
        val alarms = NotificationChannel(
            CHANNEL_ALARMS,
            "Sveglie",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Sveglie programmate in LifeOS" }
        val timers = NotificationChannel(
            CHANNEL_TIMERS,
            "Timer e cronometro",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannels(listOf(reminders, alarms, timers))
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notify(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!hasPermission(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannels(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}
