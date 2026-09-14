package com.adaptive.launcher.core.device

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Immutable
data class DeviceProfile(
    val model: String = Build.MODEL,              // A001
    val brand: String = Build.BRAND,              // Nothing
    val manufacturer: String = Build.MANUFACTURER, // Nothing
    val board: String = Build.BOARD,              // Galaga
    val sdkInt: Int = Build.VERSION.SDK_INT,       // 36
    val release: String = Build.VERSION.RELEASE,   // 16
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val fontScale: Float,
    val refreshRate: Float,
    val cutoutTopPx: Int,
    val navBarHeightPx: Int,
    val aspectRatio: Float = heightPx.toFloat() / widthPx.coerceAtLeast(1),
    val isTall: Boolean = heightPx.toFloat() / widthPx > 2.0f, // 1080x2392 = 2.21
    val isTablet: Boolean = min(widthPx, heightPx) / density >= 600,
    val swDp: Int = (min(widthPx, heightPx) / density).toInt(),
) {
    val label: String get() = "$manufacturer $model • Android $release (API $sdkInt) • ${widthPx}x${heightPx} @${densityDpi}dpi"
    val isNothingA001: Boolean get() = model == "A001" || board.equals("Galaga", ignoreCase = true)
}

object DeviceProfileFactory {
    fun fromContext(context: Context): DeviceProfile {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        try { display.getRealMetrics(metrics) } catch (_: Exception) { context.resources.displayMetrics.let { metrics.setTo(it) } }
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.density
        val densityDpi = metrics.densityDpi
        val fontScale = context.resources.configuration.fontScale
        val refreshRate = try { display.refreshRate } catch (_: Exception) { 60f }
        // cutout / nav bar estimated via resources; refined at composition via WindowInsets
        val cutoutTop = run {
            val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (id > 0) context.resources.getDimensionPixelSize(id) else 0
        }
        val navBar = run {
            val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (id > 0) context.resources.getDimensionPixelSize(id) else 0
        }
        return DeviceProfile(
            widthPx = width, heightPx = height,
            densityDpi = densityDpi, density = density,
            fontScale = fontScale, refreshRate = refreshRate,
            cutoutTopPx = cutoutTop, navBarHeightPx = navBar
        )
    }
}
