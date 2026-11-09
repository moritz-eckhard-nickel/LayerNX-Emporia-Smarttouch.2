package com.layernx.launcher.system

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.layernx.launcher.R

/** Thin, crash-safe wrappers around system screens. OEM firmwares hide some of them. */
object SystemActions {

    const val REQUEST_HOME_ROLE = 4711

    /** True if LayerNX is the current HOME app. */
    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roles.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /**
     * Asks Android to make LayerNX the HOME app. Uses the official role dialog where
     * available and falls back to the "Home app" settings screen.
     */
    fun requestDefaultLauncher(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roles.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                try {
                    @Suppress("DEPRECATION")
                    activity.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE)
                    return
                } catch (e: ActivityNotFoundException) {
                    // fall through to settings
                } catch (e: SecurityException) {
                    // fall through to settings
                }
            }
        }
        openHomeSettings(activity)
    }

    /** "Home app" chooser – also the documented way back to the Emporia launcher. */
    fun openHomeSettings(context: Context) {
        startFirst(
            context,
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun openDefaultAppsSettings(context: Context) {
        startFirst(
            context,
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun openSystemSettings(context: Context) {
        startFirst(context, Intent(Settings.ACTION_SETTINGS))
    }

    /** Starts the first intent that works. Returns false (and tells the user) if none does. */
    fun startFirst(context: Context, vararg intents: Intent): Boolean {
        for (intent in intents) {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (safeStart(context, intent)) return true
        }
        Toast.makeText(context, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        return false
    }

    fun safeStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
