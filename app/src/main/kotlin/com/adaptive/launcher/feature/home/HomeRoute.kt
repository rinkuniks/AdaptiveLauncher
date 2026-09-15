package com.adaptive.launcher.feature.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adaptive.launcher.core.device.LayoutMode
import com.adaptive.launcher.core.device.rememberLayoutConfig
import com.adaptive.launcher.core.model.LauncherApp
import com.adaptive.launcher.core.performance.ColdStartTracer
import com.adaptive.launcher.data.home.AppListFilter
import com.adaptive.launcher.data.home.HomeMode
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.search.CalculatorProvider
import com.adaptive.launcher.feature.widgets.WidgetDeck
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel(), onOpenSettings: ()->Unit = {}, onOpenOnboarding: ()->Unit = {}, onOpenStream: (String)->Unit = {}) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerApps by viewModel.drawerApps.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshDefaultLauncherState()
    }
    // Observe gesture actions for reactive handling
    val swipeUpAction by viewModel.gestureFlow(LauncherGesture.SwipeUp).collectAsStateWithLifecycle(initialValue = LauncherAction.OpenAppList)
    val swipeDownAction by viewModel.gestureFlow(LauncherGesture.SwipeDown).collectAsStateWithLifecycle(initialValue = LauncherAction.OpenSearch)
    val doubleTapAction by viewModel.gestureFlow(LauncherGesture.DoubleTap).collectAsStateWithLifecycle(initialValue = LauncherAction.None)

    LaunchedEffect(Unit) { ColdStartTracer.markFirstFrame() }

    HomeScreen(
        uiState = ui,
        drawerApps = drawerApps,
        searchResults = searchResults,
        onLaunch = { viewModel.launchApp(it) },
        onSearchChange = { viewModel.onSearchQueryChange(it) },
        onSearchingChange = { viewModel.setSearching(it) },
        onSectionSelected = { viewModel.selectSection(it) },
        onClearSection = { viewModel.clearSection() },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onPin = { viewModel.pinApp(it) },
        onUnpin = { viewModel.unpinApp(it) },
        onOpenSettings = onOpenSettings,
        onOpenStream = onOpenStream,
        onAddToStream = { streamId, app -> viewModel.addToStream(streamId, app) },
        onRemoveFromStream = { streamId, pkg -> viewModel.removeFromStream(streamId, pkg) },
        onSetFilter = { viewModel.setFilter(it) },
        onSetHomeMode = { viewModel.setHomeMode(it) },
        onToggleHomePkg = { viewModel.toggleHomePackage(it) },
        onPerformSwipeUp = { viewModel.performGesture(swipeUpAction, onOpenSearch = { viewModel.setSearching(true) }, onOpenAppList = {}, onOpenSettings = onOpenSettings) },
        onPerformSwipeDown = { viewModel.performGesture(swipeDownAction, onOpenSearch = { viewModel.setSearching(true) }, onOpenAppList = {}, onOpenSettings = onOpenSettings) },
        onPerformDoubleTap = { viewModel.performGesture(doubleTapAction, onOpenSearch = { viewModel.setSearching(!ui.isSearching) }, onOpenAppList = {}, onOpenSettings = onOpenSettings) },
        onAddWidgetId = { id -> viewModel.addWidgetId(id) },
        onRemoveWidgetId = { id -> viewModel.removeWidgetId(id) },
        onRequestDefaultLauncher = {
            val intent = viewModel.requestDefaultLauncherIntent()
            if (intent != null) {
                try { roleLauncher.launch(intent) } catch (_: Exception) {
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    drawerApps: List<LauncherApp>,
    searchResults: List<LauncherApp>,
    onLaunch: (LauncherApp) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchingChange: (Boolean) -> Unit,
    onSectionSelected: (Char?) -> Unit,
    onClearSection: () -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onPin: (LauncherApp) -> Unit,
    onUnpin: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStream: (String) -> Unit,
    onAddToStream: (String, LauncherApp) -> Unit,
    onRemoveFromStream: (String, String) -> Unit,
    onSetFilter: (AppListFilter) -> Unit,
    onSetHomeMode: (HomeMode) -> Unit,
    onToggleHomePkg: (String) -> Unit,
    onPerformSwipeUp: () -> Unit,
    onPerformSwipeDown: () -> Unit,
    onPerformDoubleTap: () -> Unit,
    onAddWidgetId: (Int) -> Unit,
    onRemoveWidgetId: (Int) -> Unit,
    onRequestDefaultLauncher: () -> Unit,
) {
    var showAppSheet by remember { mutableStateOf<LauncherApp?>(null) }
    var showDrawer by remember { mutableStateOf(false) }
    var showHomePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val drawerListState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val layout = rememberLayoutConfig()
    val myUser = android.os.Process.myUserHandle()
    val calculator = remember { CalculatorProvider() }
    val context = LocalContext.current

    val horizontalPad = when {
        layout.swDp >= 600 -> 28.dp
        layout.swDp >= 400 -> 20.dp
        layout.swDp >= 360 -> 16.dp
        else -> 14.dp
    }
    val railWidth = when {
        layout.swDp >= 600 -> 44.dp
        layout.isTall -> 34.dp
        else -> 32.dp
    }
    val displayList: List<LauncherApp> = if (uiState.isSearching) searchResults else uiState.filteredApps
    val calcResult = if (uiState.isSearching) calculator.evaluate(uiState.searchQuery) else null

    LaunchedEffect(uiState.selectedSection) {
        val sel = uiState.selectedSection ?: return@LaunchedEffect
        val idx = displayList.indexOfFirst { it.section == sel }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    // Notification access check for banner
    val hasNotifAccess = remember {
        try {
            val enabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            enabled.contains(context.packageName)
        } catch (_: Exception) { false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(showDrawer, uiState.isSearching) {
                var totalY = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalY = 0f },
                    onVerticalDrag = { change, dragAmount -> totalY += dragAmount; change.consume() },
                    onDragEnd = {
                        if (totalY < -90 && !uiState.isSearching && !showDrawer) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPerformSwipeUp()
                            // Default still opens drawer if gesture is OpenAppList/None
                            if (!showDrawer) showDrawer = true
                        }
                        if (totalY > 110 && !uiState.isSearching && !showDrawer) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPerformSwipeDown()
                        }
                        if (totalY > 120 && showDrawer) showDrawer = false
                        totalY = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPerformDoubleTap()
                })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = horizontalPad)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                ClockHeader(compact = !layout.isTall)
                IconButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            if (!uiState.isDefaultLauncher) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Set Adaptive as default launcher", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Button(onClick = onRequestDefaultLauncher, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Set default", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }

            if (!hasNotifAccess && uiState.notifications.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Enable notification badges for workspace", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            try { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                        }) { Text("Enable", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            // Filter chips - All / Installed (default) / System
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Filter:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp))
                AppListFilter.values().forEach { f ->
                    val label = when(f){ AppListFilter.All->"All"; AppListFilter.Installed->"Installed"; AppListFilter.System->"System" }
                    FilterChip(selected = uiState.appFilter==f, onClick = { onSetFilter(f) }, label = { Text(label, style=MaterialTheme.typography.labelSmall) })
                }
                Spacer(Modifier.weight(1f))
                Text("${displayList.size}/${drawerApps.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Home mode chooser
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Home:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(selected = uiState.homeMode==HomeMode.AutoMajor, onClick={ onSetHomeMode(HomeMode.AutoMajor)}, label={ Text("Auto", style=MaterialTheme.typography.labelSmall)})
                FilterChip(selected = uiState.homeMode==HomeMode.Custom, onClick={ onSetHomeMode(HomeMode.Custom); showHomePicker=true }, label={ Text("Custom", style=MaterialTheme.typography.labelSmall)})
                FilterChip(selected = uiState.homeMode==HomeMode.ShowAll, onClick={ onSetHomeMode(HomeMode.ShowAll)}, label={ Text("Show all", style=MaterialTheme.typography.labelSmall)})
                if(uiState.homeMode==HomeMode.Custom){
                    AssistChip(onClick={ showHomePicker=true }, label={ Text("Choose apps (${uiState.homePackages.size})", style=MaterialTheme.typography.labelSmall)})
                }
            }

            // Widget Deck (always visible section, even if empty shows affordance)
            WidgetDeckSection(uiState = uiState, onAddWidgetId = onAddWidgetId, onRemoveWidgetId = onRemoveWidgetId, onOpenSettings = onOpenSettings)

            if (uiState.contextSuggestions.isNotEmpty() && !uiState.isSearching && uiState.selectedSection == null) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Suggested", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("based on time & recent use", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.contextSuggestions.take(6), key = { it.packageName }) { s ->
                                val app = uiState.allApps.find { it.packageName == s.packageName }
                                AssistChip(onClick = { app?.let(onLaunch) }, label = { Text(app?.label ?: s.packageName, maxLines = 1) }, leadingIcon = { Text(app?.label?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }
            }

            // Notification workspace pills: show up to 3 most recent app notifications as chips
            if (uiState.notifications.isNotEmpty() && !uiState.isSearching) {
                val pillApps = uiState.notifications.entries.sortedByDescending { it.value.maxOfOrNull { n -> n.whenMs } ?: 0L }.take(4)
                if (pillApps.isNotEmpty()) {
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pillApps, key = { it.key }) { (pkg, list) ->
                            val app = uiState.allApps.find { it.packageName == pkg }
                            val title = list.firstOrNull()?.title ?: list.firstOrNull()?.text ?: pkg
                            AssistChip(
                                onClick = { app?.let(onLaunch) },
                                label = { Text("${app?.label ?: pkg}  ${list.size}", maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text(list.size.toString(), color = MaterialTheme.colorScheme.onError, fontSize = 10.sp) } }
                            )
                        }
                    }
                }
            }

            if (uiState.streams.isNotEmpty() && !uiState.isSearching) {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(uiState.streams, key = { it.id }) { stream ->
                        FilterChip(selected = false, onClick = { onOpenStream(stream.id) }, label = { Text(stream.name) })
                    }
                    item { AssistChip(onClick = onOpenSettings, label = { Text("+ Stream") }) }
                }
            }

            if (uiState.favorites.isNotEmpty() && !uiState.isSearching && uiState.selectedSection == null) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text("Favorites", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    uiState.favorites.forEach { fav ->
                        val notifCount = uiState.notifications[fav.packageName]?.size ?: 0
                        FavoriteRow(app = fav, notifCount = notifCount, isWork = fav.user != myUser, onLaunch = { onLaunch(fav) }, onUnpin = { onUnpin(fav) }, onLongPress = { showAppSheet = fav })
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }

            SearchBar(query = uiState.searchQuery, isSearching = uiState.isSearching, onQueryChange = onSearchChange, onSearchingChange = onSearchingChange)

            if (calcResult != null && uiState.isSearching) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("= $calcResult", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(uiState.searchQuery, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val isLandscape = layout.mode == LayoutMode.LandscapeWide
                if (isLandscape && !uiState.isSearching) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(0.42f)) {
                            Text("Favorites", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            uiState.favorites.forEach { fav ->
                                FavoriteRow(app = fav, notifCount = uiState.notifications[fav.packageName]?.size ?: 0, isWork = fav.user != myUser, onLaunch = { onLaunch(fav) }, onUnpin = { onUnpin(fav) }, onLongPress = { showAppSheet = fav })
                            }
                            if (uiState.favorites.isEmpty()) Text("Pin apps for quick access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(Modifier.weight(0.58f).fillMaxHeight()) {
                            if (displayList.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (uiState.allApps.isEmpty()) "No apps found" else "No results", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            } else {
                                val groupedLandscape: List<Pair<Char, List<LauncherApp>>> = displayList.groupBy { it.section }.toList().sortedBy { if (it.first=='#') Int.MAX_VALUE else it.first.code }
                                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = railWidth + 4.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)) {
                                    groupedLandscape.forEach { (section, apps) ->
                                        item(key = "header_$section") { Text(section.toString(), modifier = Modifier.padding(top = 10.dp, bottom = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                                        items(apps, key = { it.packageName + it.activityName + it.user.hashCode() }) { app -> AppRow(app = app, notifCount = uiState.notifications[app.packageName]?.size ?: 0, isWork = app.user != myUser, onLaunch = { onLaunch(app) }, onLongPress = { showAppSheet = app }) }
                                    }
                                }
                                if (!uiState.isSearching) AlphabetRail(sections = uiState.sections, selected = uiState.selectedSection, modifier = Modifier.align(Alignment.CenterEnd).width(railWidth).fillMaxHeight(), onSectionSelected = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSectionSelected(it) }, onClear = onClearSection)
                            }
                        }
                    }
                } else {
                    if (displayList.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (uiState.allApps.isEmpty()) "No apps found" else "No results for \"${uiState.searchQuery}\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val grouped: List<Pair<Char, List<LauncherApp>>> = if (uiState.isSearching) {
                            @Suppress("UNCHECKED_CAST") listOf('#' to (displayList as List<LauncherApp>))
                        } else {
                            @Suppress("UNCHECKED_CAST") (displayList as List<LauncherApp>).groupBy { it.section }.toList().sortedBy { if (it.first=='#') Int.MAX_VALUE else it.first.code }
                        }
                        if (layout.columns > 1 && !uiState.isSearching && uiState.selectedSection == null) {
                            LazyVerticalGrid(columns = GridCells.Fixed(layout.columns), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 64.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                grouped.forEach { (section, apps) ->
                                    item(key = "hdr_$section") { Text(section.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                                    items(apps, key = { it.packageName + it.activityName + it.user.hashCode() }) { app -> AppRow(app = app, notifCount = uiState.notifications[app.packageName]?.size ?: 0, isWork = app.user != myUser, onLaunch = { onLaunch(app) }, onLongPress = { showAppSheet = app }) }
                                }
                            }
                        } else {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = railWidth + 4.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 64.dp)) {
                                grouped.forEach { (section, apps) ->
                                    if (!uiState.isSearching) { item(key = "header_$section") { Text(section.toString(), modifier = Modifier.padding(top = 10.dp, bottom = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
                                    items(apps, key = { it.packageName + it.activityName + it.user.hashCode() }) { app -> AppRow(app = app, notifCount = uiState.notifications[app.packageName]?.size ?: 0, isWork = app.user != myUser, onLaunch = { onLaunch(app) }, onLongPress = { showAppSheet = app }) }
                                }
                            }
                            if (!uiState.isSearching) AlphabetRail(sections = uiState.sections, selected = uiState.selectedSection, modifier = Modifier.align(Alignment.CenterEnd).width(railWidth).fillMaxHeight(), onSectionSelected = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSectionSelected(it) }, onClear = onClearSection)
                        }
                    }
                }
            }

            // Swipe-up affordance
            if(!uiState.isSearching){
                Row(Modifier.fillMaxWidth().clickable{ showDrawer=true }.padding(vertical=8.dp), horizontalArrangement=Arrangement.Center, verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription=null, tint=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Swipe up for all apps  \u2022  ${drawerApps.size} apps  \u2022  ${uiState.appFilter.name}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick={ showDrawer=true }, label={ Text("Open drawer")})
                }
            }
        }

        showAppSheet?.let { app ->
            AppActionSheet(app = app, isFavorite = uiState.favorites.any { it.packageName == app.packageName }, streams = uiState.streams, onDismiss = { showAppSheet = null }, onLaunch = { onLaunch(app); showAppSheet = null }, onToggleFavorite = { onToggleFavorite(app); showAppSheet = null }, onAddToStream = { sid -> onAddToStream(sid, app); showAppSheet = null })
        }
    }

    if(showDrawer){
        ModalBottomSheet(onDismissRequest={ showDrawer=false }, sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=horizontalPad).padding(bottom=12.dp)){
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically){
                    Text("All apps", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        AppListFilter.values().forEach{ f ->
                            FilterChip(selected=uiState.appFilter==f, onClick={ onSetFilter(f)}, label={ Text(when(f){AppListFilter.All->"All"; AppListFilter.Installed->"Installed"; AppListFilter.System->"System"}, style=MaterialTheme.typography.labelSmall)})
                        }
                    }
                }
                Text("${drawerApps.size} apps  \u2022  ${uiState.appFilter.name}  \u2022  tap or long-press", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(top=4.dp, bottom=8.dp))
                val groupedDrawer = drawerApps.groupBy{ it.section }.toList().sortedBy{ if(it.first=='#') Int.MAX_VALUE else it.first.code }
                LazyColumn(state=drawerListState, modifier=Modifier.fillMaxWidth().heightIn(max=420.dp), contentPadding=PaddingValues(bottom=16.dp)){
                    groupedDrawer.forEach{ (section, apps) ->
                        item(key="d_hdr_$section"){ Text(section.toString(), modifier=Modifier.padding(top=10.dp, bottom=4.dp), style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary, fontWeight=FontWeight.Bold) }
                        items(apps, key={ it.packageName + it.activityName + it.user.hashCode() + "_d"}){ app ->
                            AppRow(app=app, notifCount=uiState.notifications[app.packageName]?.size ?: 0, isWork=app.user != myUser, onLaunch={ onLaunch(app); showDrawer=false }, onLongPress={ showAppSheet=app })
                        }
                    }
                }
            }
        }
    }

    if(showHomePicker){
        AlertDialog(
            onDismissRequest={ showHomePicker=false },
            title={ Text("Choose home apps")},
            text={
                val all = drawerApps.take(140)
                LazyColumn(modifier=Modifier.fillMaxWidth().heightIn(max=380.dp)){
                    items(all, key={ it.packageName + "_pick"}){ app ->
                        val checked = app.packageName in uiState.homePackages
                        Row(Modifier.fillMaxWidth().clickable{ onToggleHomePkg(app.packageName)}.padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically){
                            Checkbox(checked=checked, onCheckedChange={ onToggleHomePkg(app.packageName)})
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){
                                Text(app.label, maxLines=1, overflow=TextOverflow.Ellipsis, style=MaterialTheme.typography.bodySmall, fontWeight=FontWeight.Medium)
                                Text(app.packageName, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant, maxLines=1)
                            }
                            if(app.isSystemApp) AssistChip(onClick={}, label={ Text("System", style=MaterialTheme.typography.labelSmall)}, modifier=Modifier.padding(start=6.dp))
                        }
                    }
                }
            },
            confirmButton={ TextButton(onClick={ showHomePicker=false}){ Text("Done")}},
            dismissButton={ TextButton(onClick={ showHomePicker=false }){ Text("Close")}}
        )
    }
}

