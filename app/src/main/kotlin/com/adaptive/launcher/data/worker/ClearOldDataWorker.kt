package com.adaptive.launcher.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Calendar
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint { fun screenTimeRepository(): ScreenTimeRepository }

class ClearOldDataWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val repo = try {
                EntryPointAccessors.fromApplication(applicationContext, WorkerEntryPoint::class.java).screenTimeRepository()
            } catch (_: Exception) { null }
            repo?.clearOldData()
            Log.d("ClearOldDataWorker", "Ran cleanup")
            Result.success()
        } catch (e: Exception) {
            Log.e("ClearOldDataWorker", "Error: ${e.message}")
            Result.failure()
        }
    }

    companion object {
        fun scheduleDailyCleanup(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<ClearOldDataWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(calculateMidnightDelay(), TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "ClearOldDataWorker",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } catch (e: Exception) { Log.e("ClearOldDataWorker", "schedule failed ${e.message}") }
        }

        private fun calculateMidnightDelay(): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis - now
        }
    }
}
