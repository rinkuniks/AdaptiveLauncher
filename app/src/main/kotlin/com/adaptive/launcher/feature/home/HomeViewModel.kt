package com.adaptive.launcher.feature.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptive.launcher.core.model.LauncherApp
import com.adaptive.launcher.data.apps.AppMetaRepository
import com.adaptive.launcher.data.apps.AppRepository
import com.adaptive.launcher.data.favorites.FavoritesRepository
import com.adaptive.launcher.data.home.AppListFilter
import com.adaptive.launcher.data.home.HomeAlignment
import com.adaptive.launcher.data.home.HomeMode
import com.adaptive.launcher.data.home.HomePrefsRepository
import com.adaptive.launcher.data.home.HomeVAlignment
import com.adaptive.launcher.data.notifications.NotificationRepository
import com.adaptive.launcher.data.shortcuts.ShortcutRepository
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.data.backup.BackupRepository
import com.adaptive.launcher.domain.apps.TryOpenAppUseCase
import com.adaptive.launcher.domain.apps.TryOpenAppResult
import com.adaptive.launcher.domain.context.ContextEngine
import com.adaptive.launcher.domain.context.UsageRepository
import com.adaptive.launcher.domain.gestures.GestureMapper
import com.adaptive.launcher.domain.gestures.GestureRepository
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.profiles.ProfileManager
import com.adaptive.launcher.domain.screentime.ScreenTimeRepository
import com.adaptive.launcher.domain.search.AppSearchProvider
import com.adaptive.launcher.domain.streams.StreamRepository
import com.adaptive.launcher.domain.themes.ThemeRepository
import com.adaptive.launcher.launcher.DefaultLauncherHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val allApps: List<LauncherApp> = emptyList(),
    val favorites: List<LauncherApp> = emptyList(),
    val filteredApps: List<LauncherApp> = emptyList(),
    val homeVisibleApps: List<LauncherApp> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<LauncherApp> = emptyList(),
    val isSearching: Boolean = false,
    val isDefaultLauncher: Boolean = false,
    val sections: List<Char> = emptyList(),
    val drawerSections: List<Char> = emptyList(),
    val selectedSection: Char? = null,
    val appFilter: AppListFilter = AppListFilter.Installed,
    val homeMode: HomeMode = HomeMode.AutoMajor,
    val homePackages: Set<String> = emptySet(),
    val contextSuggestions: List<com.adaptive.launcher.domain.context.ContextSuggestion> = emptyList(),
    val streams: List<com.adaptive.launcher.data.streams.StreamEntity> = emptyList(),
    val notifications: Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>> = emptyMap(),
    val widgetIds: List<Int> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val challengePackages: Set<String> = emptySet(),
    // Escape parity
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
    val enablePager: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val searchProvider: AppSearchProvider,
    private val defaultLauncherHelper: DefaultLauncherHelper,
    private val usageRepository: UsageRepository,
    private val contextEngine: ContextEngine,
    private val streamRepository: StreamRepository,
    private val themeRepository: ThemeRepository,
    private val notificationRepository: NotificationRepository,
    private val profileManager: ProfileManager,
    private val gestureRepository: GestureRepository,
    private val gestureMapper: GestureMapper,
    private val backupRepository: BackupRepository,
    private val shortcutRepository: ShortcutRepository,
    private val homePrefsRepository: HomePrefsRepository,
    private val widgetRepository: WidgetRepository,
    private val appMetaRepository: AppMetaRepository,
    private val screenTimeRepository: ScreenTimeRepository,
    private val tryOpenAppUseCase: TryOpenAppUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedSectionFlow = MutableStateFlow<Char?>(null)
    private val isSearchingFlow = MutableStateFlow(false)
    private val _challengeApp = MutableStateFlow<LauncherApp?>(null)
    val challengeApp: StateFlow<LauncherApp?> = _challengeApp.asStateFlow()

    val profiles = profileManager.getProfiles()

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.apps,
        favoritesRepository.favorites,
        searchQueryFlow,
        selectedSectionFlow,
        isSearchingFlow,
        usageRepository.signals(),
        streamRepository.streams,
        notificationRepository.notifications,
        homePrefsRepository.filter,
        homePrefsRepository.homeMode,
        homePrefsRepository.homePackages,
        widgetRepository.widgetIds,
        appMetaRepository.getHiddenPackageIdsFlow(),
        appMetaRepository.getChallengePackageIdsFlow(),
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
        homePrefsRepository.enablePager,
    ) { args ->
        val apps = args[0] as List<LauncherApp>
        val favEntities = args[1] as List<com.adaptive.launcher.data.favorites.FavoriteEntity>
        val query = args[2] as String
        val section = args[3] as Char?
        val isSearching = args[4] as Boolean
        val signals = args[5] as List<com.adaptive.launcher.domain.context.ContextSignal>
        val streams = args[6] as List<com.adaptive.launcher.data.streams.StreamEntity>
        val notifs = args[7] as Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>>
        val filter = args[8] as AppListFilter
        val homeMode = args[9] as HomeMode
        val homePkgs = args[10] as Set<String>
        val widgetIds = args[11] as List<Int>
        val hiddenList = args[12] as List<String>
        val challengeList = args[13] as List<String>
        val showClock = args[14] as Boolean
        val bigClock = args[15] as Boolean
        val twelveHour = args[16] as Boolean
        val showDate = args[17] as Boolean
        val showStatusBar = args[18] as Boolean
        val showScreenTimeHome = args[19] as Boolean
        val showScreenTimeApp = args[20] as Boolean
        val showWeather = args[21] as Boolean
        val hidePrivateSpace = args[22] as Boolean
        val hideScreenTimePage = args[23] as Boolean
        val showHiddenInSearch = args[24] as Boolean
        val hapticFeedback = args[25] as Boolean
        val doubleTapToLock = args[26] as Boolean
        val showSearchBox = args[27] as Boolean
        val searchAutoOpen = args[28] as Boolean
        val bottomSearch = args[29] as Boolean
        val autoOpenApps = args[30] as Boolean
        val homeAlignment = args[31] as HomeAlignment
        val homeVAlignment = args[32] as HomeVAlignment
        val appsAlignment = args[33] as HomeAlignment
        val firstTimeHelp = args[34] as Boolean
        val enablePager = args[35] as Boolean
        val hiddenSet = hiddenList.toSet()
        val challengeSet = challengeList.toSet()

        val favApps = favEntities.mapNotNull { fav -> apps.find { it.packageName == fav.packageName } }
        val favSet = favApps.map { it.packageName to it.user }.toSet()
        val allWithFavFlag = apps.map { it.copy(isFavorite = favSet.contains(it.packageName to it.user)) }

        // Private space filtering
        val afterPrivate = if (hidePrivateSpace) allWithFavFlag.filter { !profileManager.isPrivateApp(it.user) } else allWithFavFlag
        // Hidden filter for home/drawer (always hide hidden)
        val visibleAll = afterPrivate.filter { it.packageName !in hiddenSet }

        val filteredByType = when(filter){
            AppListFilter.All -> visibleAll
            AppListFilter.Installed -> visibleAll.filter { !it.isSystemApp }
            AppListFilter.System -> visibleAll.filter { it.isSystemApp }
        }

        val homeVisible = when {
            filter == AppListFilter.System -> filteredByType
            filter == AppListFilter.All && homeMode != HomeMode.Custom -> filteredByType
            else -> when(homeMode){
                HomeMode.ShowAll -> filteredByType
                HomeMode.AutoMajor -> filteredByType.filter { app ->
                    !app.isSystemApp || app.packageName in HomePrefsRepository.MajorPackages
                }
                HomeMode.Custom -> {
                    if(homePkgs.isEmpty()) filteredByType.filter { !it.isSystemApp }.take(12)
                    else filteredByType.filter { it.packageName in homePkgs }
                }
            }
        }

        val activeList = if(isSearching) emptyList() else homeVisible
        val filteredForSections = if(isSearching) emptyList()
            else if(section != null) activeList.filter { it.section == section }
            else activeList

        val sections = activeList.map { it.section }.distinct().sortedBy { if (it=='#') Char.MAX_VALUE else it }
        val drawerSections = filteredByType.map { it.section }.distinct().sortedBy { if (it=='#') Char.MAX_VALUE else it }

        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        val hourBucket = cal.get(java.util.Calendar.HOUR_OF_DAY)/3
        val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val suggestions = try { contextEngine.score(signals, now, hourBucket, day) } catch(_:Exception){ emptyList() }

        HomeUiState(
            allApps = allWithFavFlag,
            favorites = favApps,
            filteredApps = filteredForSections,
            homeVisibleApps = homeVisible,
            searchQuery = query,
            searchResults = emptyList(),
            isSearching = isSearching,
            isDefaultLauncher = defaultLauncherHelper.isDefaultLauncher(),
            sections = sections,
            drawerSections = drawerSections,
            selectedSection = section,
            appFilter = filter,
            homeMode = homeMode,
            homePackages = homePkgs,
            contextSuggestions = suggestions,
            streams = streams,
            notifications = notifs,
            widgetIds = widgetIds,
            hiddenPackages = hiddenSet,
            challengePackages = challengeSet,
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
            enablePager = enablePager,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val drawerApps: StateFlow<List<LauncherApp>> = combine(appRepository.apps, homePrefsRepository.filter, appMetaRepository.getHiddenPackageIdsFlow(), homePrefsRepository.hidePrivateSpace){ apps, filter, hidden, hidePrivate ->
        val hiddenSet = hidden.toSet()
        var visible = apps.filter { it.packageName !in hiddenSet }
        if (hidePrivate) visible = visible.filter { !profileManager.isPrivateApp(it.user) }
        when(filter){
            AppListFilter.All -> visible
            AppListFilter.Installed -> visible.filter { !it.isSystemApp }
            AppListFilter.System -> visible.filter { it.isSystemApp }
        }.sortedWith(compareBy({ it.section == '#' }, { it.normalizedLabel }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<LauncherApp>>(emptyList())
    val searchResults: StateFlow<List<LauncherApp>> = _searchResults

    init {
        viewModelScope.launch { streamRepository.seedDefaults() }
        // Auto-open search if enabled
        viewModelScope.launch {
            if (homePrefsRepository.searchAutoOpen.first()) isSearchingFlow.value = true
        }
        // Search collector respects showHiddenInSearch
        viewModelScope.launch {
            combine(searchQueryFlow.debounce(60), homePrefsRepository.filter, appRepository.apps, appMetaRepository.getHiddenPackageIdsFlow()){ q, f, apps, hidden -> Quad(q,f,apps, hidden) }
                .collect { quad ->
                    val q = quad.a as String
                    val filter = quad.b as AppListFilter
                    @Suppress("UNCHECKED_CAST")
                    val apps = quad.c as List<LauncherApp>
                    @Suppress("UNCHECKED_CAST")
                    val hidden = quad.d as List<String>
                    if (q.isBlank()) { _searchResults.value = emptyList(); return@collect }
                    val showHidden = try { homePrefsRepository.showHiddenInSearch.first() } catch(_:Exception){ false }
                    val hidePrivate = try { homePrefsRepository.hidePrivateSpace.first() } catch(_:Exception){ false }
                    val hiddenSet = hidden.toSet()
                    var base: List<LauncherApp> = apps
                    if (hidePrivate) base = base.filter { !profileManager.isPrivateApp(it.user) }
                    // Search ignores All/Installed/System filter — filter only affects browsing, not search
                    var searchBase: List<LauncherApp> = base
                    if (!showHidden) searchBase = searchBase.filter { it.packageName !in hiddenSet }
                    val scored = searchProvider.search(q, searchBase)
                    val mapped = scored.map { it.app }
                    _searchResults.value = mapped
                    // auto-open single result when enabled
                    if (mapped.size == 1) {
                        val autoOpen = try { homePrefsRepository.autoOpenApps.first() } catch(_:Exception){ false }
                        if (autoOpen) {
                            val target = mapped.first()
                            // debounce: only auto-open if query still matches and not already launching
                            try { tryLaunch(target) } catch(_:Exception){}
                        }
                    }
                }
        }
        // Purge stale AppMeta entries when installed apps change
        viewModelScope.launch {
            appRepository.apps.collect { apps ->
                val keep = apps.map { it.packageName }.distinct()
                try { appMetaRepository.purgeNotIn(keep) } catch (_: Exception) {}
                try { appMetaRepository.purgeEmpty() } catch (_: Exception) {}
            }
        }
    }

    private data class Quad<A,B,C,D>(val a:A, val b:B, val c:C, val d:D)
    private data class Quint<A,B,C,D,E>(val a:A, val b:B, val c:C, val d:D, val e:E)

    fun onSearchQueryChange(q: String) { searchQueryFlow.value = q }
    fun setSearching(v: Boolean) {
        isSearchingFlow.value = v
        if (!v) { searchQueryFlow.value = ""; _searchResults.value = emptyList(); selectedSectionFlow.value = null }
    }
    fun selectSection(c: Char?) { selectedSectionFlow.value = c }
    fun clearSection() { selectedSectionFlow.value = null }

    fun setFilter(f: AppListFilter){ viewModelScope.launch{ homePrefsRepository.setFilter(f) } }
    fun setHomeMode(m: HomeMode){ viewModelScope.launch{ homePrefsRepository.setHomeMode(m) } }
    fun toggleHomePackage(pkg: String){ viewModelScope.launch{ homePrefsRepository.toggleHomePackage(pkg) } }
    fun setHomePackages(pkgs: Set<String>){ viewModelScope.launch{ homePrefsRepository.setHomePackages(pkgs) } }

    // HomePrefs setters
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

    fun launchApp(app: LauncherApp) {
        viewModelScope.launch { try{ usageRepository.recordLaunch(app.packageName)}catch(_:Exception){} }
        try { screenTimeRepository.onAppOpened(app.packageName) } catch(_:Exception){}
        appRepository.launch(app)
    }

    fun tryLaunch(app: LauncherApp) {
        viewModelScope.launch {
            when (tryOpenAppUseCase(app.packageName, false)) {
                TryOpenAppResult.ShowChallenge -> _challengeApp.value = app
                TryOpenAppResult.Launch -> launchApp(app)
            }
        }
    }

    fun confirmChallengeLaunch() {
        val app = _challengeApp.value ?: return
        _challengeApp.value = null
        viewModelScope.launch {
            when (tryOpenAppUseCase(app.packageName, true)) {
                else -> launchApp(app)
            }
        }
    }

    fun dismissChallenge() { _challengeApp.value = null }

    suspend fun setHidden(packageName: String, hidden: Boolean) = appMetaRepository.setHidden(packageName, hidden)
    suspend fun setChallenge(packageName: String, challenge: Boolean) = appMetaRepository.setChallenge(packageName, challenge)
    fun toggleHidden(app: LauncherApp) { viewModelScope.launch { val cur = appMetaRepository.isHidden(app.packageName); appMetaRepository.setHidden(app.packageName, !cur) } }
    fun toggleChallenge(app: LauncherApp) { viewModelScope.launch { val cur = appMetaRepository.isChallenge(app.packageName); appMetaRepository.setChallenge(app.packageName, !cur) } }

    fun getShortcuts(app: LauncherApp) = shortcutRepository.getShortcuts(app.packageName, app.user)
    fun launchShortcut(shortcut: android.content.pm.ShortcutInfo) = shortcutRepository.startShortcut(shortcut)

    fun toggleFavorite(app: LauncherApp) {
        viewModelScope.launch {
            val isFav = uiState.value.favorites.any { it.packageName == app.packageName && it.user == app.user }
            if (isFav) {
                val id = "${app.packageName}#${app.user.hashCode()}"
                try { favoritesRepository.unpin(id) } catch (_: Exception) {}
            } else {
                favoritesRepository.pin(app.packageName, app.activityName, app.user.hashCode().toLong())
            }
        }
    }

    fun pinApp(app: LauncherApp) {
        viewModelScope.launch { favoritesRepository.pin(app.packageName, app.activityName, app.user.hashCode().toLong()) }
    }

    fun unpinApp(app: LauncherApp) {
        viewModelScope.launch {
            val id = "${app.packageName}#${app.user.hashCode()}"
            favoritesRepository.unpin(id)
        }
    }

    fun createStream(name: String){ viewModelScope.launch{ streamRepository.create(name) } }
    fun deleteStream(id: String){ viewModelScope.launch{ streamRepository.delete(id) } }
    fun addToStream(streamId: String, app: LauncherApp){ viewModelScope.launch{ streamRepository.addApp(streamId, app.packageName, app.activityName, app.user.hashCode().toLong()) } }
    fun removeFromStream(streamId: String, packageName: String){ viewModelScope.launch{ streamRepository.removeApp(streamId, packageName) } }

    suspend fun exportBackup(): String = backupRepository.export()
    suspend fun importBackup(json: String): Boolean = backupRepository.importBackup(json)

    suspend fun setGesture(g: LauncherGesture, a: LauncherAction) = gestureRepository.set(g, a)
    fun gestureFlow(g: LauncherGesture) = gestureRepository.observe(g)
    fun performGesture(action: LauncherAction, onOpenSearch: ()->Unit={}, onOpenAppList: ()->Unit={}, onOpenSettings: ()->Unit={}) {
        gestureMapper.perform(action, onOpenSearch, onOpenAppList)
        if (action == LauncherAction.OpenSettings) try { onOpenSettings() } catch(_:Exception){}
    }
    fun tryLock(): Boolean {
        // 1) Accessibility GLOBAL_ACTION_LOCK_SCREEN if service instance available
        try {
            val inst = com.adaptive.launcher.core.accessibility.LockAccessibilityService.instance
            if (inst != null && com.adaptive.launcher.core.accessibility.LockAccessibilityService.isEnabled(context)) {
                if (inst.tryLock()) return true
            }
        } catch(_:Exception){}
        // 2) DevicePolicyManager fallback (requires device admin)
        try {
            val dpm = context.getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
            val admin = android.content.ComponentName(context, com.adaptive.launcher.core.accessibility.LockDeviceAdminReceiver::class.java)
            if (dpm != null && dpm.isAdminActive(admin)) {
                dpm.lockNow()
                return true
            }
        } catch(_:Exception){}
        return false
    }

    fun addWidgetId(id: Int){ viewModelScope.launch{ widgetRepository.addWidgetId(id) } }
    fun removeWidgetId(id: Int){ viewModelScope.launch{ widgetRepository.removeWidgetId(id) } }
    suspend fun allocateWidgetId(): Int = widgetRepository.allocateId()

    fun requestDefaultLauncherIntent() = defaultLauncherHelper.createRoleRequestIntent()
    fun refreshDefaultLauncherState() { selectedSectionFlow.value = selectedSectionFlow.value }
}
