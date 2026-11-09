package com.layernx.launcher.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.layernx.launcher.R
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.core.RoleCandidate
import com.layernx.launcher.core.RoleRanking
import com.layernx.launcher.data.AppRepository
import com.layernx.launcher.data.Prefs
import com.layernx.launcher.data.RoleResolver

/** Shared choosers for "which app is my <role> app?" and "which role goes here?". */
object RolePickers {

    fun chooseAppForRole(
        activity: Activity,
        prefs: Prefs,
        resolver: RoleResolver,
        role: AppRole,
        onChanged: () -> Unit,
    ) {
        val roleName = activity.getString(role.labelRes)
        val manual = prefs.roleApp(role)?.takeIf { resolver.isLaunchable(it, role) }
        val ranked = resolver.rankedCandidates(role)
        val auto = ranked.firstOrNull()

        val items = mutableListOf<PickerItem>()
        items += PickerItem(
            title = activity.getString(R.string.role_auto),
            subtitle = auto?.let { activity.getString(R.string.role_auto_subtitle, it.label) }
                ?: activity.getString(R.string.role_auto_none),
            icon = activity.getDrawable(role.iconRes),
            checked = manual == null,
        ) {
            prefs.setRoleApp(role, null)
            onChanged()
        }
        ranked.forEach { candidate ->
            items += PickerItem(
                title = candidate.label,
                subtitle = tagFor(activity, candidate),
                icon = if (AppRepository.findByPackage(candidate.packageName) == null) {
                    applicationIcon(activity, candidate.packageName)
                } else {
                    null
                },
                app = AppRepository.findByPackage(candidate.packageName),
                checked = manual == candidate.packageName,
            ) {
                prefs.setRoleApp(role, candidate.packageName)
                onChanged()
            }
        }
        val rankedPackages = ranked.map { it.packageName }.toSet()
        items += PickerItem(
            title = activity.getString(R.string.role_other_app),
            subtitle = activity.getString(R.string.role_other_app_subtitle),
            icon = activity.getDrawable(R.drawable.ic_apps),
            checked = manual != null && manual !in rankedPackages,
            showChevron = true,
        ) {
            chooseAnyApp(activity, prefs, role, manual, onChanged)
        }

        Picker.show(
            activity,
            prefs.accent,
            activity.getString(R.string.section_default_apps),
            roleName,
            items,
        )
    }

    private fun chooseAnyApp(
        activity: Activity,
        prefs: Prefs,
        role: AppRole,
        current: String?,
        onChanged: () -> Unit,
    ) {
        val items = AppRepository.apps
            .distinctBy { it.packageName }
            .map { entry ->
                PickerItem(
                    title = entry.label,
                    subtitle = if (RoleRanking.isOemPackage(entry.packageName)) {
                        activity.getString(R.string.tag_emporia)
                    } else {
                        null
                    },
                    app = entry,
                    checked = entry.packageName == current,
                ) {
                    prefs.setRoleApp(role, entry.packageName)
                    onChanged()
                }
            }
        Picker.show(
            activity,
            prefs.accent,
            activity.getString(role.labelRes),
            activity.getString(R.string.role_other_app),
            items,
        )
    }

    fun chooseRole(
        activity: Activity,
        accent: Int,
        eyebrow: CharSequence?,
        title: CharSequence,
        current: AppRole?,
        onPick: (AppRole) -> Unit,
    ) {
        val items = AppRole.entries.map { role ->
            PickerItem(
                title = activity.getString(role.labelRes),
                icon = activity.getDrawable(role.iconRes),
                checked = role == current,
            ) { onPick(role) }
        }
        Picker.show(activity, accent, eyebrow, title, items)
    }

    private fun tagFor(activity: Activity, candidate: RoleCandidate): String = activity.getString(
        when {
            RoleRanking.isOemPackage(candidate.packageName) -> R.string.tag_emporia
            !candidate.isSystemApp -> R.string.tag_installed
            else -> R.string.tag_system
        },
    )

    private fun applicationIcon(activity: Activity, packageName: String): Drawable? = try {
        activity.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
