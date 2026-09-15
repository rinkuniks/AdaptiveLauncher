package com.adaptive.launcher.launcher

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.adaptive.launcher.core.device.DeviceProfileFactory
import com.adaptive.launcher.core.performance.ColdStartTracer
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.domain.themes.ThemeRepository
import com.adaptive.launcher.navigation.AppNavHost
import com.adaptive.launcher.ui.theme.AdaptiveLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    @Inject lateinit var themeRepository: ThemeRepository
    @Inject lateinit var widgetRepository: WidgetRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ColdStartTracer.markStart()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        try {
            val profile = DeviceProfileFactory.fromContext(this)
            Log.i("AdaptiveLauncher", "Device: ${profile.label}; sw${profile.swDp}dp aspect=${"%.2f".format(profile.aspectRatio)} tall=${profile.isTall} cutout=${profile.cutoutTopPx}px nav=${profile.navBarHeightPx}px refresh=${profile.refreshRate}Hz")
        } catch (_: Exception) {}
        setContent {
            val mode by themeRepository.mode.collectAsState(initial = com.adaptive.launcher.domain.themes.ThemeMode.System)
            AdaptiveLauncherTheme(themeMode = mode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
    override fun onStart() { super.onStart(); try { widgetRepository.startListening() } catch (_: Exception) {} }
    override fun onStop() { try { widgetRepository.stopListening() } catch (_: Exception) {}; super.onStop() }
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {}
}
