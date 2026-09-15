package com.adaptive.launcher.feature.settings

import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptive.launcher.core.device.DeviceProfileFactory
import com.adaptive.launcher.data.apps.AppMetaRepository
import com.adaptive.launcher.data.apps.AppRepository
import com.adaptive.launcher.data.backup.BackupRepository
import com.adaptive.launcher.data.home.HomeAlignment
import com.adaptive.launcher.data.home.HomePrefsRepository
import com.adaptive.launcher.data.home.HomeVAlignment
import com.adaptive.launcher.data.notifications.NotificationRepository
import com.adaptive.launcher.data.streams.StreamEntity
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.domain.gestures.GestureRepository
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.profiles.ProfileManager
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import com.adaptive.launcher.domain.streams.StreamRepository
import com.adaptive.launcher.domain.themes.ThemeMode
import com.adaptive.launcher.domain.themes.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUi(
    val deviceLabel: String="",
    val refreshHz: Int=60,
    val swDp: Int=0,
    val layoutMode: String="",
    val profilesLabel: String="",
    val themeMode: ThemeMode=ThemeMode.System,
    val gestureMap: Map<LauncherGesture, LauncherAction> = emptyMap(),
    val streams: List<StreamEntity> = emptyList(),
    val notifications: Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>> = emptyMap(),
    val widgetInfo: String="",
    val widgetIds: List<Int> = emptyList(),
    val backupJson: String? = null,
    val backupStatus: String? = null,
    val hasNotificationAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    // HomePrefs
    val showClock: Boolean = true,
    val bigClock: Boolean = false,
    val twelveHour: Boolean = false,
    val showDate: Boolean = true,
    val showStatusBar: Boolean = true,
    val showScreenTimeHome: Boolean = true,
    val showScreenTimeApp: Boolean = true,
    val showWeather: Boolean = true,
    val hidePrivateSpace: Boolean = false,
    val hideScreenTimePage: Boolean = false,
    val showHiddenInSearch: Boolean = false,
    val hapticFeedback: Boolean = true,
    val doubleTapToLock: Boolean = true,
    val showSearchBox: Boolean = true,
    val searchAutoOpen: Boolean = false,
    val bottomSearch: Boolean = false,
    val autoOpenApps: Boolean = false,
    val homeAlignment: HomeAlignment = HomeAlignment.Left,
    val homeVAlignment: HomeVAlignment = HomeVAlignment.Center,
    val appsAlignment: HomeAlignment = HomeAlignment.Left,
    val firstTimeHelp: Boolean = true,
    // AppMeta
    val hiddenIds: List<String> = emptyList(),
    val challengeIds: List<String> = emptyList(),
    val allAppsForMeta: List<com.adaptive.launcher.core.model.LauncherApp> = emptyList(),
    val showWallpaper: Boolean = false,
    val fontName: String = "System",
    val dynamicColor: Boolean = false,
    val enablePager: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
    private val gestureRepository: GestureRepository,
    private val streamRepository: StreamRepository,
    private val notificationRepository: NotificationRepository,
    private val widgetRepository: WidgetRepository,
    private val profileManager: ProfileManager,
    private val backupRepository: BackupRepository,
    private val homePrefsRepository: HomePrefsRepository,
    private val appMetaRepository: AppMetaRepository,
    private val appRepository: AppRepository,
    private val screenTimeRepository: ScreenTimeRepository,
): ViewModel(){
    private val _backupJson = MutableStateFlow<String?>(null)
    private val _backupStatus = MutableStateFlow<String?>(null)
    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(q: String){ _query.value = q }

    val ui: StateFlow<SettingsUi> = combine(
        themeRepository.mode,
        streamRepository.streams,
        notificationRepository.notifications,
        _backupJson,
        _backupStatus,
        combine(LauncherGesture.all.map{ g -> gestureRepository.observe(g).map{ a -> g to a } }){ it.toMap() },
        widgetRepository.widgetIds,
        homePrefsRepository.showClock,
        homePrefsRepository.bigClock,
        homePrefsRepository.twelveHour,
        homePrefsRepository.showDate,
        homePrefsRepository.showStatusBar,
        homePrefsRepository.showScreenTimeHome,
        homePrefsRepository.showScreenTimeApp,
        homePrefsRepository.showWeather,
        homePrefsRepository.hidePrivateSpace,
        homePrefsRepository.hideScreenTimePage,
        homePrefsRepository.showHiddenInSearch,
        homePrefsRepository.hapticFeedback,
        homePrefsRepository.doubleTapToLock,
        homePrefsRepository.showSearchBox,
        homePrefsRepository.searchAutoOpen,
        homePrefsRepository.bottomSearch,
        homePrefsRepository.autoOpenApps,
        homePrefsRepository.homeAlignment,
        homePrefsRepository.homeVAlignment,
        homePrefsRepository.appsAlignment,
        homePrefsRepository.firstTimeHelp,
        appMetaRepository.getHiddenPackageIdsFlow(),
        appMetaRepository.getChallengePackageIdsFlow(),
        appRepository.apps,
        themeRepository.showWallpaper,
        themeRepository.font,
        themeRepository.dynamic,
        homePrefsRepository.enablePager,
    ){ args ->
        val mode = args[0] as ThemeMode
        val streams = args[1] as List<StreamEntity>
        val notifs = args[2] as Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>>
        val bj = args[3] as String?
        val bs = args[4] as String?
        val gmap = args[5] as Map<LauncherGesture, LauncherAction>
        val wids = args[6] as List<Int>
        val showClock = args[7] as Boolean
        val bigClock = args[8] as Boolean
        val twelveHour = args[9] as Boolean
        val showDate = args[10] as Boolean
        val showStatusBar = args[11] as Boolean
        val showScreenTimeHome = args[12] as Boolean
        val showScreenTimeApp = args[13] as Boolean
        val showWeather = args[14] as Boolean
        val hidePrivateSpace = args[15] as Boolean
        val hideScreenTimePage = args[16] as Boolean
        val showHiddenInSearch = args[17] as Boolean
        val hapticFeedback = args[18] as Boolean
        val doubleTapToLock = args[19] as Boolean
        val showSearchBox = args[20] as Boolean
        val searchAutoOpen = args[21] as Boolean
        val bottomSearch = args[22] as Boolean
        val autoOpenApps = args[23] as Boolean
        val homeAlignment = args[24] as HomeAlignment
        val homeVAlignment = args[25] as HomeVAlignment
        val appsAlignment = args[26] as HomeAlignment
        val firstTimeHelp = args[27] as Boolean
        val hiddenIds = args[28] as List<String>
        val challengeIds = args[29] as List<String>
        @Suppress("UNCHECKED_CAST")
        val allApps = args[30] as List<com.adaptive.launcher.core.model.LauncherApp>
        val showWallpaper = args[31] as Boolean
        val fontName = args[32] as String
        val dynamicColor = args[33] as Boolean
        val enablePager = args[34] as Boolean
        val profile = DeviceProfileFactory.fromContext(context)
        val profilesLabel = profileManager.getProfiles().joinToString{ (if(it.isPrivate) "Private" else if(it.isWork) "Work" else "Personal") + "#" + it.serial }
        val enabledNotif = try {
            val s = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            s.contains(context.packageName)
        } catch (_: Exception) { false }
        val enabledA11y = try {
            val s = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services") ?: ""
            s.contains(context.packageName)
        } catch (_: Exception) { false }
        SettingsUi(
            deviceLabel = profile.label,
            refreshHz = profile.refreshRate.toInt(),
            swDp = profile.swDp,
            layoutMode = if(profile.isTablet) "Tablet" else if(profile.isTall) "TallPhone" else "Compact",
            profilesLabel = profilesLabel.ifEmpty{ "single profile"},
            themeMode = mode,
            gestureMap = gmap,
            streams = streams,
            notifications = notifs,
            widgetInfo = "host ready; ${try{ AppWidgetManager.getInstance(context).installedProviders.size }catch(_:Exception){0}} providers; ${wids.size} pinned",
            widgetIds = wids,
            backupJson = bj,
            backupStatus = bs,
            hasNotificationAccess = enabledNotif,
            hasAccessibilityAccess = enabledA11y,
            showClock = showClock,
            bigClock = bigClock,
            twelveHour = twelveHour,
            showDate = showDate,
            showStatusBar = showStatusBar,
            showScreenTimeHome = showScreenTimeHome,
            showScreenTimeApp = showScreenTimeApp,
            showWeather = showWeather,
            hidePrivateSpace = hidePrivateSpace,
            hideScreenTimePage = hideScreenTimePage,
            showHiddenInSearch = showHiddenInSearch,
            hapticFeedback = hapticFeedback,
            doubleTapToLock = doubleTapToLock,
            showSearchBox = showSearchBox,
            searchAutoOpen = searchAutoOpen,
            bottomSearch = bottomSearch,
            autoOpenApps = autoOpenApps,
            homeAlignment = homeAlignment,
            homeVAlignment = homeVAlignment,
            appsAlignment = appsAlignment,
            firstTimeHelp = firstTimeHelp,
            hiddenIds = hiddenIds,
            challengeIds = challengeIds,
            allAppsForMeta = allApps,
            showWallpaper = showWallpaper,
            fontName = fontName,
            dynamicColor = dynamicColor,
            enablePager = enablePager,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUi())

    fun setTheme(m: ThemeMode){ viewModelScope.launch{ themeRepository.setMode(m)} }
    fun cycleGesture(g: LauncherGesture){
        viewModelScope.launch{
            val cur = gestureRepository.observe(g).first()
            val next = when(cur){
                LauncherAction.None -> LauncherAction.OpenSearch
                LauncherAction.OpenSearch -> LauncherAction.OpenAppList
                LauncherAction.OpenAppList -> LauncherAction.OpenSettings
                LauncherAction.OpenSettings -> LauncherAction.OpenNotifications
                LauncherAction.OpenNotifications -> LauncherAction.None
                is LauncherAction.LaunchApp -> LauncherAction.None
            }
            gestureRepository.set(g, next)
        }
    }
    fun setGestureDirect(g: LauncherGesture, a: LauncherAction){ viewModelScope.launch{ gestureRepository.set(g, a) } }
    fun createStream(name: String){ viewModelScope.launch{ streamRepository.create(name)} }
    fun deleteStream(id: String){ viewModelScope.launch{ streamRepository.delete(id)} }
    fun moveStream(id: String, dir: Int){
        viewModelScope.launch{
            val list = streamRepository.streams.first().toMutableList()
            val idx = list.indexOfFirst{ it.id==id }
            if(idx==-1) return@launch
            val to = (idx+dir).coerceIn(0, list.size-1)
            if(to==idx) return@launch
            val item = list.removeAt(idx)
            list.add(to, item)
            streamRepository.reorder(list.map{ it.id })
        }
    }
    fun onWidgetPicked(id: Int){ viewModelScope.launch{ if(id!=-1) widgetRepository.addWidgetId(id) } }
    fun removeWidget(id: Int){ viewModelScope.launch{ widgetRepository.removeWidgetId(id) } }
    fun requestWidgetPicker(ctx: Context){
        try{
            val id = widgetRepository.allocateId()
            if(id==-1) return
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply{ putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }catch(_:Exception){}
    }
    // HomePrefs passthrough
    fun setShowClock(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowClock(v) } }
    fun setBigClock(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setBigClock(v) } }
    fun setTwelveHour(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setTwelveHour(v) } }
    fun setShowDate(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowDate(v) } }
    fun setShowStatusBar(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowStatusBar(v) } }
    fun setShowScreenTimeHome(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowScreenTimeHome(v) } }
    fun setShowScreenTimeApp(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowScreenTimeApp(v) } }
    fun setShowWeather(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowWeather(v) } }
    fun setHidePrivateSpace(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setHidePrivateSpace(v) } }
    fun setHideScreenTimePage(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setHideScreenTimePage(v) } }
    fun setShowHiddenInSearch(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowHiddenInSearch(v) } }
    fun setHapticFeedback(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setHapticFeedback(v) } }
    fun setDoubleTapToLock(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setDoubleTapToLock(v) } }
    fun setShowSearchBox(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setShowSearchBox(v) } }
    fun setSearchAutoOpen(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setSearchAutoOpen(v) } }
    fun setBottomSearch(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setBottomSearch(v) } }
    fun setAutoOpenApps(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setAutoOpenApps(v) } }
    fun setHomeAlignment(v: HomeAlignment){ viewModelScope.launch{ homePrefsRepository.setHomeAlignment(v) } }
    fun setHomeVAlignment(v: HomeVAlignment){ viewModelScope.launch{ homePrefsRepository.setHomeVAlignment(v) } }
    fun setAppsAlignment(v: HomeAlignment){ viewModelScope.launch{ homePrefsRepository.setAppsAlignment(v) } }
    fun setFirstTimeHelp(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setFirstTimeHelp(v) } }
    fun setEnablePager(v: Boolean){ viewModelScope.launch{ homePrefsRepository.setEnablePager(v) } }
    fun setShowWallpaper(v: Boolean){ viewModelScope.launch{ themeRepository.setShowWallpaper(v) } }
    fun setFont(v: String){ viewModelScope.launch{ themeRepository.setFont(v) } }
    fun setDynamicColor(v: Boolean){ viewModelScope.launch{ themeRepository.setDynamic(v) } }

    // AppMeta
    fun toggleHidden(pkg: String){ viewModelScope.launch{ val cur = appMetaRepository.isHidden(pkg); appMetaRepository.setHidden(pkg, !cur) } }
    fun toggleChallenge(pkg: String){ viewModelScope.launch{ val cur = appMetaRepository.isChallenge(pkg); appMetaRepository.setChallenge(pkg, !cur) } }
    fun clearHidden(){ viewModelScope.launch{ ui.value.hiddenIds.forEach{ appMetaRepository.setHidden(it, false) } } }
    fun clearChallenges(){ viewModelScope.launch{ ui.value.challengeIds.forEach{ appMetaRepository.setChallenge(it, false) } } }

    fun setBackupJson(json: String){ _backupJson.value = json; _backupStatus.value = null }
    suspend fun exportBackup(){ _backupJson.value = backupRepository.export(); _backupStatus.value = "Exported schemaVersion 1 (${_backupJson.value?.length ?: 0} chars)" }
    suspend fun importBackup(){ val json = _backupJson.value ?: return; val ok = backupRepository.importBackup(json); _backupStatus.value = if(ok) "Imported OK" else "Import failed - invalid JSON" }
    suspend fun refresh(){
        _backupStatus.value = "Refreshed"
    }
}
