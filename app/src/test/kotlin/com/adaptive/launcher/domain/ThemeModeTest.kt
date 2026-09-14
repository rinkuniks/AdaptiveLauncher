package com.adaptive.launcher.domain

import com.adaptive.launcher.domain.themes.ThemeMode
import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {
    @Test fun valueOf_allModes(){ assertEquals(ThemeMode.Dark, ThemeMode.valueOf("Dark")); assertEquals(ThemeMode.Amoled, ThemeMode.valueOf("Amoled")) }
    @Test fun invalid_defaultsToSystem(){ val mode = runCatching{ ThemeMode.valueOf("Invalid")}.getOrDefault(ThemeMode.System); assertEquals(ThemeMode.System, mode) }
    @Test fun light_notDark(){ assertNotEquals(ThemeMode.Light, ThemeMode.Dark) }
}
