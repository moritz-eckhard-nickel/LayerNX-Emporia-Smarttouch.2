package com.layernx.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Source of truth for launchable apps. Loads off the main thread and refreshes itself
 * through [LauncherApps.Callback] whenever apps are installed, updated or removed —
 * no polling, no background work while nothing changes.
 *
 * All public members must be used from the main thread.
 */
object AppRepository {

    private const val TAG = "LayerNX"

    fun interface Listener {
        fun onAppsChanged(apps: List<AppEntry>)
    }

    private val main = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "layernx-apps").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val listeners = LinkedHashSet<Listener>()

    private lateinit var appContext: Context
    private var generation = 0
    private var loadedLocale: Locale? = null

    var apps: List<AppEntry> = emptyList()
        private set

    var isLoaded: Boolean = false
        private set

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = packagesChanged()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = packagesChanged()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = packagesChanged()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            packagesChanged()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) =
            packagesChanged()
    }

    private fun packagesChanged() {
        // Icons may have changed with an update; drop them so they are re-rendered.
        IconCache.clear()
        reload()
    }

    fun init(context: Context) {
        val application = context.applicationContext
        if (::appContext.isInitialized) {
            if (appContext === application) return
            // Only reachable when a process hosts a new Application instance (unit tests).
            launcherApps().unregisterCallback(callback)
        }
        appContext = application
        launcherApps().registerCallback(callback, main)
        reload()
    }

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    /** Reloads if labels were loaded for a different system language. */
    fun refreshIfStale() {
        if (loadedLocale != Locale.getDefault()) reload()
    }

    fun reload() {
        val token = ++generation
        val locale = Locale.getDefault()
        executor.execute {
            val loaded = try {
                load(locale)
            } catch (e: RuntimeException) {
                // A misbehaving package must never take the home screen down.
                Log.w(TAG, "Loading apps failed: ${e.javaClass.simpleName}")
                null
            }
            main.post {
                if (token != generation || loaded == null) return@post
                apps = loaded
                isLoaded = true
                loadedLocale = locale
                listeners.toList().forEach { it.onAppsChanged(loaded) }
            }
        }
    }

    fun findByPackage(packageName: String): AppEntry? =
        apps.firstOrNull { it.packageName == packageName && it.user == Process.myUserHandle() }
            ?: apps.firstOrNull { it.packageName == packageName }

    fun findByKey(key: String): AppEntry? = apps.firstOrNull { it.key == key }

    private fun load(locale: Locale): List<AppEntry> {
        val launcherApps = launcherApps()
        val userManager = appContext.getSystemService(UserManager::class.java)
        val users: List<UserHandle> = userManager?.userProfiles?.takeIf { it.isNotEmpty() }
            ?: listOf(Process.myUserHandle())
        val ownPackage = appContext.packageName
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

        return users.asSequence()
            .flatMap { user -> launcherApps.getActivityList(null, user).asSequence() }
            .filter { it.componentName.packageName != ownPackage }
            .map { info ->
                val label = info.label?.toString()?.trim().orEmpty()
                    .ifEmpty { info.componentName.packageName }
                AppEntry(info, label)
            }
            .distinctBy { it.key }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
            .toList()
    }

    private fun launcherApps(): LauncherApps =
        appContext.getSystemService(LauncherApps::class.java)
}
