package com.lifeos.launcher

import android.content.Context
import android.util.Log

/**
 * Launcher gestures that need a system service. Both are best effort: Android
 * may deny or remove the API, and the caller shows the real outcome instead of
 * pretending the gesture worked.
 */
object SystemGestures {

    /** Expands the notification shade using the hidden StatusBarManager API. */
    fun expandNotifications(context: Context): Boolean = runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        val manager = Class.forName("android.app.StatusBarManager")
        val method = manager.getMethod("expandNotificationsPanel")
        method.invoke(service)
        true
    }.onFailure { Log.w("LifeOS", "Apertura notifiche non disponibile: ${it.message}") }
        .getOrDefault(false)

    fun collapseNotifications(context: Context): Boolean = runCatching {
        val service = context.getSystemService("statusbar") ?: return false
        val manager = Class.forName("android.app.StatusBarManager")
        manager.getMethod("collapsePanels").invoke(service)
        true
    }.getOrDefault(false)
}
