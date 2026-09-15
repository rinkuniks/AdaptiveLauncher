# Escape Launcher → Adaptive Launcher — Deep Read & Implementation Plan

Source: https://github.com/georgeclensy/Escape-Launcher (master @ 2026-09, minSdk 28 target 36, Kotlin, Compose M3, Hilt, Room, DataStore, Navigation3, WorkManager)
Analysed via local clone at /tmp/escape — 240 Kotlin files, ~9 modules.

> Goal: port Escape's addiction-reduction philosophy (favorite-first home, friction on distracting apps, built-in screen time, minimalist search/private-space awareness) into Adaptive Launcher (one-handed, minimal, adaptive) without losing Adaptive's existing pillars (context suggestions, streams, gesture mapper, widget deck, work profile).

---

## 1. What Escape actually is

Minimalist launcher replacing HOME. Three pager pages:
- **ScreenTime dashboard** (page 0, hidable)
- **Home** (favorites + clock/date/screen-time glance + weather + AppWidget)
- **AppsList** (full list, searchable, private-space & work-apps sections)

Long-press anywhere on home → Settings. Swipe-down on home drag → expand notifications panel. Double-tap home → lock screen via AccessibilityService. Back → animate to home page. Home button → GlobalViewModel.navigateHomeEvent.

Design language: M3 dynamic/preset themes + font picker (google fonts vs foss fonts) + wallpaper toggle.

### 1.1 Module map

```
app                 : MainHomeScreenActivity (pager, splash, analytics, messaging, widgets, ScreenOffReceiver, ClearOldDataWorker)
core/common         : DefaultSettings, StringUtils.formatScreenTime, AppUtils.isMainUserApp, PermissionUtils, WindowUtils
core/data           : Room (AppDatabase `modifiedApps` + `app_usage`), DataStore (PreferencesKeys ~26 keys), repositories
core/di             : CoroutineModule (AppScope, IoDispatcher)
core/domain         : use-cases (GetFavoriteApps, TryOpenApp, LaunchApp, GetAppActions, ScreenTime, SearchApps, ManagedProfiles, Clock, Onboarding init)
core/model          : InstalledApp(packageName, displayName, componentName, user), ModifiedApp, AppUsage, AppUsageUiModel
core/theme          : AppColourScheme, PresetColourSchemes (13), EscapeType, FontResolver (google vs foss flavor)
core/ui             : Clock, OpenChallenge, ScreenTimeDashComposables, HomeScreenItem, AppListHeader, GlanceWidget, Blur/ gradients
feature/homescreen  : Home.kt (favorites LazyColumn + Clock + Glance row + WidgetRenderer) + NewHomeScreenViewModel + ClockViewModel
feature/appslist    : AppsList.kt + AppsListViewModel
feature/screentime  : ScreenTimeDash.kt + ScreenTimeViewModel
feature/privatespace, workapps, securefolder : managed-profile sections injected into pager/apps list as extraListItems/floatingContent
feature/newwidgets  : WidgetHostManager + WidgetRenderer + WidgetPicker (single widget id, width/height/offset)
feature/onboarding  : 6 pages (welcome, favorites, analytics, launcher, accessibility, statistics) + finish
feature/settings    : Settings.kt + mainpage/theme/font/hidden/openChallenges/widget/weather/devOptions
feature/weather     : foss vs google flavor via WeatherProxy/WeatherImpl
```

Build: convention plugins (AndroidApplication/Library/Compose/Hilt/Room), fossDebug/googleDebug fossRelease/googleRelease flavors, Firebase Messaging stubbed in foss.

### 1.2 Data truth

**DataStore `settings`** — 26 PreferencesKeys (mirrored in DefaultSettings):
analytics, haptic, hidePrivateSpace, twelveHourClock/showClock/bigClock/showDate/showStatusBar/showScreenTimeHome/showWeather/useFahrenheit/showScreenTimeApp/firstTimeHelp, homeVAlignment/homeAlignment, weatherAppPackage, appsAlignment, widgetOffset/Height/Width/Id, theme/showWallpaper/font, showSearchBox/searchAutoOpen/bottomSearch/autoOpenApps, doubleTapToLock/hideScreenTimePage/showHiddenInSearch/firstTime.
Migration from legacy SharedPreferences (`com.geecee.escapelauncher` + `2131755384`) → DataStore + Room at first DataStore read on `preferencesDataStore(produceMigrations=SharedPreferencesToDataStoreMigration)`.

