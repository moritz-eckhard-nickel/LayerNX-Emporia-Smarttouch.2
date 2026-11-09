package com.layernx.launcher.core

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.layernx.launcher.R

/**
 * A task the launcher can open ("Telefon", "Browser", …). Each role knows which intents
 * an app must handle to qualify, so LayerNX can offer every suitable app — not only the
 * one the firmware pre-selected.
 */
enum class AppRole(
    val key: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    PHONE("phone", R.string.role_phone, R.drawable.ic_phone),
    MESSAGES("messages", R.string.role_messages, R.drawable.ic_messages),
    CAMERA("camera", R.string.role_camera, R.drawable.ic_camera),
    CONTACTS("contacts", R.string.role_contacts, R.drawable.ic_contacts),
    BROWSER("browser", R.string.role_browser, R.drawable.ic_browser),
    GALLERY("gallery", R.string.role_gallery, R.drawable.ic_gallery),
    MUSIC("music", R.string.role_music, R.drawable.ic_music),
    CLOCK("clock", R.string.role_clock, R.drawable.ic_clock),
    CALENDAR("calendar", R.string.role_calendar, R.drawable.ic_calendar),
    CALCULATOR("calculator", R.string.role_calculator, R.drawable.ic_calculator),
    SETTINGS("settings", R.string.role_settings, R.drawable.ic_gear),
    ;

    /** Intents whose handlers are candidates for this role. A fresh list is built on every call. */
    fun probeIntents(): List<Intent> = when (this) {
        PHONE -> listOf(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            Intent(Intent.ACTION_DIAL),
        )
        MESSAGES -> listOf(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            mainWithCategory(Intent.CATEGORY_APP_MESSAGING),
        )
        CAMERA -> listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        CONTACTS -> listOf(
            mainWithCategory(Intent.CATEGORY_APP_CONTACTS),
            Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI),
        )
        BROWSER -> listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
                .addCategory(Intent.CATEGORY_BROWSABLE),
            mainWithCategory(Intent.CATEGORY_APP_BROWSER),
        )
        GALLERY -> listOf(
            Intent(Intent.ACTION_VIEW).setDataAndType(SAMPLE_IMAGE, "image/*"),
            mainWithCategory(Intent.CATEGORY_APP_GALLERY),
        )
        MUSIC -> listOf(
            mainWithCategory(Intent.CATEGORY_APP_MUSIC),
            Intent(Intent.ACTION_VIEW).setDataAndType(SAMPLE_AUDIO, "audio/*"),
        )
        CLOCK -> listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
        )
        CALENDAR -> listOf(
            mainWithCategory(Intent.CATEGORY_APP_CALENDAR),
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
        )
        CALCULATOR -> listOf(mainWithCategory(Intent.CATEGORY_APP_CALCULATOR))
        SETTINGS -> listOf(Intent(Settings.ACTION_SETTINGS))
    }

    /** The intent that simply *opens* an app of this role (no data, no side effects). */
    fun openIntent(): Intent = when (this) {
        PHONE -> Intent(Intent.ACTION_DIAL)
        MESSAGES -> mainWithCategory(Intent.CATEGORY_APP_MESSAGING)
        CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        CONTACTS -> mainWithCategory(Intent.CATEGORY_APP_CONTACTS)
        BROWSER -> mainWithCategory(Intent.CATEGORY_APP_BROWSER)
        GALLERY -> mainWithCategory(Intent.CATEGORY_APP_GALLERY)
        MUSIC -> mainWithCategory(Intent.CATEGORY_APP_MUSIC)
        CLOCK -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        CALENDAR -> mainWithCategory(Intent.CATEGORY_APP_CALENDAR)
        CALCULATOR -> mainWithCategory(Intent.CATEGORY_APP_CALCULATOR)
        SETTINGS -> Intent(Settings.ACTION_SETTINGS)
    }

    /**
     * Apps that win the automatic choice whenever they are installed, ahead of Android's own
     * default. "Nachrichten" means WhatsApp on this phone; SMS stays reachable via "Alle Apps".
     */
    val preferredPackages: List<String>
        get() = when (this) {
            MESSAGES -> listOf(WHATSAPP, WHATSAPP_BUSINESS)
            else -> emptyList()
        }

    /**
     * The intent Android resolves to the user's system-wide default for this role, or null
     * when Android has no notion of a default (e.g. calculators).
     */
    fun defaultProbe(): Intent? = when (this) {
        PHONE, MESSAGES, CAMERA, BROWSER, GALLERY, CLOCK, SETTINGS -> probeIntents().first()
        CONTACTS -> probeIntents()[1]
        MUSIC -> probeIntents()[1]
        CALENDAR -> probeIntents()[1]
        CALCULATOR -> null
    }

    companion object {
        const val WHATSAPP = "com.whatsapp"
        const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        private val SAMPLE_IMAGE: Uri = Uri.parse("content://media/external/images/media/1")
        private val SAMPLE_AUDIO: Uri = Uri.parse("content://media/external/audio/media/1")

        /** The default dock, matching the LayerNX mockup. */
        val DEFAULT_DOCK: List<AppRole> = listOf(PHONE, MESSAGES, CAMERA)

        fun fromKey(key: String?): AppRole? = entries.firstOrNull { it.key == key }

        private fun mainWithCategory(category: String): Intent =
            Intent(Intent.ACTION_MAIN).addCategory(category)
    }
}
