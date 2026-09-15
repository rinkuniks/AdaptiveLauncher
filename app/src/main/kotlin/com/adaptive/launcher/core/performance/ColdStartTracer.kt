package com.adaptive.launcher.core.performance

import android.os.SystemClock
import android.util.Log

object ColdStartTracer {
    private var startMs: Long = SystemClock.elapsedRealtime()

    fun markStart() { startMs = SystemClock.elapsedRealtime() }

    fun markFirstFrame(tag: String = "ColdStart") {
        val elapsed = SystemClock.elapsedRealtime() - startMs
        Log.i("AdaptiveLauncher", "$tag first-frame ${elapsed}ms")
    }

    fun elapsed(): Long = SystemClock.elapsedRealtime() - startMs
}