**Room — `modified_apps_database`**
```kotlin
@Entity("modifiedApps") ModifiedAppEntity(packageId PK, displayName?, isHidden, isChallenge, favouritePosition Double?)
@Entity("app_usage") AppUsageEntity(packageName PK = "$pkg-$yyyy-MM-dd", totalTime Long)
```
DAOs:
- ModifiedAppsDao: getFavoriteFlow/IDs, getHiddenFlow, getChallengeFlow, add/remove/reorder/tidy/purge, displayName helpers, ensureRowExists.
  Reorder uses fractional doubles (gap/2, compaction when gap <1e-10) — no index rewrite storm.
- AppUsageDao: insertOrUpdate, getAllFlow, deleteOldDataExcept("%-today","%-yesterday"), getTotal/UsageList for dateFlow, sumFlow.
Databases: `AppDatabase` (app_usage) + `ModifiedAppsDatabase` (modifiedApps). Provided via DatabaseModule.

**System sources**
- AppsRepositoryImpl: LauncherApps.getActivityList(null, user) per UserManager.userProfiles, filters out own package, debounced 500ms via SharedFlow trigger, distinctBy pkg+user, sorted. Also shortcuts via LauncherApps.ShortcutQuery.
- ManagedProfileRepositoryImpl / isSupported checks privateSpace (Android 15 VANILLA_ICE_CREAM managed profile) vs work profile vs Samsung secure folder reflection.
- EscapeAccessibilityService (AccessibilityService) — only for GLOBAL_ACTION_LOCK_SCREEN.

### 1.3 Feature deep dives (the parts we must clone)

**Screen time tracking**
```
launch → LaunchAppUseCase returns bool → caller (MainPagerScreenViewModel.openApp → onAppOpened(pkg) OR HomeScreen -> same)
     onAppOpened(pkg) { appSessions[pkg]=now }
return to launcher:
  MainHomeScreenActivity.onResume() + ScreenOffReceiver.onReceive(SCREEN_OFF)
     → if hasActiveSession() → repo.onAppClosed(activePkg)
        onAppClosed: usageTime = now - openTime; key="$pkg-$today"; upsert sum
dashboard:
  ScreenTimeViewModel.datesFlow: emit (today, yesterday) + loop delay until next midnight (+1s buffer)
     totalUsage = totalForDateFlow(today)  stateIn WhileSubscribed(5000)
     yesterdayTotal = totalForDateFlow(yesterday)
     appUsageUiList = GetAppUsageUiListUseCase(today,yesterday)
        combine(todaySortedFlow, yesterdaySortedFlow, installedApps) →
           map { increase = today> yesterday, name = installed.find(pkg)?.displayName ?: "null" } filter null
UI: formatScreenTime(ms) -> "5h 3m" | "12m" | "45s"
     today header + arrow (red up / green down), 2 infoBoxes ( % of 16h day, % over 30m rec ), AppUsages box (per app "12m" or "<1m")
cleanup: ClearOldDataWorker periodic 1 day, initialDelay to midnight, deleteOldDataExcept(today,yesterday) daily
```
Edge: getDisplayName fallback "null" filter hides uninstalled apps. ScreenOff log includes formatted time.

**Open challenge (friction)**
```
ModifiedApps.isChallenge(pkg) flag per app toggled in Settings (OpenChallenges page)
TryOpenAppUseCase(pkg, bypass=false): if bypass → Launch; else if isChallenge → ShowChallenge else Launch
MainPagerScreen: openApp() in MainPagerScreenViewModel checks result
  ShowChallenge → showOpenChallenge=true, keep currentSelectedApp
  Launch → launchAppUseCase(app, onAppOpened) → goToMainPage delayed 500ms; if bypass also hide challenge
UI: OpenChallenge Composable full-screen gradient B2D8D8→004C4C, AnimatedVisibility steps 5..1
     LaunchedEffect: loop steps.size, showText 3s + 1s transition, haptic LongPress per tick if enabled, nextScreen after 5→1 then openApp(bypass=true) after 500ms
     Back button → showOpenChallenge=false
     Next screen gradient second fadeIn — intentional double Box.
```
Settings/Onboarding expose add/remove challenge Chips + App list.

