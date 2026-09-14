package com.adaptive.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.adaptive.launcher.domain.themes.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF3A5CCC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EAF2),
    outlineVariant = Color(0xFFD6D9E6)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8BA0FF),
    onPrimary = Color(0xFF0F1A4A),
    primaryContainer = Color(0xFF2A3A8A),
    background = Color(0xFF0F1115),
    surface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFF252A36),
    outlineVariant = Color(0xFF3A3F52)
)
private val Amoled = darkColorScheme(
    primary = Color(0xFF8BA0FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2A3A),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    outlineVariant = Color(0xFF2A2A2A)
)

@Composable
fun AdaptiveLauncherTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamic: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when(themeMode){
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Amoled -> true
        ThemeMode.System -> systemDark
    }
    val isAmoled = themeMode == ThemeMode.Amoled
    val ctx = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        isAmoled -> Amoled
        darkTheme -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
