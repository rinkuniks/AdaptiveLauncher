package com.adaptive.launcher.data.home

import android.content.Context
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

private val Context.homePrefs by preferencesDataStore(name="home_prefs")

enum class AppListFilter { All, Installed, System }
enum class HomeMode { AutoMajor, Custom, ShowAll }

@Singleton
class HomePrefsRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    private val filterKey = stringPreferencesKey("app_filter")
    private val homeModeKey = stringPreferencesKey("home_mode")
    private val homePackagesKey = stringSetPreferencesKey("home_packages")
    private val showSystemBadgeKey = booleanPreferencesKey("show_system_badge")

    val filter: Flow<AppListFilter> = ctx.homePrefs.data.map { prefs ->
        runCatching{ AppListFilter.valueOf(prefs[filterKey] ?: "Installed")}.getOrDefault(AppListFilter.Installed)
    }
    val homeMode: Flow<HomeMode> = ctx.homePrefs.data.map { prefs ->
        runCatching{ HomeMode.valueOf(prefs[homeModeKey] ?: "AutoMajor")}.getOrDefault(HomeMode.AutoMajor)
    }
    val homePackages: Flow<Set<String>> = ctx.homePrefs.data.map { it[homePackagesKey] ?: emptySet() }
    val showSystemBadge: Flow<Boolean> = ctx.homePrefs.data.map { it[showSystemBadgeKey] ?: true }

    suspend fun setFilter(f: AppListFilter){ ctx.homePrefs.edit{ it[filterKey]=f.name } }
    suspend fun setHomeMode(m: HomeMode){ ctx.homePrefs.edit{ it[homeModeKey]=m.name } }
    suspend fun setHomePackages(pkgs: Set<String>){ ctx.homePrefs.edit{ it[homePackagesKey]=pkgs } }
    suspend fun toggleHomePackage(pkg: String){
        ctx.homePrefs.edit{ prefs ->
            val cur = prefs[homePackagesKey] ?: emptySet()
            prefs[homePackagesKey] = if(pkg in cur) cur - pkg else cur + pkg
        }
    }
    suspend fun setShowSystemBadge(v: Boolean){ ctx.homePrefs.edit{ it[showSystemBadgeKey]=v } }

    companion object {
        val MajorPackages = setOf(
            "com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer",
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
            "com.android.camera2", "com.google.android.GoogleCamera", "com.sec.android.app.camera",
            "com.google.android.chrome", "com.android.chrome", "com.sec.android.app.sbrowser",
            "com.google.android.gm", "com.google.android.apps.maps", "com.google.android.apps.photos",
            "com.whatsapp", "com.instagram.android", "com.spotify.music", "com.netflix.mediaclient"
        )
    }
}
