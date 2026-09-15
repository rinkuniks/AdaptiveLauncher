package com.adaptive.launcher

import android.app.Application
import com.adaptive.launcher.core.performance.ColdStartTracer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdaptiveLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ColdStartTracer.markStart()
    }
}
