package com.adaptive.launcher.domain.screentime

import kotlinx.coroutines.flow.Flow

data class AppUsage(val packageName: String, val totalTime: Long)
data class AppUsageUiModel(val packageName: String, val appName: String, val totalTime: Long, val usageIncreased: Boolean)

interface ScreenTimeRepository {
    fun onAppOpened(packageName: String)
    suspend fun onAppClosed(packageName: String): Int
    fun hasActiveSession(): Boolean
    fun getActiveSessionPackageName(): String?
    suspend fun clearOldData()
    suspend fun getTotalUsageForDate(date: String): Long
    suspend fun getUsageForApp(packageName: String, date: String): Long
    suspend fun getScreenTimeListSorted(date: String): List<AppUsage>
    fun getScreenTimeListSortedFlow(date: String): Flow<List<AppUsage>>
    fun getTotalUsageForDateFlow(date: String): Flow<Long>
    val allUsageFlow: Flow<List<AppUsage>>
}
