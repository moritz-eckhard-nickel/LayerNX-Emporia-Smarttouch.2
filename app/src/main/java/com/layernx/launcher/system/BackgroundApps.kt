package com.layernx.launcher.system

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.util.concurrent.Executors

/**
 * "!" button: ends the cached background processes of all apps, like a task manager would.
 *
 * Uses [ActivityManager.killBackgroundProcesses], the only way an ordinary app may do this. It
 * never touches the app in front, foreground services (music, WhatsApp's connection), the phone
 * process or anything persistent – and it is not a force-stop: alarms, jobs and pushes still
 * start apps again when needed.
 */
object BackgroundApps {

    private val executor by lazy { Executors.newSingleThreadExecutor { r -> Thread(r, "layernx-close-apps") } }
    private val main by lazy { Handler(Looper.getMainLooper()) }

    /** Packages that are never touched. */
    private val ALWAYS_KEEP = setOf("android", "com.android.systemui", "com.android.phone", "com.android.server.telecom")

    /** Which of [installed] to close; pure so it can be unit-tested. */
    fun selectPackages(installed: Collection<String>, ownPackage: String, keyboardPackage: String?): List<String> =
        installed.asSequence()
            .filter { it.isNotBlank() }
            .filter { it != ownPackage && it != keyboardPackage && it !in ALWAYS_KEEP }
            .distinct()
            .sorted()
            .toList()

    /** Closes background apps off the main thread and reports the free memory in MB afterwards. */
    fun closeAll(context: Context, installed: Collection<String>, onDone: (freeMb: Long) -> Unit) {
        val app = context.applicationContext
        val packages = selectPackages(installed, app.packageName, keyboardPackage(app))
        val activityManager = app.getSystemService(ActivityManager::class.java)
        executor.execute {
            packages.forEach { pkg ->
                try {
                    activityManager?.killBackgroundProcesses(pkg)
                } catch (e: SecurityException) {
                    // Should not happen with KILL_BACKGROUND_PROCESSES; skip the package if it does.
                }
            }
            // Give the system a moment to reclaim the memory before measuring.
            Thread.sleep(700)
            val info = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(info)
            val freeMb = info.availMem / (1024 * 1024)
            main.post { onDone(freeMb) }
        }
    }

    private fun keyboardPackage(context: Context): String? =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
}
