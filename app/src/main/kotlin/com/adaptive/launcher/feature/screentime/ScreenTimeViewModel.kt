package com.adaptive.launcher.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptive.launcher.domain.screentime.AppUsageUiModel
import com.adaptive.launcher.domain.screentime.GetAppUsageUiListUseCase
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val screenTimeRepository: ScreenTimeRepository,
    private val getAppUsageUiListUseCase: GetAppUsageUiListUseCase
) : ViewModel() {

    private val datesFlow: Flow<Pair<String, String>> = flow {
        while (true) {
            val now = Date()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            val cal = Calendar.getInstance().apply { time = now; add(Calendar.DAY_OF_YEAR, -1) }
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            emit(today to yesterday)
            val nextMidnight = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val delayMs = nextMidnight.timeInMillis - System.currentTimeMillis()
            delay((delayMs + 1000).milliseconds)
        }
    }.distinctUntilChanged()

    val totalUsage: StateFlow<Long> = datesFlow.flatMapLatest { (today, _) -> screenTimeRepository.getTotalUsageForDateFlow(today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val yesterdayTotalUsage: StateFlow<Long> = datesFlow.flatMapLatest { (_, y) -> screenTimeRepository.getTotalUsageForDateFlow(y) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val appUsageUiList: StateFlow<List<AppUsageUiModel>> = datesFlow.flatMapLatest { (today, y) -> getAppUsageUiListUseCase(today, y) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onAppOpened(packageName: String) = screenTimeRepository.onAppOpened(packageName)
    suspend fun onAppClosed(packageName: String) { screenTimeRepository.onAppClosed(packageName) }
    fun hasActiveSession(): Boolean = screenTimeRepository.hasActiveSession()
    fun getActiveSessionPackageName(): String? = screenTimeRepository.getActiveSessionPackageName()
    fun getScreenTime(packageName: String): Long = appUsageUiList.value.find { it.packageName == packageName }?.totalTime ?: 0L
}