**Hidden apps / Favorites**
```
ModifiedApps.isHidden toggle via HiddenApps screen (search + show-hidden-in-search flag)
FavoritePosition Double? ordering: addFavourite = lastPos+1; removeFavourite = clear; reorderFavouriteApp(pId,from,to) fractional; tidyFavouritePositions mapIndexed index.toDouble
AppsListViewModel combines installedApps + hiddenIds + challengeIds + favIds etc to filter out hidden unless showHiddenInSearch
Home only renders favoriteApps via GetFavoriteAppsUseCase (installedApps + favs join, sorted by favouritePosition)
Hidden apps hidden from home entirely; apps list optionally hides behind search term for private space (search term gate).
```

**Clock & Glance**
```
ClockViewModel: ticker Job with delay(millisUntilNextMinute) per 12h flag; timeParts Triple(hour,minute,isAm)
Home: SimpleDateFormat("EEE d MMM") midnight ticker; Timer glance = formatScreenTime(todayUsage); Weather glance via WeatherViewModel
Alignments: HomeAlignment Left/Center/End mapped from string key; HomeVAlignment Top/Center/Bottom using Arrangement
BigClock toggle doubles size, 12h toggle via GetCurrentTimePartsUseCase.
```

**Search & AppsList**
```
SearchAppsUseCase fuzzy (SearchUtils levenshtein-ish), SearchSettings showSearchBox/searchAutoOpen/bottomSearch/autoOpenApps
AppsList header sticky alphabet rail A-Z + '#', alignment Left/center drag, show/hide search, extraListItems inject SecureFolder/PrivateSpace.
```

**Widgets**
```
WidgetSettings: widgetId (-1 none) + offset/height/width persisted; WidgetHostManager startListening in onStart/stopListening in onStop
WidgetRenderer composable renders host view; ConfigureWidgetActivity for binding.
```

---

## 2. Adaptive Launcher today (what we already have)

Single `app` module, package com.adaptive.launcher, Hilt+KSP, Room 2.8.4, DataStore 1.1.4, Navigation Compose (not Navigation3).

DB: `AppDatabase` v2 [FavoriteEntity(id PK "$pkg#userSerial", packageName, activityName, position Int, userSerial Long), StreamEntity + CrossRef]. No modifiedApps, no app_usage.
DataStore split: `SettingsRepository` (adaptive_prefs: hiddenApps set, favorites_order string, isFirstLaunch) + `HomePrefsRepository` (home_prefs: filter All/Installed/System, homeMode AutoMajor/Custom/ShowAll, homePackages set, showSystemBadge) + Gesture/Theme/Widget/Backup repos similar.
App discovery: `AppRepository` via LauncherApps/UserManager similar but exposes AppListFilter System vs Installed heuristic (FLAG_SYSTEM) + normalizedLabel/section.
ViewModel: `HomeViewModel` combine apps+favorites+search+signals+streams+notifs+homePrefs+widgetIds → HomeUiState (sections, drawerSections, contextSuggestions, etc). Search via AppSearchProvider (fuzzy + CalculatorProvider). Favorites reorder via orderedIds list (int positions, not fractional) in FavoritesRepository.
UI: `HomeRoute/HomeScreen` single screen merging home favorites + app list + search + alphabet rail + swipe up/down gestures + widget deck + notification pills + streams chips + filter/HomeMode chips. `HomeActivity` (HOME) + `MainActivity` (LAUNCHER) both inflate AppNavHost (home/settings/onboarding/streamDetail). No pager, no separate appsList/screentime pages. No screen time, no challenge, no true hidden/metadata DB, no clock ticker separation (simple SimpleDateFormat each second in ClockHeader), no alignment prefs, no wallpaper toggle.

Good: adaptive already has streams, context engine, gesture layer, widget deck, backup, shortcuts/action sheet — Escape lacks these. Keep them.

---

## 3. Design decision — what Adaptive adopts from Escape

Principles: Adaptive stays one-handed minimal adaptive identity; Escape's *addictive-friction* becomes Adaptive's *intentional use* feature behind a toggle, not forced.

