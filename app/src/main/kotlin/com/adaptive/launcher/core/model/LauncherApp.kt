package com.adaptive.launcher.core.model

import android.os.UserHandle

data class LauncherApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val normalizedLabel: String,
    val user: UserHandle,
    val section: Char,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val usageScore: Float = 0f,
    val isSystemApp: Boolean = false,
)

data class FavoriteItem(
    val id: String,
    val packageName: String,
    val activityName: String,
    val position: Int,
    val userSerial: Long
)

sealed interface SearchResult {
    data class AppResult(val app: LauncherApp, val score: Float) : SearchResult
}
