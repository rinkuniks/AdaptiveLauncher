package com.adaptive.launcher.domain.themes

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode{ System, Light, Dark, Amoled }
private val Context.themeStore by preferencesDataStore(name="theme")

@Singleton
class ThemeRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    private val modeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent")
    private val wallpaperKey = booleanPreferencesKey("show_wallpaper")
    private val fontKey = stringPreferencesKey("font")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    val mode: Flow<ThemeMode> = ctx.themeStore.data.map{ prefs -> runCatching{ ThemeMode.valueOf(prefs[modeKey] ?: "System")}.getOrDefault(ThemeMode.System) }
    val accent: Flow<String?> = ctx.themeStore.data.map{ it[accentKey] }
    val showWallpaper: Flow<Boolean> = ctx.themeStore.data.map{ it[wallpaperKey] ?: false }
    val font: Flow<String> = ctx.themeStore.data.map{ it[fontKey] ?: "System" }
    val dynamic: Flow<Boolean> = ctx.themeStore.data.map{ it[dynamicKey] ?: false }
    suspend fun setMode(m: ThemeMode){ ctx.themeStore.edit{ it[modeKey]=m.name } }
    suspend fun setAccent(hex: String?){ ctx.themeStore.edit{ if(hex==null) it.remove(accentKey) else it[accentKey]=hex } }
    suspend fun setShowWallpaper(v: Boolean){ ctx.themeStore.edit{ it[wallpaperKey]=v } }
    suspend fun setFont(name: String){ ctx.themeStore.edit{ it[fontKey]=name } }
    suspend fun setDynamic(v: Boolean){ ctx.themeStore.edit{ it[dynamicKey]=v } }
    companion object {
        val fonts = listOf("System","Outfit","Inter","Roboto","Mono")
    }
}