Adopt fully:
1. Screen time tracking (built-in, no UsageStats permission) — total + per-app, today vs yesterday trend, daily cleanup. Value-add for adaptive "know your time" insight.
2. Open challenge countdown (per-app) — optional friction stored per app, countdown overlay before launch.
3. Hidden apps management (Room-backed, survives reinstall filtering, search toggle).
4. Clock/date/glance toggles + alignment + haptic + screen-time-on-home/app flags.
5. formatScreenTime + ScreenOffReceiver pattern + midnight rollover.

Adapt partially:
- History kept 2 days only (not full history) — keeps DB tiny, privacy-friendly.
- Pager: Adaptive will NOT replace single-screen home with 3-page pager immediately. Instead expose screen time as a dedicated route `Destinations.ScreenTime` reachable from home glance / settings, preserving current HomeScreen gesture model. Future: opt-in pager.
- Theme/fonts: keep Adaptive's ThemeRepository; extend with wallpaper toggle + font key if needed later.
- Private-space/work-apps: Adaptive already handles multi-user; wire ProfileManager awareness (hidePrivateSpace key) but no separate PrivateSpace UI v1.

Don't copy blindly:
- FOSS/google flavor split, Firebase messaging, analytics, weather provider — not needed.
- Navigation3 + AppNavKey sealed — stay on navigation-compose.

---

## 4. Implementation plan (phased, acceptance criteria)

### Phase 0 — Repo hygiene & versioning
- Bump `AppDatabase` v2 → v3 adding `app_usage` + `app_meta` tables, `fallbackToDestructiveMigration(true)` keeps dev builds green.
- Add `androidx.work:work-runtime-ktx:2.10.0` + `hilt-work` to libs.versions.toml.
- Add `formatScreenTime` util under `core/common` (TimeUnit).

### Phase 1 — Data layer (foundation)  — DONE when Room builds & tests pass
1. `core/model/AppUsage.kt`, `AppUsageUiModel.kt`, `AppMeta.kt` (mirrors ModifiedApp but name AppMeta to avoid clash with FavoriteEntity). Fields: packageId PK, displayName?, isHidden, isChallenge.
2. `data/screentime/AppUsageEntity`, `AppUsageDao`, `data/apps/AppMetaEntity`, `AppMetaDao` (methods: getById, upsert, getHiddenFlow/ChallengeFlow, isHidden/isChallenge, setHidden/Challenge via ensureRowExists transaction, purge).
3. `data/screentime/ScreenTimeRepository` interface + `ScreenTimeRepositoryImpl` (ConcurrentHashMap sessions, onAppOpened/Closed, hasActiveSession, flows) — port Escape logic verbatim.
4. `data/apps/AppMetaRepository` interface+impl.
5. `di/AppModule` provides DAOs + repos + Request for `androidx.hilt.work.HiltWorkerFactory` if using hilt-work.
6. `core/common/StringUtils.kt` add formatScreenTime, `core/common/DefaultSettings` additions (SHOW_CLOCK etc) if needed else keep local constants.
Accept: `./gradlew assembleDebug` green, DAOs instrumented test inserts round-trip.

### Phase 2 — Screen time feature  — DONE when dashboard renders real data
1. `domain/screentime/GetAppUsageUiListUseCase` combine flows (screenTime today/yesterdayAppUsages + appRepository.apps) exactly like Escape.
2. `feature/screentime/ScreenTimeViewModel` datesFlow midnight ticker + totalUsage/yesterdayTotal/appUsageUiList StateFlows.
3. `feature/screentime/ScreenTimeDash.kt` composables: `ScreenTime(time,increased)`, `ScreenTimeInfoBox`, `AppUsageRow`, `AppUsages` card, `calculateOveragePercentage`. Follow M3 colors, `escapeGreen/escapeRed` mapped to Adaptive theme `Color(0xFF4CAF50)/Error`.
4. `data/worker/ClearOldDataWorker` (HiltWorker or plain CoroutineWorker) scheduleDailyCleanup at midnight, called from AdaptiveLauncherApp.onCreate.
5. `navigation/Destinations.ScreenTime` + NavHost entry; home glance timer chip navigates there; settings entry.
6. Lifecycle wiring: `MainActivity`/`HomeActivity` onResume + `ScreenOffReceiver` (BroadcastReceiver for SCREEN_OFF) close active app session and persist. `HomeViewModel.launchApp` calls `screenTimeRepository.onAppOpened(pkg)` on success.
Accept: launch Chrome → press home → open dashboard shows Chrome + time >0; rotate after midnight still shows fresh day; adb shell am broadcast screen_off writes correctly.

