package com.adaptive.launcher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "adaptive_prefs")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val hiddenAppsKey = stringSetPreferencesKey("hidden_apps")
    private val favoritesOrderKey = stringPreferencesKey("favorites_order") // not used; Room is source of truth
    private val isFirstLaunchKey = booleanPreferencesKey("is_first_launch")

    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { it[hiddenAppsKey] ?: emptySet() }
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[isFirstLaunchKey] ?: true }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val cur = prefs[hiddenAppsKey] ?: emptySet()
            prefs[hiddenAppsKey] = if (hidden) cur + packageName else cur - packageName
        }
    }

    suspend fun markLaunched() {
        context.dataStore.edit { it[isFirstLaunchKey] = false }
    }
}
