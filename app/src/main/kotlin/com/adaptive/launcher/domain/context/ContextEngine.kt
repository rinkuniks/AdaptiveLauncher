package com.adaptive.launcher.domain.context

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

data class ContextSignal(val packageName: String, val lastLaunchMs: Long, val launchCount: Int, val hourBucket: Int, val dayOfWeek: Int)
data class ContextSuggestion(val packageName: String, val score: Float, val reason: String)

@Singleton
class ContextEngine @Inject constructor(){
    // Transparent scoring: frequency + recency + timeBucket + dayOfWeek + sequence
    fun score(signals: List<ContextSignal>, nowMs: Long, currentHourBucket: Int, currentDay: Int): List<ContextSuggestion> {
        return signals.map { s ->
            val recencyWeight = exp(-(nowMs - s.lastLaunchMs).toDouble() / (1000*3600*6)).toFloat() * 40f // 6h half
            val freqWeight = (kotlin.math.log2((s.launchCount+1).toFloat()) * 12f).coerceAtMost(30f)
            val timeWeight = if(s.hourBucket==currentHourBucket) 15f else 0f
            val dayWeight = if(s.dayOfWeek==currentDay) 5f else 0f
            val total = recencyWeight + freqWeight + timeWeight + dayWeight
            val reason = buildString{
                if(recencyWeight>8) append("Recent ")
                if(freqWeight>10) append("Frequent ")
                if(timeWeight>0) append("This time ")
            }.trim().ifEmpty{"General"}
            ContextSuggestion(s.packageName, total, reason)
        }.sortedByDescending{ it.score }.take(6)
    }
}
