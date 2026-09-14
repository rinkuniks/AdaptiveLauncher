package com.adaptive.launcher.data.shortcuts

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    fun getShortcuts(packageName: String, user: UserHandle): List<ShortcutInfo> = try {
        val q = LauncherApps.ShortcutQuery().setPackage(packageName).setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
        launcherApps.getShortcuts(q, user) ?: emptyList()
    } catch (_: Exception) { emptyList() }
    fun startShortcut(shortcut: ShortcutInfo) = try { launcherApps.startShortcut(shortcut, null, null) } catch (_: Exception) {}
}
