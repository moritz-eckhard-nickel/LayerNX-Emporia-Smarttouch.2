package com.layernx.launcher.system

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.layernx.launcher.R
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.data.AppEntry
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.data.RoleResolver

/** Starts apps. Every path is guarded: a broken or removed app only produces a short message. */
class Launcher(private val activity: Activity) {

    val prefs = Prefs(activity)
    val resolver = RoleResolver(activity, prefs)
    private val launcherApps: LauncherApps = activity.getSystemService(LauncherApps::class.java)

    fun launch(entry: AppEntry, source: View? = null): Boolean = try {
        launcherApps.startMainActivity(entry.component, entry.user, boundsOf(source), null)
        true
    } catch (e: RuntimeException) {
        // ActivityNotFound/Security/IllegalState – or an OEM framework quirk. The home screen
        // must survive any of them; retry once through a plain explicit intent.
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        SystemActions.safeStart(activity, intent) || failed(entry.label)
    }

    /**
     * Opens the app chosen for [role]. Returns false only if no suitable app is installed;
     * the caller should then let the user pick one.
     */
    fun launch(role: AppRole, source: View? = null): Boolean {
        val resolution = resolver.resolve(role) ?: return false
        val intent = resolver.launchIntent(role, resolution.packageName) ?: return false
        boundsOf(source)?.let { intent.sourceBounds = it }
        if (!SystemActions.safeStart(activity, intent)) failed(resolution.label)
        return true
    }

    /**
     * Opens the dialer with [number] pre-filled. Uses ACTION_DIAL, never ACTION_CALL: the call
     * itself (including emergency numbers) is always placed by the system phone app.
     */
    fun dial(number: String): Boolean {
        var intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
        resolver.resolve(AppRole.PHONE)?.packageName?.let { pkg ->
            if (resolver.canHandle(pkg, intent)) intent = intent.setPackage(pkg)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (SystemActions.safeStart(activity, intent)) return true
        // The chosen app refused; let Android pick any dialer.
        return SystemActions.safeStart(activity, intent.setPackage(null)) ||
            failed(activity.getString(R.string.role_phone))
    }

    fun openAppInfo(entry: AppEntry) {
        try {
            launcherApps.startAppDetailsActivity(entry.component, entry.user, null, null)
        } catch (e: RuntimeException) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", entry.packageName, null))
            if (!SystemActions.safeStart(activity, intent)) failed(entry.label)
        }
    }

    private fun failed(label: String): Boolean {
        Toast.makeText(activity, activity.getString(R.string.launch_failed, label), Toast.LENGTH_SHORT).show()
        return false
    }

    private fun boundsOf(view: View?): Rect? {
        if (view == null || !view.isAttachedToWindow) return null
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }
}
