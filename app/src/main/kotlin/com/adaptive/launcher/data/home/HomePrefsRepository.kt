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
enum class HomeAlignment { Left, Center, Right }
enum class HomeVAlignment { Top, Center, Bottom }

@Singleton
class HomePrefsRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    private val filterKey = stringPreferencesKey("app_filter")
    private val homeModeKey = stringPreferencesKey("home_mode")
    private val homePackagesKey = stringSetPreferencesKey("home_packages")
    private val showSystemBadgeKey = booleanPreferencesKey("show_system_badge")

    // Escape parity — appearence / home personalization
    private val showClockKey = booleanPreferencesKey("show_clock")
    private val bigClockKey = booleanPreferencesKey("big_clock")
    private val twelveHourKey = booleanPreferencesKey("twelve_hour_clock")
    private val showDateKey = booleanPreferencesKey("show_date")
    private val showStatusBarKey = booleanPreferencesKey("show_status_bar")
    private val showScreenTimeHomeKey = booleanPreferencesKey("show_screen_time_home")
    private val showScreenTimeAppKey = booleanPreferencesKey("show_screen_time_app")
    private val showWeatherKey = booleanPreferencesKey("show_weather")
    private val hidePrivateSpaceKey = booleanPreferencesKey("hide_private_space")
    private val hideScreenTimePageKey = booleanPreferencesKey("hide_screen_time_page")
    private val showHiddenInSearchKey = booleanPreferencesKey("show_hidden_in_search")
    private val hapticFeedbackKey = booleanPreferencesKey("haptic_feedback")
    private val doubleTapToLockKey = booleanPreferencesKey("double_tap_to_lock")
    private val showSearchBoxKey = booleanPreferencesKey("show_search_box")
    private val searchAutoOpenKey = booleanPreferencesKey("search_auto_open")
    private val bottomSearchKey = booleanPreferencesKey("bottom_search")
    private val autoOpenAppsKey = booleanPreferencesKey("auto_open_apps")
    private val homeAlignmentKey = stringPreferencesKey("home_alignment")
    private val homeVAlignmentKey = stringPreferencesKey("home_v_alignment")
    private val appsAlignmentKey = stringPreferencesKey("apps_alignment")
    private val firstTimeHelpKey = booleanPreferencesKey("first_time_help")
    private val enablePagerKey = booleanPreferencesKey("enable_pager")

    val filter: Flow<AppListFilter> = ctx.homePrefs.data.map { prefs ->
        runCatching{ AppListFilter.valueOf(prefs[filterKey] ?: "Installed")}.getOrDefault(AppListFilter.Installed)
    }
    val homeMode: Flow<HomeMode> = ctx.homePrefs.data.map { prefs ->
        runCatching{ HomeMode.valueOf(prefs[homeModeKey] ?: "AutoMajor")}.getOrDefault(HomeMode.AutoMajor)
    }
    val homePackages: Flow<Set<String>> = ctx.homePrefs.data.map { it[homePackagesKey] ?: emptySet() }
    val showSystemBadge: Flow<Boolean> = ctx.homePrefs.data.map { it[showSystemBadgeKey] ?: true }

    val showClock: Flow<Boolean> = ctx.homePrefs.data.map { it[showClockKey] ?: true }
    val bigClock: Flow<Boolean> = ctx.homePrefs.data.map { it[bigClockKey] ?: false }
    val twelveHour: Flow<Boolean> = ctx.homePrefs.data.map { it[twelveHourKey] ?: false }
    val showDate: Flow<Boolean> = ctx.homePrefs.data.map { it[showDateKey] ?: true }
    val showStatusBar: Flow<Boolean> = ctx.homePrefs.data.map { it[showStatusBarKey] ?: true }
    val showScreenTimeHome: Flow<Boolean> = ctx.homePrefs.data.map { it[showScreenTimeHomeKey] ?: true }
    val showScreenTimeApp: Flow<Boolean> = ctx.homePrefs.data.map { it[showScreenTimeAppKey] ?: true }
    val showWeather: Flow<Boolean> = ctx.homePrefs.data.map { it[showWeatherKey] ?: true }
    val hidePrivateSpace: Flow<Boolean> = ctx.homePrefs.data.map { it[hidePrivateSpaceKey] ?: false }
    val hideScreenTimePage: Flow<Boolean> = ctx.homePrefs.data.map { it[hideScreenTimePageKey] ?: false }
    val showHiddenInSearch: Flow<Boolean> = ctx.homePrefs.data.map { it[showHiddenInSearchKey] ?: false }
    val hapticFeedback: Flow<Boolean> = ctx.homePrefs.data.map { it[hapticFeedbackKey] ?: true }
    val doubleTapToLock: Flow<Boolean> = ctx.homePrefs.data.map { it[doubleTapToLockKey] ?: true }
    val showSearchBox: Flow<Boolean> = ctx.homePrefs.data.map { it[showSearchBoxKey] ?: true }
    val searchAutoOpen: Flow<Boolean> = ctx.homePrefs.data.map { it[searchAutoOpenKey] ?: false }
    val bottomSearch: Flow<Boolean> = ctx.homePrefs.data.map { it[bottomSearchKey] ?: false }
    val autoOpenApps: Flow<Boolean> = ctx.homePrefs.data.map { it[autoOpenAppsKey] ?: false }
    val homeAlignment: Flow<HomeAlignment> = ctx.homePrefs.data.map { prefs ->
        runCatching{ HomeAlignment.valueOf(prefs[homeAlignmentKey] ?: "Left")}.getOrDefault(HomeAlignment.Left)
    }
    val homeVAlignment: Flow<HomeVAlignment> = ctx.homePrefs.data.map { prefs ->
        runCatching{ HomeVAlignment.valueOf(prefs[homeVAlignmentKey] ?: "Center")}.getOrDefault(HomeVAlignment.Center)
    }
    val appsAlignment: Flow<HomeAlignment> = ctx.homePrefs.data.map { prefs ->
        runCatching{ HomeAlignment.valueOf(prefs[appsAlignmentKey] ?: "Left")}.getOrDefault(HomeAlignment.Left)
    }
    val firstTimeHelp: Flow<Boolean> = ctx.homePrefs.data.map { it[firstTimeHelpKey] ?: true }
    val enablePager: Flow<Boolean> = ctx.homePrefs.data.map { it[enablePagerKey] ?: false }

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

    suspend fun setShowClock(v: Boolean){ ctx.homePrefs.edit{ it[showClockKey]=v } }
    suspend fun setBigClock(v: Boolean){ ctx.homePrefs.edit{ it[bigClockKey]=v } }
    suspend fun setTwelveHour(v: Boolean){ ctx.homePrefs.edit{ it[twelveHourKey]=v } }
    suspend fun setShowDate(v: Boolean){ ctx.homePrefs.edit{ it[showDateKey]=v } }
    suspend fun setShowStatusBar(v: Boolean){ ctx.homePrefs.edit{ it[showStatusBarKey]=v } }
    suspend fun setShowScreenTimeHome(v: Boolean){ ctx.homePrefs.edit{ it[showScreenTimeHomeKey]=v } }
    suspend fun setShowScreenTimeApp(v: Boolean){ ctx.homePrefs.edit{ it[showScreenTimeAppKey]=v } }
    suspend fun setShowWeather(v: Boolean){ ctx.homePrefs.edit{ it[showWeatherKey]=v } }
    suspend fun setHidePrivateSpace(v: Boolean){ ctx.homePrefs.edit{ it[hidePrivateSpaceKey]=v } }
    suspend fun setHideScreenTimePage(v: Boolean){ ctx.homePrefs.edit{ it[hideScreenTimePageKey]=v } }
    suspend fun setShowHiddenInSearch(v: Boolean){ ctx.homePrefs.edit{ it[showHiddenInSearchKey]=v } }
    suspend fun setHapticFeedback(v: Boolean){ ctx.homePrefs.edit{ it[hapticFeedbackKey]=v } }
    suspend fun setDoubleTapToLock(v: Boolean){ ctx.homePrefs.edit{ it[doubleTapToLockKey]=v } }
    suspend fun setShowSearchBox(v: Boolean){ ctx.homePrefs.edit{ it[showSearchBoxKey]=v } }
    suspend fun setSearchAutoOpen(v: Boolean){ ctx.homePrefs.edit{ it[searchAutoOpenKey]=v } }
    suspend fun setBottomSearch(v: Boolean){ ctx.homePrefs.edit{ it[bottomSearchKey]=v } }
    suspend fun setAutoOpenApps(v: Boolean){ ctx.homePrefs.edit{ it[autoOpenAppsKey]=v } }
    suspend fun setHomeAlignment(v: HomeAlignment){ ctx.homePrefs.edit{ it[homeAlignmentKey]=v.name } }
    suspend fun setHomeVAlignment(v: HomeVAlignment){ ctx.homePrefs.edit{ it[homeVAlignmentKey]=v.name } }
    suspend fun setAppsAlignment(v: HomeAlignment){ ctx.homePrefs.edit{ it[appsAlignmentKey]=v.name } }
    suspend fun setFirstTimeHelp(v: Boolean){ ctx.homePrefs.edit{ it[firstTimeHelpKey]=v } }
    suspend fun setEnablePager(v: Boolean){ ctx.homePrefs.edit{ it[enablePagerKey]=v } }

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
