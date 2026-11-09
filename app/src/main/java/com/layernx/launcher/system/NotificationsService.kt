package com.layernx.launcher.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.layernx.launcher.data.NotificationStore

/**
 * Receives the phone's notifications once the user grants "Benachrichtigungszugriff", so the
 * "i" button can list them – the Emporia firmware offers no notification shade of its own.
 * Callbacks arrive on the main thread.
 */
class NotificationsService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        publish()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        NotificationStore.update(emptyList(), connected = false, ownPackage = packageName)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = try {
            activeNotifications?.toList().orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: RuntimeException) {
            // The binder can die while the system rebinds the listener.
            emptyList()
        }
        NotificationStore.update(active, connected = true, ownPackage = packageName)
    }

    companion object {
        /** The connected service, used to dismiss notifications. Null while not connected. */
        @Volatile
        var instance: NotificationsService? = null
            private set

        fun dismiss(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (e: RuntimeException) {
                // Listener just disconnected – nothing to dismiss.
            }
        }

        fun dismissAll() {
            try {
                instance?.cancelAllNotifications()
            } catch (e: RuntimeException) {
                // Listener just disconnected – nothing to dismiss.
            }
        }
    }
}
