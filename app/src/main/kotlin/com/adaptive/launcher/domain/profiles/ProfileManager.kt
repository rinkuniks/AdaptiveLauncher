package com.adaptive.launcher.domain.profiles

import android.content.Context
import android.os.UserHandle
import com.adaptive.launcher.data.apps.LauncherAppsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileInfo(val user: UserHandle, val serial: Long, val isWork: Boolean, val isPrivate: Boolean)

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val provider: LauncherAppsProvider
) {
    fun getProfiles(): List<ProfileInfo> {
        val handles = try{ provider.getProfiles() }catch(_:Exception){ listOf(android.os.Process.myUserHandle()) }
        return handles.mapIndexed{ idx, h ->
            // Heuristic: second profile is work; private profile detection via API 34+ not yet, treat as not private
            ProfileInfo(h, h.hashCode().toLong(), isWork = idx==1, isPrivate = false)
        }
    }
    fun isWorkApp(user: UserHandle): Boolean = getProfiles().find{ it.user==user }?.isWork == true
}
