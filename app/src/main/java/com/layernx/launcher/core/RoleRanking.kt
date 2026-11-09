package com.layernx.launcher.core

/** An app that can fulfil a launcher role (e.g. "Browser"), reduced to what ranking needs. */
data class RoleCandidate(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
)

/**
 * Decides which app LayerNX opens for a role when the user has not picked one.
 *
 * Order (best first):
 * 1. the app Android itself would use (the user's system-wide default), unless it is an
 *    Emporia app and Emporia apps are to be avoided,
 * 2. apps the user installed themselves (e.g. Firefox, Open Camera, Simple Gallery),
 * 3. standard Android / chipset apps that came with the phone (AOSP dialer, MediaTek camera),
 * 4. Emporia apps — only as a last resort so a role never ends up without an app.
 */
object RoleRanking {

    private val OEM_PACKAGE_PREFIXES = listOf("com.emporia")

    fun isOemPackage(packageName: String): Boolean =
        OEM_PACKAGE_PREFIXES.any { packageName == it || packageName.startsWith("$it.") }

    fun rank(
        candidates: List<RoleCandidate>,
        systemDefaultPackage: String?,
        avoidOem: Boolean,
    ): List<RoleCandidate> {
        val unique = candidates.distinctBy { it.packageName }
        return unique.sortedWith(
            compareBy<RoleCandidate> { score(it, systemDefaultPackage, avoidOem) }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName },
        )
    }

    fun choose(
        candidates: List<RoleCandidate>,
        systemDefaultPackage: String?,
        avoidOem: Boolean,
    ): RoleCandidate? = rank(candidates, systemDefaultPackage, avoidOem).firstOrNull()

    private fun score(candidate: RoleCandidate, systemDefault: String?, avoidOem: Boolean): Int {
        val oem = avoidOem && isOemPackage(candidate.packageName)
        val isDefault = candidate.packageName == systemDefault
        return when {
            oem && isDefault -> 3
            oem -> 4
            isDefault -> 0
            !candidate.isSystemApp -> 1
            else -> 2
        }
    }
}
