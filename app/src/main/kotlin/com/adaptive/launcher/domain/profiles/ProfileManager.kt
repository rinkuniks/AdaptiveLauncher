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
        val handles = try { provider.getProfiles() } catch (_: Exception) { listOf(android.os.Process.myUserHandle()) }
        return handles.mapIndexed { idx, h ->
            // Private profile detection: best-effort on API 34+ via reflection; otherwise heuristic
            val isPrivate = try {
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    val um = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
                    // Use reflection to avoid compile-time dependency on new API if toolchain varies
                    try {
                        val m = um.javaClass.getMethod("isPrivateProfile")
                        (m.invoke(um) as? Boolean) == true
                    } catch (_: Exception) { false }
                } else false
            } catch (_: Exception) { false }
            ProfileInfo(h, h.hashCode().toLong(), isWork = idx == 1, isPrivate = isPrivate)
        }
    }
    fun isWorkApp(user: UserHandle): Boolean = getProfiles().find { it.user == user }?.isWork == true
    fun isPrivateApp(user: UserHandle): Boolean = getProfiles().find { it.user == user }?.isPrivate == true
}
