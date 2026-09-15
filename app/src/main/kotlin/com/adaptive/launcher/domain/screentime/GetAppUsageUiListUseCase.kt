package com.adaptive.launcher.domain.screentime

import com.adaptive.launcher.data.apps.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetAppUsageUiListUseCase @Inject constructor(
    private val screenTimeRepository: ScreenTimeRepository,
    private val appRepository: AppRepository
) {
    operator fun invoke(today: String, yesterday: String): Flow<List<AppUsageUiModel>> =
        combine(
            screenTimeRepository.getScreenTimeListSortedFlow(today),
            screenTimeRepository.getScreenTimeListSortedFlow(yesterday),
            appRepository.apps
        ) { todayUsage, yesterdayUsage, apps ->
            todayUsage.mapNotNull { entry ->
                val yesterdayEntry = yesterdayUsage.find { it.packageName == entry.packageName }
                val increased = entry.totalTime > (yesterdayEntry?.totalTime ?: 0L)
                val appName = apps.find { it.packageName == entry.packageName }?.label ?: return@mapNotNull null
                AppUsageUiModel(packageName = entry.packageName, appName = appName, totalTime = entry.totalTime, usageIncreased = increased)
            }
        }
}
