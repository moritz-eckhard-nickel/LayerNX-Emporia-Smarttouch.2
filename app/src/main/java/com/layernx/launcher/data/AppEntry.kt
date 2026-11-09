package com.layernx.launcher.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/** One launchable activity as shown in "All apps". */
class AppEntry(
    val info: LauncherActivityInfo,
    val label: String,
) {
    val component: ComponentName get() = info.componentName
    val packageName: String get() = info.componentName.packageName
    val user: UserHandle get() = info.user
    val isSystemApp: Boolean
        get() = (info.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

    /** Stable identifier used for hidden-app bookkeeping and icon caching. */
    val key: String = info.componentName.flattenToString() + "#" + info.user.hashCode()
}