@Composable
private fun WidgetDeckSection(uiState: HomeUiState, onAddWidgetId: (Int)->Unit, onRemoveWidgetId: (Int)->Unit, onOpenSettings: ()->Unit) {
    val context = LocalContext.current
    var injectedHelper: com.adaptive.launcher.domain.widgets.WidgetHostHelper? by remember { mutableStateOf(null) }
    var injectedRepo: com.adaptive.launcher.data.widgets.WidgetRepository? by remember { mutableStateOf(null) }
    // Lazy resolve via Hilt EntryPoint if available, else degrade gracefully
    LaunchedEffect(Unit) {
        try {
            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            injectedHelper = entry.widgetHelper()
            injectedRepo = entry.widgetRepo()
        } catch (_: Exception) {}
    }
    val widgetIds = uiState.widgetIds
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) onAddWidgetId(id)
        }
    }
    // Only render deck when we have helper/repo and on APIs where host works; always show add affordance
    if (widgetIds.isNotEmpty() && injectedHelper != null && injectedRepo != null) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            WidgetDeck(widgetIds = widgetIds, helper = injectedHelper!!, repository = injectedRepo!!, onRemove = onRemoveWidgetId, onAddClicked = {
                try {
                    val id = injectedRepo!!.allocateId()
                    if (id != -1) {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
                        pickLauncher.launch(intent)
                    }
                } catch (_: Exception) {}
            })
        }
    } else if (widgetIds.isEmpty()) {
        // Minimal affordance row so feature is discoverable without clutter
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Widgets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = {
                try {
                    if (injectedRepo != null) {
                        val id = injectedRepo!!.allocateId()
                        if (id != -1) {
                            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
                            pickLauncher.launch(intent)
                        }
                    } else onOpenSettings()
                } catch (_: Exception) { onOpenSettings() }
            }) { Text("Add widget", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetHelper(): com.adaptive.launcher.domain.widgets.WidgetHostHelper
    fun widgetRepo(): com.adaptive.launcher.data.widgets.WidgetRepository
}

@Composable
private fun ClockHeader(compact: Boolean = false) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); kotlinx.coroutines.delay(1000) } }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now).uppercase(Locale.getDefault()) }
    Column(modifier = Modifier.padding(top = if (compact) 10.dp else 14.dp)) {
        Text(time, style = MaterialTheme.typography.displaySmall.copy(fontSize = if (compact) 32.sp else 36.sp), fontWeight = FontWeight.Light)
        Text(date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
    }
}

