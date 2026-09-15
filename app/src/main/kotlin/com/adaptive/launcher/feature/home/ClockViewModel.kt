package com.adaptive.launcher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import javax.inject.Singleton

data class TimeParts(val hour: Int, val minute: Int, val isAm: Boolean)

@HiltViewModel
class ClockViewModel @Inject constructor() : ViewModel() {
    private val _timeParts = MutableStateFlow(currentParts(false))
    val timeParts: StateFlow<TimeParts> = _timeParts.asStateFlow()
    private var tickerJob: Job? = null

    fun startTicker(twelveHour: Boolean) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = LocalTime.now()
                val h24 = now.hour
                val isAm = h24 < 12
                val h = if (twelveHour) {
                    val hh = h24 % 12
                    if (hh == 0) 12 else hh
                } else h24
                _timeParts.value = TimeParts(h, now.minute, isAm)
                val millisUntilNextMinute = ((59 - now.second) * 1000L) + ((1_000_000_000L - now.nano) / 1_000_000L)
                delay(millisUntilNextMinute.coerceAtLeast(1L))
            }
        }
    }
    fun stopTicker() { tickerJob?.cancel(); tickerJob = null }
    override fun onCleared() { stopTicker(); super.onCleared() }
    private fun currentParts(twelveHour: Boolean): TimeParts {
        val now = LocalTime.now()
        val h24 = now.hour
        val isAm = h24 < 12
        val h = if (twelveHour) { val hh = h24 % 12; if (hh == 0) 12 else hh } else h24
        return TimeParts(h, now.minute, isAm)
    }
}
