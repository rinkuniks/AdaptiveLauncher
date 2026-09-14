package com.adaptive.launcher.core.device

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class LayoutMode { CompactPhone, TallPhone, Tablet, FoldableInner, LandscapeWide }

data class LayoutConfig(
    val mode: LayoutMode,
    val swDp: Int,
    val shDp: Int,
    val wDp: Int,
    val hDp: Int,
    val isLandscape: Boolean,
    val isWide: Boolean,
    val isTall: Boolean,
    val horizontalPad: androidx.compose.ui.unit.Dp,
    val railWidth: androidx.compose.ui.unit.Dp,
    val columns: Int,
)

@Composable
fun rememberLayoutConfig(): LayoutConfig {
    val c = LocalConfiguration.current
    val sw = c.screenWidthDp
    val sh = c.screenHeightDp
    val w = c.screenWidthDp
    val h = c.screenHeightDp
    val isLand = c.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Foldable inner ~ 800dp+ sw, tablet 600dp+, tall phone aspect >2.0
    val aspect = if (sw > 0) sh.toFloat() / sw else 2.2f
    val isTall = aspect > 2.0f
    val isWide = sw >= 600 || (isLand && w >= 600)
    val mode = when {
        sw >= 800 -> LayoutMode.FoldableInner
        sw >= 600 -> LayoutMode.Tablet
        isLand && w >= 600 -> LayoutMode.LandscapeWide
        isTall -> LayoutMode.TallPhone
        else -> LayoutMode.CompactPhone
    }
    val pad = when {
        sw < 360 -> 14.dp
        sw < 400 -> 16.dp
        sw < 600 -> 20.dp
        sw < 800 -> 24.dp
        else -> 28.dp
    }
    val rail = when (mode) {
        LayoutMode.Tablet, LayoutMode.FoldableInner, LayoutMode.LandscapeWide -> 44.dp
        LayoutMode.TallPhone -> 34.dp
        else -> 32.dp
    }
    val cols = when (mode) {
        LayoutMode.Tablet -> 2
        LayoutMode.FoldableInner -> 3
        LayoutMode.LandscapeWide -> 2
        else -> 1
    }
    return remember(sw, sh, isLand) { LayoutConfig(mode, sw, sh, w, h, isLand, isWide, isTall, pad, rail, cols) }
}
