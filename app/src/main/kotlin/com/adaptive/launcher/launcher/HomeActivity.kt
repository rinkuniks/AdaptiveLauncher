package com.adaptive.launcher.launcher

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowManager
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
import androidx.lifecycle.lifecycleScope
import com.adaptive.launcher.core.device.DeviceProfileFactory
import com.adaptive.launcher.core.performance.ColdStartTracer
import com.adaptive.launcher.core.ui.receivers.ScreenOffReceiver
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import com.adaptive.launcher.domain.themes.ThemeRepository
import com.adaptive.launcher.navigation.AppNavHost
import com.adaptive.launcher.ui.theme.AdaptiveLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    @Inject lateinit var themeRepository: ThemeRepository
    @Inject lateinit var widgetRepository: WidgetRepository
    @Inject lateinit var screenTimeRepository: ScreenTimeRepository
    private lateinit var screenOffReceiver: ScreenOffReceiver
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
        screenOffReceiver = ScreenOffReceiver {
            if (screenTimeRepository.hasActiveSession()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val pkg = screenTimeRepository.getActiveSessionPackageName() ?: return@launch
                    if (pkg.isNotEmpty()) screenTimeRepository.onAppClosed(pkg)
                }
            }
        }
        try { registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF)) } catch (_: Exception) {}
        // Wallpaper flag — keep window in sync with theme pref
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            themeRepository.showWallpaper.collect { show ->
                try {
                    if (show) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                } catch(_:Exception){}
            }
        } }
        setContent {
            val mode by themeRepository.mode.collectAsState(initial = com.adaptive.launcher.domain.themes.ThemeMode.System)
            val wallpaper by themeRepository.showWallpaper.collectAsState(initial = false)
            val font by themeRepository.font.collectAsState(initial = "System")
            val dynamic by themeRepository.dynamic.collectAsState(initial = false)
            AdaptiveLauncherTheme(themeMode = mode, showWallpaper = wallpaper, fontName = font, dynamic = dynamic) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
    override fun onStart() { super.onStart(); try { widgetRepository.startListening() } catch (_: Exception) {} }
    override fun onResume() {
        super.onResume()
        if (screenTimeRepository.hasActiveSession()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val pkg = screenTimeRepository.getActiveSessionPackageName() ?: return@launch
                if (pkg.isNotEmpty()) screenTimeRepository.onAppClosed(pkg)
            }
        }
    }
    override fun onStop() { try { widgetRepository.stopListening() } catch (_: Exception) {}; super.onStop() }
    override fun onDestroy() {
        super.onDestroy()
        try { if (::screenOffReceiver.isInitialized) unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
    }
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {}
}