@Composable
private fun SearchBar(query: String, isSearching: Boolean, onQueryChange: (String) -> Unit, onSearchingChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        if (!isSearching) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onSearchingChange(true) }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)); Text("Search apps  \u00b7  swipe down  \u00b7  double-tap", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 12.dp), singleLine = true, decorationBox = { inner -> if (query.isEmpty()) Text("Type to search  \u2022  try \"yt\"  \u2022  \"12*19\"", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() })
                Spacer(Modifier.width(8.dp)); TextButton(onClick = { onSearchingChange(false) }) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun AppRow(app: LauncherApp, notifCount: Int, isWork: Boolean, onLaunch: () -> Unit, onLongPress: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = onLaunch, onLongClick = onLongPress).padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(app.label.firstOrNull()?.uppercase() ?: "#", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            if (notifCount > 0) { Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.error), contentAlignment = Alignment.Center) { Text(notifCount.coerceAtMost(9).toString(), color = MaterialTheme.colorScheme.onError, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
                if (isWork) { Box(Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("Work", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) } }
                if (app.isSystemApp) { Box(Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("System", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp) } }
            }
            Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (app.isFavorite) { Icon(Icons.Filled.Star, contentDescription = "Favorite", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun FavoriteRow(app: LauncherApp, notifCount: Int, isWork: Boolean, onLaunch: () -> Unit, onUnpin: () -> Unit, onLongPress: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = onLaunch, onLongClick = onLongPress).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(app.label.firstOrNull()?.uppercase() ?: "#", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp)); Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (isWork) { Box(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("Work", fontSize = 9.sp) } }
        if (app.isSystemApp) { Box(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.outlineVariant).padding(horizontal = 5.dp, vertical = 2.dp)) { Text("Sys", fontSize = 9.sp) } }
        if (notifCount > 0) { Badge(containerColor = MaterialTheme.colorScheme.error) { Text(notifCount.toString(), color = MaterialTheme.colorScheme.onError) }; Spacer(Modifier.width(6.dp)) }
        IconButton(onClick = onUnpin) { Icon(Icons.Filled.Star, contentDescription = "Unpin", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun AlphabetRail(sections: List<Char>, selected: Char?, modifier: Modifier, onSectionSelected: (Char) -> Unit, onClear: () -> Unit) {
    val allLetters = ('A'..'Z').toList()
    Box(modifier = modifier.pointerInput(sections) { detectVerticalDragGestures(onDragStart = { offset -> val idx = ((offset.y / size.height) * allLetters.size).toInt().coerceIn(0, allLetters.size - 1); onSectionSelected(allLetters[idx]) }, onVerticalDrag = { change, _ -> change.consume(); val idx = ((change.position.y / size.height) * allLetters.size).toInt().coerceIn(0, allLetters.size - 1); onSectionSelected(allLetters[idx]) }, onDragEnd = {}, onDragCancel = {}) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            allLetters.forEach { c -> val hasApps = sections.contains(c); val isSel = selected == c; Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent).clickable { onSectionSelected(c) }.padding(horizontal = 6.dp, vertical = 1.dp), contentAlignment = Alignment.Center) { Text(c.toString(), fontSize = if (isSel) 15.sp else 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = when { isSel -> MaterialTheme.colorScheme.onPrimary; hasApps -> MaterialTheme.colorScheme.onBackground; else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f) }) } }
            Spacer(Modifier.height(4.dp)); Text("#", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.clickable { onSectionSelected('#') })
        }
    }
}

