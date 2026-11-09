package com.layernx.launcher.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.view.View
import com.layernx.launcher.data.AppRepository
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.io.File
import java.io.FileOutputStream

/** Recreates the relevant app landscape of a stock Emporia TOUCHsmart.2 plus a few user-installed apps. */
object FakeDevice {

    data class FakeApp(
        val pkg: String,
        val activity: String,
        val label: String,
        val system: Boolean,
        val filters: List<IntentFilter>,
        /** Further activities: class name, whether it is a start-screen entry, intent filters. */
        val extraActivities: List<Triple<String, Boolean, List<IntentFilter>>> = emptyList(),
    )

    private fun filter(action: String, category: String? = null, scheme: String? = null, mime: String? = null) =
        IntentFilter(action).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            category?.let { addCategory(it) }
            scheme?.let { addDataScheme(it) }
            mime?.let { addDataType(it) }
        }

    val APPS = listOf(
        FakeApp(
            "com.android.dialer", "com.android.dialer.main.MainActivity", "Telefon", true,
            listOf(filter(Intent.ACTION_DIAL), filter(Intent.ACTION_DIAL, scheme = "tel")),
        ),
        FakeApp(
            "com.emporia.contacts", "com.emporia.contacts.MainActivity", "Kontakte", true,
            listOf(filter(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CONTACTS), filter(Intent.ACTION_DIAL, scheme = "tel")),
        ),
        FakeApp(
            "com.emporia.mms", "com.emporia.mms.MainActivity", "Nachrichten", true,
            listOf(filter(Intent.ACTION_SENDTO, scheme = "smsto"), filter(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)),
        ),
        FakeApp(
            "com.mediatek.camera", "com.mediatek.camera.CameraLauncher", "Kamera", true,
            listOf(filter(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        ),
        FakeApp(
            "com.emporia.browser", "com.emporia.browser.MainActivity", "Browser", true,
            listOf(
                filter(Intent.ACTION_VIEW, Intent.CATEGORY_BROWSABLE, scheme = "https"),
                filter(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            ),
        ),
        FakeApp(
            "org.mozilla.firefox", "org.mozilla.firefox.App", "Firefox", false,
            listOf(
                filter(Intent.ACTION_VIEW, Intent.CATEGORY_BROWSABLE, scheme = "https"),
                filter(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
            ),
        ),
        FakeApp(
            "com.emporia.gallery", "com.emporia.gallery.MainActivity", "Galerie", true,
            listOf(filter(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY)),
        ),
        FakeApp("org.telegram.messenger", "org.telegram.ui.LaunchActivity", "Telegram", false, emptyList()),
        // WhatsApp: its start entry handles no messaging intents, a second start entry exists, and
        // "smsto:" goes to a conversation screen that closes without a recipient.
        FakeApp(
            "com.whatsapp", "com.whatsapp.Main", "WhatsApp", false, emptyList(),
            extraActivities = listOf(
                Triple("com.whatsapp.Conversation", false, listOf(filter(Intent.ACTION_SENDTO, scheme = "smsto"))),
                Triple("com.whatsapp.AlternateIcon", true, emptyList()),
            ),
        ),
        FakeApp("com.android.settings", "com.android.settings.Settings", "Einstellungen", true, emptyList()),
        FakeApp("com.emporia.deskclock", "com.emporia.deskclock.MainActivity", "Uhr", true, emptyList()),
        FakeApp("com.emporia.weather", "com.emporia.weather.MainActivity", "Wetter", true, emptyList()),
    )

    fun install(app: Application = RuntimeEnvironment.getApplication()) {
        val pm = shadowOf(app.packageManager)
        val launcherApps = shadowOf(app.getSystemService(LauncherApps::class.java))
        val user = Process.myUserHandle()
        APPS.forEach { fake ->
            val appInfo = ApplicationInfo().apply {
                packageName = fake.pkg
                name = fake.label
                nonLocalizedLabel = fake.label
                flags = if (fake.system) ApplicationInfo.FLAG_SYSTEM else 0
                enabled = true
            }
            pm.installPackage(
                PackageInfo().apply {
                    packageName = fake.pkg
                    applicationInfo = appInfo
                },
            )
            val component = ComponentName(fake.pkg, fake.activity)
            val activityInfo = pm.addActivityIfNotPresent(component).apply {
                nonLocalizedLabel = fake.label
                applicationInfo = appInfo
                exported = true
                enabled = true
            }
            pm.addOrUpdateActivity(activityInfo)
            pm.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
            fake.filters.forEach { pm.addIntentFilterForActivity(component, it) }
            fake.extraActivities.forEach { (className, isLauncher, filters) ->
                val extra = ComponentName(fake.pkg, className)
                pm.addOrUpdateActivity(
                    pm.addActivityIfNotPresent(extra).apply {
                        applicationInfo = appInfo
                        exported = true
                    },
                )
                if (isLauncher) {
                    pm.addIntentFilterForActivity(
                        extra,
                        IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                    )
                }
                filters.forEach { pm.addIntentFilterForActivity(extra, it) }
            }

            val launcherInfo = ReflectionHelpers.callConstructor(
                LauncherActivityInfo::class.java,
                ClassParameter.from(android.content.Context::class.java, app),
                ClassParameter.from(ActivityInfo::class.java, activityInfo),
                ClassParameter.from(android.os.UserHandle::class.java, user),
            )
            launcherApps.addActivity(user, launcherInfo)
        }
        AppRepository.init(app)
        AppRepository.reload()
        awaitApps(APPS.size)
    }

    /** Waits for the background loader and drains the main looper. */
    fun awaitApps(expected: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (AppRepository.apps.size == expected) return
            Thread.sleep(10)
        }
        error("Apps did not load: ${AppRepository.apps.size} of $expected")
    }

    /** Lets async icon loads finish. */
    fun settle(ms: Long = 300) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Renders [view] (optionally at its full scroll height) to build/screenshots/[name].png. */
    fun screenshot(view: View, name: String, fullHeight: Int? = null): File {
        val width = view.width
        val height = fullHeight ?: view.height
        if (fullHeight != null) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Palette.BG) // window background is not part of a content view
        view.draw(canvas)
        val dir = File("build/screenshots").apply { mkdirs() }
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
