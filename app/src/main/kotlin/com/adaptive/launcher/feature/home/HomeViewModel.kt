package com.adaptive.launcher.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptive.launcher.core.model.LauncherApp
import com.adaptive.launcher.data.apps.AppRepository
import com.adaptive.launcher.data.favorites.FavoritesRepository
import com.adaptive.launcher.data.home.AppListFilter
import com.adaptive.launcher.data.home.HomeMode
import com.adaptive.launcher.data.home.HomePrefsRepository
import com.adaptive.launcher.data.notifications.NotificationRepository
import com.adaptive.launcher.data.shortcuts.ShortcutRepository
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.data.backup.BackupRepository
import com.adaptive.launcher.domain.context.ContextEngine
import com.adaptive.launcher.domain.context.UsageRepository
import com.adaptive.launcher.domain.gestures.GestureMapper
import com.adaptive.launcher.domain.gestures.GestureRepository
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.profiles.ProfileManager
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedSectionFlow = MutableStateFlow<Char?>(null)
    private val isSearchingFlow = MutableStateFlow(false)

    val profiles = profileManager.getProfiles()

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.apps,
        favoritesRepository.favorites,
        searchQueryFlow.debounce(80),
        selectedSectionFlow,
        isSearchingFlow,
        usageRepository.signals(),
        streamRepository.streams,
        notificationRepository.notifications,
        homePrefsRepository.filter,
        homePrefsRepository.homeMode,
        homePrefsRepository.homePackages,
        widgetRepository.widgetIds,
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

        val favApps = favEntities.mapNotNull { fav -> apps.find { it.packageName == fav.packageName } }
        val favSet = favApps.map { it.packageName to it.user }.toSet()
        val allWithFavFlag = apps.map { it.copy(isFavorite = favSet.contains(it.packageName to it.user)) }

        val filteredByType = when(filter){
            AppListFilter.All -> allWithFavFlag
            AppListFilter.Installed -> allWithFavFlag.filter { !it.isSystemApp }
            AppListFilter.System -> allWithFavFlag.filter { it.isSystemApp }
        }

        val homeVisible = when(homeMode){
            HomeMode.ShowAll -> filteredByType
            HomeMode.AutoMajor -> filteredByType.filter { app ->
                !app.isSystemApp || app.packageName in HomePrefsRepository.MajorPackages
            }
            HomeMode.Custom -> {
                if(homePkgs.isEmpty()) filteredByType.filter { !it.isSystemApp }.take(12)
                else filteredByType.filter { it.packageName in homePkgs }
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // Drawer exposed separately for bottom sheet (full filtered list)
    val drawerApps: StateFlow<List<LauncherApp>> = combine(appRepository.apps, homePrefsRepository.filter){ apps, filter ->
        val mapped = apps
        when(filter){
            AppListFilter.All -> mapped
            AppListFilter.Installed -> mapped.filter { !it.isSystemApp }
            AppListFilter.System -> mapped.filter { it.isSystemApp }
        }.sortedWith(compareBy({ it.section == '#' }, { it.normalizedLabel }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<LauncherApp>>(emptyList())
    val searchResults: StateFlow<List<LauncherApp>> = _searchResults

    init {
        viewModelScope.launch { streamRepository.seedDefaults() }
        viewModelScope.launch {
            combine(searchQueryFlow.debounce(60), homePrefsRepository.filter, appRepository.apps){ q, f, apps -> Triple(q,f,apps) }
                .collect { (q, filter, apps) ->
                    if (q.isBlank()) { _searchResults.value = emptyList(); return@collect }
                    val filteredForSearch = when(filter){
                        AppListFilter.All -> apps
                        AppListFilter.Installed -> apps.filter { !it.isSystemApp }
                        AppListFilter.System -> apps.filter { it.isSystemApp }
                    }
                    val scored = searchProvider.search(q, filteredForSearch)
                    _searchResults.value = scored.map { it.app }
                }
        }
    }

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

    fun launchApp(app: LauncherApp) {
        viewModelScope.launch { try{ usageRepository.recordLaunch(app.packageName)}catch(_:Exception){} }
        appRepository.launch(app)
    }

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
        // Extra: OpenSettings is handled by mapper via context intent; allow callback override
        if (action == LauncherAction.OpenSettings) try { onOpenSettings() } catch(_:Exception){}
    }

    fun addWidgetId(id: Int){ viewModelScope.launch{ widgetRepository.addWidgetId(id) } }
    fun removeWidgetId(id: Int){ viewModelScope.launch{ widgetRepository.removeWidgetId(id) } }
    suspend fun allocateWidgetId(): Int = widgetRepository.allocateId()

    fun requestDefaultLauncherIntent() = defaultLauncherHelper.createRoleRequestIntent()
    fun refreshDefaultLauncherState() { selectedSectionFlow.value = selectedSectionFlow.value }
}