@Composable
private fun AppActionSheet(app: LauncherApp, isFavorite: Boolean, streams: List<com.adaptive.launcher.data.streams.StreamEntity>, onDismiss: () -> Unit, onLaunch: () -> Unit, onToggleFavorite: () -> Unit, onAddToStream: (String) -> Unit) {
    val context = LocalContext.current
    var shortcuts by remember { mutableStateOf<List<android.content.pm.ShortcutInfo>>(emptyList()) }
    LaunchedEffect(app.packageName) {
        try {
            val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps
            val q = android.content.pm.LauncherApps.ShortcutQuery().setPackage(app.packageName).setQueryFlags(android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            shortcuts = try { launcherApps.getShortcuts(q, app.user) ?: emptyList() } catch (_: Exception) { emptyList() }
        } catch (_: Exception) {}
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(app.label) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    if(app.user != android.os.Process.myUserHandle()){ AssistChip(onClick={}, label={Text("Work profile")}, leadingIcon={ Text("W", style=MaterialTheme.typography.labelSmall)}) }
                    if(app.isSystemApp){ AssistChip(onClick={}, label={ Text("System app")}) }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp)); TextButton(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) { Text("Open app", modifier = Modifier.fillMaxWidth()) }
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isFavorite) "Remove from favorites" else "Add to favorites") } }
                if(shortcuts.isNotEmpty()){ HorizontalDivider(Modifier.padding(vertical = 6.dp)); Text("Shortcuts", style=MaterialTheme.typography.labelMedium); shortcuts.take(4).forEach{ sc -> TextButton(onClick={ try{ val la = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as android.content.pm.LauncherApps; la.startShortcut(sc, null, null)}catch(_:Exception){}; onDismiss()}, modifier=Modifier.fillMaxWidth()){ Text(sc.shortLabel?.toString() ?: sc.longLabel?.toString() ?: "Shortcut", modifier=Modifier.fillMaxWidth()) } } }
                if(streams.isNotEmpty()){ HorizontalDivider(Modifier.padding(vertical = 6.dp)); Text("Add to stream", style=MaterialTheme.typography.labelMedium); LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){ items(streams, key={it.id}){ s -> AssistChip(onClick={ onAddToStream(s.id) }, label={ Text(s.name, style=MaterialTheme.typography.labelSmall)}) } } }
                HorizontalDivider(Modifier.padding(vertical = 6.dp)); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedButton(onClick={ try{ val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply{ data = android.net.Uri.parse("package:${app.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; context.startActivity(intent)}catch(_:Exception){}}, modifier=Modifier.weight(1f)){ Text("App info", style=MaterialTheme.typography.labelSmall) }; OutlinedButton(onClick={ try{ val intent = Intent(Intent.ACTION_DELETE).apply{ data = android.net.Uri.parse("package:${app.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)}; context.startActivity(intent)}catch(_:Exception){}}, modifier=Modifier.weight(1f)){ Text("Uninstall", style=MaterialTheme.typography.labelSmall) } }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
