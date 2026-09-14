package com.adaptive.launcher.data.apps

import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

interface LauncherAppsProvider {
    fun getLauncherActivities(): List<LauncherActivityInfo>
    fun getProfiles(): List<UserHandle>
    fun isSystemApp(packageName: String): Boolean = false
    fun startMainActivity(packageName: String, activityName: String, user: UserHandle)
    fun registerCallback(callback: Callback)
    fun unregisterCallback(callback: Callback)

    interface Callback {
        fun onPackagesChanged()
    }
}
