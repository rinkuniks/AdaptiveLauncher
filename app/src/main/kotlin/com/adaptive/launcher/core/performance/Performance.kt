package com.adaptive.launcher.core.performance

import android.os.Trace
import androidx.compose.runtime.Composable

inline fun <T> traceSection(name: String, block: ()->T): T{ Trace.beginSection(name); try{ return block()} finally{ Trace.endSection() } }

object BaselineHelper{
    // Hook for Macrobenchmark Baseline Profiles – placeholder
    fun generate(){}
}
