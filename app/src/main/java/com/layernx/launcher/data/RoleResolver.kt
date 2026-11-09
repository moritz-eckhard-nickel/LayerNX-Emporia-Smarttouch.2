package com.layernx.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.layernx.launcher.core.AppRole
import com.layernx.launcher.core.RoleCandidate
import com.layernx.launcher.core.RoleRanking

/** Which app LayerNX opens for a role, and whether the user picked it or it was chosen automatically. */
data class RoleResolution(
    val role: AppRole,
    val packageName: String,
    val label: String,
    val isManual: Boolean,
)

/**
 * Finds every installed app that can serve an [AppRole] and picks the one to launch:
 * the user's explicit choice if it is still installed, otherwise [RoleRanking]'s choice.
 */
class RoleResolver(context: Context, private val prefs: Prefs) {

    private val pm: PackageManager = context.packageManager
    private val ownPackage: String = context.packageName

    fun candidates(role: AppRole): List<RoleCandidate> {
        val byPackage = LinkedHashMap<String, ApplicationInfo>()
        for (intent in role.probeIntents()) {
            for (info in query(intent)) {
                val activity = info.activityInfo ?: continue
                val pkg = activity.packageName
                if (pkg == ownPackage || pkg == ANDROID_PACKAGE || !activity.exported) continue
                if (!activity.applicationInfo.enabled) continue
                byPackage.putIfAbsent(pkg, activity.applicationInfo)
            }
        }
        for (pkg in role.preferredPackages) {
            if (pkg in byPackage) continue
            val info = try {
                applicationInfo(pkg)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            if (info.enabled && pm.getLaunchIntentForPackage(pkg) != null) byPackage[pkg] = info
        }
        return byPackage.map { (pkg, appInfo) ->
            RoleCandidate(
                packageName = pkg,
                label = appInfo.loadLabel(pm).toString().ifBlank { pkg },
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }
    }

    /** The package Android would pick on its own, or null if it would ask the user. */
    fun systemDefault(role: AppRole): String? {
        val probe = role.defaultProbe() ?: return null
        val resolved = resolve(probe)?.activityInfo?.packageName ?: return null
        return resolved.takeUnless { it == ANDROID_PACKAGE || it == ownPackage }
    }

    /** All apps that can be opened for [role], best first. Apps that cannot be opened are left out. */
    fun rankedCandidates(role: AppRole): List<RoleCandidate> =
        RoleRanking.rank(
            candidates(role).filter { launchIntent(role, it.packageName) != null },
            systemDefault(role),
            prefs.avoidOemApps,
            role.preferredPackages,
        )

    fun automatic(role: AppRole): RoleCandidate? = rankedCandidates(role).firstOrNull()

    fun resolve(role: AppRole): RoleResolution? {
        prefs.roleApp(role)?.let { chosen ->
            if (isLaunchable(chosen, role)) {
                return RoleResolution(role, chosen, labelOf(chosen), isManual = true)
            }
        }
        val auto = automatic(role) ?: return null
        return RoleResolution(role, auto.packageName, auto.label, isManual = false)
    }

    /**
     * Explicit intent that *opens* [packageName] for [role] – always its normal start screen.
     *
     * - One start-screen entry: that one.
     * - Several (e.g. "Telefon" and "Kontakte" in one OEM app): the entry that serves the role,
     *   otherwise the entry Android itself would open.
     * - None: only the role's plain "open" action (no data) qualifies.
     *
     * Data-carrying probes such as SENDTO "smsto:" are never used to open an app: without a
     * recipient, apps like WhatsApp close again immediately.
     */
    fun launchIntent(role: AppRole, packageName: String): Intent? {
        // queryIntentActivities already omits components disabled at runtime. The manifest-level
        // `enabled` flag must not be checked: apps such as WhatsApp enable their entry at runtime.
        val launchers = query(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(packageName),
        ).mapNotNull { it.activityInfo }.filter { it.exported }

        if (launchers.size > 1) {
            val roleComponents = (listOf(role.openIntent()) + role.probeIntents())
                .flatMap { query(Intent(it).setPackage(packageName)) }
                .mapNotNull { it.activityInfo }
                .map { it.packageName to it.name }
                .toSet()
            launchers.firstOrNull { (it.packageName to it.name) in roleComponents }
                ?.let { return mainIntent(it.packageName, it.name) }
            pm.getLaunchIntentForPackage(packageName)?.let {
                return it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
        launchers.firstOrNull()?.let { return mainIntent(it.packageName, it.name) }

        val open = role.openIntent()
        val target = query(Intent(open).setPackage(packageName)).firstOrNull()?.activityInfo
        if (target != null && target.exported) {
            return Intent(open).setClassName(target.packageName, target.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return null
    }

    private fun mainIntent(packageName: String, className: String): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(packageName, className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    fun isLaunchable(packageName: String, role: AppRole): Boolean =
        isEnabled(packageName) && launchIntent(role, packageName) != null

    /** True if [packageName] can handle [intent] (used to pin dial intents to the chosen phone app). */
    fun canHandle(packageName: String, intent: Intent): Boolean =
        query(Intent(intent).setPackage(packageName)).isNotEmpty()

    fun labelOf(packageName: String): String = try {
        val info = applicationInfo(packageName)
        info.loadLabel(pm).toString().ifBlank { packageName }
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }

    private fun isEnabled(packageName: String): Boolean = try {
        applicationInfo(packageName).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    @Suppress("DEPRECATION")
    private fun applicationInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            pm.getApplicationInfo(packageName, 0)
        }

    @Suppress("DEPRECATION")
    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

    @Suppress("DEPRECATION")
    private fun resolve(intent: Intent): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    private companion object {
        const val ANDROID_PACKAGE = "android"
    }
}
