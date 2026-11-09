package com.layernx.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.layernx.launcher.core.Accent
import com.layernx.launcher.core.AppRole

/** All user choices of the launcher. Backed by a single private SharedPreferences file. */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var userName: String
        get() = sp.getString(KEY_NAME, "").orEmpty()
        set(value) = sp.edit().putString(KEY_NAME, value.trim().take(MAX_NAME_LENGTH)).apply()

    var accent: Int
        get() = Accent.sanitize(sp.getInt(KEY_ACCENT, Accent.DEFAULT))
        set(value) = sp.edit().putInt(KEY_ACCENT, Accent.sanitize(value)).apply()

    /** When choosing apps automatically, prefer standard/installed apps over Emporia apps. */
    var avoidOemApps: Boolean
        get() = sp.getBoolean(KEY_AVOID_OEM, true)
        set(value) = sp.edit().putBoolean(KEY_AVOID_OEM, value).apply()

    /** Hide Emporia apps from "All apps" (display only). */
    var hideOemApps: Boolean
        get() = sp.getBoolean(KEY_HIDE_OEM, false)
        set(value) = sp.edit().putBoolean(KEY_HIDE_OEM, value).apply()

    /** The package the user explicitly chose for [role], or null for "automatic". */
    fun roleApp(role: AppRole): String? =
        sp.getString(KEY_ROLE_PREFIX + role.key, null)?.takeIf { it.isNotBlank() }

    fun setRoleApp(role: AppRole, packageName: String?) {
        sp.edit().apply {
            if (packageName.isNullOrBlank()) remove(KEY_ROLE_PREFIX + role.key)
            else putString(KEY_ROLE_PREFIX + role.key, packageName)
        }.apply()
    }

    fun dockRole(slot: Int): AppRole {
        require(slot in 0 until DOCK_SIZE) { "Invalid dock slot $slot" }
        return AppRole.fromKey(sp.getString(KEY_DOCK_PREFIX + slot, null)) ?: AppRole.DEFAULT_DOCK[slot]
    }

    fun setDockRole(slot: Int, role: AppRole) {
        require(slot in 0 until DOCK_SIZE) { "Invalid dock slot $slot" }
        sp.edit().putString(KEY_DOCK_PREFIX + slot, role.key).apply()
    }

    /** Flattened component names of apps hidden from "All apps". */
    val hiddenApps: Set<String>
        get() = sp.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()

    fun setHidden(appKey: String, hidden: Boolean) {
        val updated = hiddenApps.toMutableSet().apply { if (hidden) add(appKey) else remove(appKey) }
        sp.edit().putStringSet(KEY_HIDDEN, updated).apply()
    }

    companion object {
        const val FILE = "layernx_prefs"
        const val DOCK_SIZE = 3
        const val MAX_NAME_LENGTH = 18

        private const val KEY_NAME = "user_name"
        private const val KEY_ACCENT = "accent"
        private const val KEY_AVOID_OEM = "avoid_oem_apps"
        private const val KEY_HIDE_OEM = "hide_oem_apps"
        private const val KEY_ROLE_PREFIX = "role_app_"
        private const val KEY_DOCK_PREFIX = "dock_slot_"
        private const val KEY_HIDDEN = "hidden_apps"
    }
}
