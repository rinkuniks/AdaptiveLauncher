package com.adaptive.launcher.domain.themes

import android.content.Context
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
    val mode: Flow<ThemeMode> = ctx.themeStore.data.map{ prefs -> runCatching{ ThemeMode.valueOf(prefs[modeKey] ?: "System")}.getOrDefault(ThemeMode.System) }
    val accent: Flow<String?> = ctx.themeStore.data.map{ it[accentKey] }
    suspend fun setMode(m: ThemeMode){ ctx.themeStore.edit{ it[modeKey]=m.name } }
    suspend fun setAccent(hex: String?){ ctx.themeStore.edit{ if(hex==null) it.remove(accentKey) else it[accentKey]=hex } }
}