### Phase 3 — Open challenge  — DONE when distracting app shows 5..1 overlay
1. `domain/apps/TryOpenAppUseCase` (or inline in HomeViewModel) checks AppMetaRepository.isChallenge.
2. `core/ui/composables/OpenChallenge.kt` 5-4-3-2-1 AnimatedVisibility, haptics gate, goBack/openApp callbacks.
3. `HomeViewModel` state: `showChallengeFlow: MutableStateFlow<LauncherApp?>`, `openApp(app, bypass)` suspend: if !bypass and isChallenge → emit challenge else appRepository.launch + screenTime.onAppOpened; onChallengeComplete recall with bypass.
4. `HomeRoute` overlays OpenChallenge AnimatedVisibility when challengeApp != null, similar to Escape MainPagerScreen.
5. Settings: `Open Challenges` page lists apps with toggle isChallenge, plus hidden-compat isMainUserApp guard if multi-user.
Accept: mark Instagram as challenge in settings → tap it on home → see 5..1 gradient → after countdown app launches; Back exits overlay; haptics respect setting.

### Phase 4 — Home personalization (Escape parity)
- Extend `HomePrefsRepository` keys: showClock/bigClock/twelveHour/showDate/showScreenTimeHome/showScreenTimeApp/showStatusBar/homeAlignment/homeVAlignment/hapticFeedback via boolean/string prefs (reuse settingsDataStore or homePrefs).
- `ClockViewModel` extracted ticker (millisUntilNextMinute) or keep ClockHeader but gate with settings.
- Home.kt: Glance row (date + screen time glance + weather placeholder) gated, alignment mapping (Alignment.Start/Center.End + Arrangement.Top/Center/Bottom), haptics via LocalHapticFeedback gated.
- Add `ScreenOffReceiver` + `EscapeAccessibilityService`-like lock? Optional: reuse existing gesture double-tap → lock if permission.
Accept: settings toggles hide/show clock/date/screen-time chip; home alignment chips move content; haptics toggle silences.

### Phase 5 — Hidden apps & search alignment
- Migrate hidden set from SettingsRepository stringSet to AppMetaRepository isHidden flow; provide toggle in HiddenApps settings (LazyColumn with search filtering, showHiddenInSearch flag).
- AppsList/HomeViewModel filter logic: `filtered = if showHiddenInSearch then all else all filter !isHidden`.
- Private-space: add hidePrivateSpace flag (bool) mirrored from HomePrefs; gate work/private profile sections (already multi-user aware) similar to Escape's isHiddenPrivateSpace.
Accept: hide WhatsApp → disappears from home & drawer; toggle "show hidden in search" → reappears when searching; search still finds via provider but filtered.

### Phase 6 — Navigation & onboarding (future)
- Optional pager `MainPagerScreen` with hideScreenTimePage flag, Home↔AppsList↔ScreenTime HorizontalPager; onboarding 4-page minimal (welcome, pick favorites, allow haptics/widgets, challenge intro) using SettingsRepository.isFirstLaunch.
Defer to v0.4.

### Phase 7 — Polish
- Purge stale AppMeta via purgeAppsWithNoData after package uninstall refresh.
- Unit tests: ScreenTimeRepository onAppClosed sum, ModifiedApps reorder fractions, GetAppUsageUiListUseCase trend, formatScreenTime.
- F-Droid compliance: no proprietary deps added.

---

## 5. Mapping Adaptive current → Escape target (file-level)

