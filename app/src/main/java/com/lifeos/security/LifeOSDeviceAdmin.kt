package com.lifeos.security

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Device admin receiver used only to allow "double tap to lock the screen". */
class LifeOSDeviceAdmin : DeviceAdminReceiver() {
    companion object {
        fun component(context: Context): ComponentName =
            ComponentName(context, LifeOSDeviceAdmin::class.java)
    }
}

/**
 * Screen locking through the device administration API. Locking is granted by
 * the user explicitly activating the admin; without it LifeOS never locks the
 * device and instead offers the activation screen.
 */
object ScreenLocker {

    fun isActive(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(DevicePolicyManager::class.java)
        manager.isAdminActive(LifeOSDeviceAdmin.component(context))
    }.getOrDefault(false)

    fun activationIntent(context: Context): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, LifeOSDeviceAdmin.component(context))
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "LifeOS usa questo permesso solo per bloccare lo schermo con il doppio tocco."
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Locks now, or returns false when the admin is not active. */
    fun lock(context: Context): Boolean = runCatching {
        if (!isActive(context)) return false
        context.getSystemService(DevicePolicyManager::class.java).lockNow()
        true
    }.getOrDefault(false)
}
