package com.adaptive.launcher

import android.os.Build
import android.view.WindowManager
import android.os.Bundle
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
import com.adaptive.launcher.domain.themes.ThemeRepository
import com.adaptive.launcher.navigation.AppNavHost
import com.adaptive.launcher.ui.theme.AdaptiveLauncherTheme
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themeRepository: ThemeRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
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
}