| Escape file | Adaptive counterpart | action |
|---|---|---|
| PreferencesKeys + DefaultSettings | `data/settings/SettingsRepository` + `data/home/HomePrefsRepository` + new `core/common/DefaultSettings.kt` | merge keys, add migration fn if legacy prefs exist |
| ModifiedAppEntity/DAO/Repository | `data/apps/AppMetaEntity.kt` / `AppMetaDao.kt` / `AppMetaRepository.kt` | new |
| AppUsageEntity/DAO/Repository | `data/screentime/*` | new |
| GetAppUsageUiListUseCase | `domain/screentime/GetAppUsageUiListUseCase.kt` | new |
| ScreenTimeViewModel + ScreenTimeDash + ScreenTimeDashComposables | `feature/screentime/*` | new, create route |
| TryOpenAppUseCase + LaunchAppUseCase | `domain/apps/TryOpenAppUseCase.kt`, extend existing `AppRepository.launch` | new/wrap |
| OpenChallenge.kt | `core/ui/composables/OpenChallenge.kt` | new |
| Home.kt / ClockViewModel | `feature/home/HomeRoute.kt` + new `feature/home/ClockViewModel.kt` | extend with settings gates |
| ClearOldDataWorker | `data/worker/ClearOldDataWorker.kt` | new |
| ScreenOffReceiver + MainHomeScreenActivity lifecycle | `core/ui/receivers/ScreenOffReceiver.kt` + `launcher/HomeActivity` onResume/onStart hook | add |
| HiddenApps settings | `feature/settings/SettingsRoute.kt` new section | extend |
| Theme(FontResolver) | `domain/themes/ThemeRepository` | no change v1 |

---

## 6. Concrete file/todo checklist for this PR (Phase 1-3 minimal shippable)

- [x] libs.versions.toml: add work-runtime 2.10.0, hilt-work 1.2.0 if not present
- [x] app/build.gradle.kts: add work & hilt-work deps
- [x] data/favorites/AppDatabase.kt: v3 + entities [FavoriteEntity, StreamEntity, StreamAppCrossRef, AppMetaEntity, AppUsageEntity]
- [x] data/apps/AppMetaEntity.kt, AppMetaDao.kt
- [x] data/screentime/AppUsageEntity.kt, AppUsageDao.kt
- [x] data/screentime/ScreenTimeRepository.kt (+Impl) / data/apps/AppMetaRepository.kt (+Impl)
- [x] core/common/FormatUtils.kt: formatScreenTime
- [x] di/AppModule.kt: provide daos + repos
- [x] domain/screentime/GetAppUsageUiListUseCase.kt
- [x] feature/screentime/ScreenTimeViewModel.kt + ScreenTimeDash.kt (route)
- [x] core/ui/composables/OpenChallenge.kt
- [x] feature/home/HomeViewModel.kt: inject meta+screenTime repos, add challenge state + onAppOpened hook, expose totals via combine or delegate to viewModel
- [x] feature/home/HomeRoute.kt: overlay challenge, glance chip wired to screenTime, launch path tryChallenge
- [x] launcher/HomeActivity.kt / MainActivity.kt: onResume active-session flush + ScreenOffReceiver registration + worker schedule
- [x] data/worker/ClearOldDataWorker.kt
- [x] navigation/AppNavHost.kt: add ScreenTime destination
- [x] feature/settings/SettingsRoute.kt: add Hidden apps + Open challenges + Screen time card
- [ ] tests: AppLabelNormalizer existing stays green

---

## 7. Risks & mitigations

- Room migration destructive: dev builds ok; before release write Migration 2→3 (CREATE TABLE app_meta/app_usage) to keep user favorites.
- LauncherApps requires HOME role for shortcuts/private space — gracefully return empty when SecurityException.
- Midnight ticker flow holds ViewModelScope; use WhileSubscribed(5000) + distinctUntilChanged so config change doesn't leak.
- HiltWorker needs @HiltWorker + WorkManager init; fallback to plain CoroutineWorker if hilt-work not wired.
- App sessions map race: ConcurrentHashMap + onAppOpened overwrites; onAppClosed removes — single active session contract matches Escape (getActiveSessionPackageName returns first key).

---

## 8. Verification

Build: `gradle :app:assembleDebug` must pass on JDK 21 / AGP 8.13 / SDK 36.
Runtime: launch app, set 2 challenges, verify countdown, open ScreenTime Dash, verify times grow after app use, verify hidePrivateSpace/search toggles, verify worker enqueue log.
Tests: existing 7 unit tests + new screenTime formatter test.
