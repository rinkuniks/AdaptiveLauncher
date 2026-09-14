package com.adaptive.launcher.core.icons

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconEngine @Inject constructor(@ApplicationContext private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    fun getIcon(packageName: String, user: UserHandle, density: Int = 0): Drawable? = try {
        // Lazy icon loading; fallback to package icon
        context.packageManager.getApplicationInfo(packageName, 0).loadIcon(context.packageManager)
    } catch (_: Exception) { null }
    fun getBadgedIcon(drawable: Drawable, user: UserHandle): Drawable = try {
        context.packageManager.getUserBadgedIcon(drawable, user)
    } catch (_: Exception) { drawable }
}
