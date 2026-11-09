package com.layernx.launcher.data

import android.app.Notification
import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification

/**
 * In-memory snapshot of the phone's current notifications, filled by
 * [com.layernx.launcher.system.NotificationsService]. Nothing is stored or logged.
 * Main thread only.
 */
object NotificationStore {

    class Item(
        val key: String,
        val packageName: String,
        val title: CharSequence?,
        val text: CharSequence?,
        val postTime: Long,
        val isClearable: Boolean,
        val isGroupSummary: Boolean,
        val autoCancel: Boolean,
        val contentIntent: PendingIntent?,
        val smallIcon: Icon?,
    )

    fun interface Listener {
        fun onNotificationsChanged()
    }

    private val listeners = LinkedHashSet<Listener>()

    /** True while LayerNX has notification access and the system has connected the service. */
    var isConnected: Boolean = false
        private set

    var items: List<Item> = emptyList()
        private set

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    fun update(notifications: List<StatusBarNotification>, connected: Boolean, ownPackage: String) {
        isConnected = connected
        items = select(notifications.map(::toItem), ownPackage)
        listeners.toList().forEach { it.onNotificationsChanged() }
    }

    /** Newest first; drops LayerNX's own notifications and group summaries (their children are shown). */
    fun select(all: List<Item>, ownPackage: String): List<Item> =
        all.filter { it.packageName != ownPackage && !it.isGroupSummary }
            .sortedByDescending { it.postTime }

    private fun toItem(sbn: StatusBarNotification): Item {
        val n = sbn.notification
        val extras = n.extras
        val text = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)
        return Item(
            key = sbn.key,
            packageName = sbn.packageName,
            title = extras?.getCharSequence(Notification.EXTRA_TITLE),
            text = text,
            postTime = sbn.postTime,
            isClearable = sbn.isClearable,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            autoCancel = (n.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            contentIntent = n.contentIntent,
            smallIcon = n.smallIcon,
        )
    }
}
