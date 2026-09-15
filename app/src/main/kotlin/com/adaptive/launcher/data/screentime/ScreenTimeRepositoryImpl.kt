package com.adaptive.launcher.data.screentime

import android.util.Log
import com.adaptive.launcher.domain.screentime.AppUsage
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepositoryImpl @Inject constructor(
    private val appUsageDao: AppUsageDao
) : ScreenTimeRepository {
    private val appSessions = ConcurrentHashMap<String, Long>()

    override val allUsageFlow: Flow<List<AppUsage>> = appUsageDao.getAllUsageFlow().map { entities ->
        entities.map { u -> AppUsage(packageName = u.packageName.substringBeforeLast("-"), totalTime = u.totalTime) }
    }

    override fun onAppOpened(packageName: String) {
        appSessions[packageName] = System.currentTimeMillis()
    }

    override fun hasActiveSession(): Boolean = appSessions.isNotEmpty()

    override fun getActiveSessionPackageName(): String? = appSessions.keys.firstOrNull()

    override suspend fun onAppClosed(packageName: String): Int {
        val openTime = appSessions[packageName] ?: return 0
        val usageTime = System.currentTimeMillis() - openTime
        val currentDate = getCurrentDate()
        val appKey = "$packageName-$currentDate"
        return try {
            val existing = appUsageDao.getAppUsage(appKey)
            val updated = (existing?.totalTime ?: 0L) + usageTime
            appUsageDao.insertOrUpdate(AppUsageEntity(packageName = appKey, totalTime = updated))
            appSessions.remove(packageName)
            1
        } catch (e: Exception) {
            Log.e("ScreenTimeRepository", "Error saving usage: ${e.message}")
            0
        }
    }

    override suspend fun clearOldData() {
        val today = getCurrentDate()
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        try { appUsageDao.deleteOldDataExcept("%-$today", "%-$yesterday") } catch (e: Exception) {
            Log.e("ScreenTimeRepository", "Error clearing old data: ${e.message}")
        }
    }

    override suspend fun getTotalUsageForDate(date: String): Long =
        appUsageDao.getTotalUsageForDate("%-$date") ?: 0L

    override fun getTotalUsageForDateFlow(date: String): Flow<Long> =
        appUsageDao.getTotalUsageForDateFlow("%-$date").map { it ?: 0L }

    override suspend fun getUsageForApp(packageName: String, date: String): Long =
        appUsageDao.getAppUsage("$packageName-$date")?.totalTime ?: 0L

    override suspend fun getScreenTimeListSorted(date: String): List<AppUsage> =
        appUsageDao.getUsageListForDate("%-$date").map { u ->
            AppUsage(packageName = u.packageName.substringBeforeLast("-$date"), totalTime = u.totalTime)
        }.sortedByDescending { it.totalTime }

    override fun getScreenTimeListSortedFlow(date: String): Flow<List<AppUsage>> =
        appUsageDao.getUsageListForDateFlow("%-$date").map { list ->
            list.map { u -> AppUsage(packageName = u.packageName.substringBeforeLast("-$date"), totalTime = u.totalTime) }
                .sortedByDescending { it.totalTime }
        }

    private fun getCurrentDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
