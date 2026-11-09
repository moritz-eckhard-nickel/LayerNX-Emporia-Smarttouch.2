package com.layernx.launcher.system

import android.app.Activity
import android.app.ActivityOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast
import com.layernx.launcher.R
import com.layernx.launcher.data.AppRepository
import com.layernx.launcher.data.NotificationStore

/** Permission screens and actions around notifications and "Bitte nicht stören". */
object NotificationAccess {

    fun component(context: Context) = ComponentName(context, NotificationsService::class.java)

    fun isGranted(context: Context): Boolean {
        val component = component(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return context.getSystemService(NotificationManager::class.java)
                ?.isNotificationListenerAccessGranted(component) == true
        }
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }

    /** After an app update the system sometimes forgets to reconnect the listener. */
    fun reconnectIfNeeded(context: Context) {
        if (NotificationsService.instance != null || !isGranted(context)) return
        try {
            NotificationListenerService.requestRebind(component(context))
        } catch (e: RuntimeException) {
            // The system binds it on its own when it can.
        }
    }

    fun openListenerSettings(activity: Activity) {
        val intents = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intents += Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component(activity).flattenToString())
        }
        intents += Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intents += Intent(Settings.ACTION_SETTINGS)
        SystemActions.startFirst(activity, *intents.toTypedArray())
    }

    fun openDndAccessSettings(activity: Activity) {
        SystemActions.startFirst(
            activity,
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
            Intent(Settings.ACTION_SOUND_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    /** Opens what the notification points to, like tapping it in a notification shade. */
    fun open(activity: Activity, launcher: Launcher, item: NotificationStore.Item) {
        val sent = item.contentIntent?.let { send(activity, it) } ?: false
        if (!sent) {
            val app = AppRepository.findByPackage(item.packageName)
            if (app == null || !launcher.launch(app, null)) {
                Toast.makeText(activity, R.string.notifications_open_failed, Toast.LENGTH_SHORT).show()
                return
            }
        }
        if (item.autoCancel) NotificationsService.dismiss(item.key)
    }

    private fun send(activity: Activity, intent: PendingIntent): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // LayerNX is in front, so it may let the notification's app start its screen.
            @Suppress("DEPRECATION")
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
            intent.send(activity, 0, null, null, null, null, options)
        } else {
            intent.send()
        }
        true
    } catch (e: PendingIntent.CanceledException) {
        false
    }
}
