package com.adaptive.launcher.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemLauncherAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LauncherAppsProvider {
    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    private var callback: LauncherApps.Callback? = null

    override fun getLauncherActivities(): List<LauncherActivityInfo> {
        val profiles = getProfiles()
        return profiles.flatMap { user -> launcherApps.getActivityList(null, user) }
    }

    override fun getProfiles(): List<UserHandle> = try {
        userManager.userProfiles
    } catch (_: Exception) {
        listOf(android.os.Process.myUserHandle())
    }

    override fun isSystemApp(packageName: String): Boolean = try {
        val ai = context.packageManager.getApplicationInfo(packageName, 0)
        (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (_: Exception) { false }

    override fun startMainActivity(packageName: String, activityName: String, user: UserHandle) {
        val cn = android.content.ComponentName(packageName, activityName)
        launcherApps.startMainActivity(cn, user, null, null)
    }

    override fun registerCallback(callback: LauncherAppsProvider.Callback) {
        val sysCb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = callback.onPackagesChanged()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = callback.onPackagesChanged()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = callback.onPackagesChanged()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = callback.onPackagesChanged()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = callback.onPackagesChanged()
        }
        this.callback = sysCb
        launcherApps.registerCallback(sysCb)
    }

    override fun unregisterCallback(callback: LauncherAppsProvider.Callback) {
        this.callback?.let { launcherApps.unregisterCallback(it) }
        this.callback = null
    }
}
