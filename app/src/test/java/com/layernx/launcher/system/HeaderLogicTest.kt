package com.layernx.launcher.system

import com.layernx.launcher.data.NotificationStore
import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderLogicTest {

    @Test
    fun bellCyclesSoundVibrateSilent() {
        assertEquals(RingerState.VIBRATE, RingerState.next(RingerState.SOUND, canVibrate = true))
        assertEquals(RingerState.SILENT, RingerState.next(RingerState.VIBRATE, canVibrate = true))
        assertEquals(RingerState.SOUND, RingerState.next(RingerState.SILENT, canVibrate = true))
    }

    @Test
    fun bellSkipsVibrationWithoutVibrator() {
        assertEquals(RingerState.SILENT, RingerState.next(RingerState.SOUND, canVibrate = false))
    }

    @Test
    fun closingAppsSparesLauncherKeyboardAndPhone() {
        val selected = BackgroundApps.selectPackages(
            installed = listOf(
                "org.mozilla.firefox", "com.whatsapp", "com.layernx.launcher", "com.emporia.inputmethod.latin",
                "com.android.phone", "com.android.systemui", "android", "com.whatsapp", "",
            ),
            ownPackage = "com.layernx.launcher",
            keyboardPackage = "com.emporia.inputmethod.latin",
        )
        assertEquals(listOf("com.whatsapp", "org.mozilla.firefox"), selected)
    }

    @Test
    fun notificationsAreNewestFirstWithoutSummariesOrOwn() {
        fun item(key: String, pkg: String, time: Long, summary: Boolean = false) = NotificationStore.Item(
            key = key, packageName = pkg, title = key, text = null, postTime = time, isClearable = true,
            isGroupSummary = summary, autoCancel = true, contentIntent = null, smallIcon = null,
        )
        val selected = NotificationStore.select(
            listOf(
                item("old", "com.whatsapp", 1),
                item("summary", "com.whatsapp", 5, summary = true),
                item("new", "org.telegram.messenger", 9),
                item("own", "com.layernx.launcher", 10),
            ),
            ownPackage = "com.layernx.launcher",
        )
        assertEquals(listOf("new", "old"), selected.map { it.key })
    }
}
