package com.adaptive.launcher.feature.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adaptive.launcher.data.home.HomeAlignment
import com.adaptive.launcher.data.home.HomeVAlignment
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.themes.ThemeMode
import com.adaptive.launcher.navigation.Destinations
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(navController: NavController, vm: SettingsViewModel = hiltViewModel()){
    val ui by vm.ui.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val json = ui.backupJson ?: return@rememberLauncherForActivityResult
            try { ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } } catch (_: Exception) {}
        }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
                vm.setBackupJson(json)
                scope.launch { vm.importBackup() }
            } catch (_: Exception) {}
        }
    }
    val widgetPick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val id = res.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            vm.onWidgetPicked(id)
        }
    }
    var gesturePickerFor by remember { mutableStateOf<LauncherGesture?>(null) }
    var showHiddenPicker by remember { mutableStateOf(false) }
    var showChallengePicker by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adaptive Settings") },
                navigationIcon = { IconButton(onClick={ navController.popBackStack()} ){ Icon(Icons.Filled.ArrowBack, contentDescription="Back") }},
                actions = { IconButton(onClick={ scope.launch{ vm.refresh() }} ){ Icon(Icons.Filled.Refresh, contentDescription="Refresh") }}
            )
        }
    ){ pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical=12.dp)){
            item{
                Text("Device & diagnostics", style=MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(ui.deviceLabel, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Refresh ${ui.refreshHz}Hz • sw${ui.swDp}dp • ${ui.layoutMode}", style=MaterialTheme.typography.labelSmall)
                        Text("Profiles: ${ui.profilesLabel}", style=MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(onClick={ navController.navigate(Destinations.ScreenTime) }){ Text("Screen time", style=MaterialTheme.typography.labelSmall) }
                            Text(if(ui.hideScreenTimePage) "ScreenTime page hidden" else "ScreenTime visible", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item{
                Text("Theme", style=MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    ThemeMode.values().forEach{ m ->
                        FilterChip(selected=ui.themeMode==m, onClick={ vm.setTheme(m)}, label={ Text(m.name)})
                    }
                }
                Spacer(Modifier.height(6.dp))
                LabeledSwitch("Show wallpaper (transparent background)", ui.showWallpaper){ vm.setShowWallpaper(it) }
                LabeledSwitch("Dynamic color (Android 12+)", ui.dynamicColor){ vm.setDynamicColor(it) }
                Text("Font", style=MaterialTheme.typography.labelMedium, modifier=Modifier.padding(top=6.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    com.adaptive.launcher.domain.themes.ThemeRepository.fonts.forEach{ f ->
                        FilterChip(selected=ui.fontName==f, onClick={ vm.setFont(f)}, label={ Text(f, style=MaterialTheme.typography.labelSmall)})
                    }
                }
                Text(if(ui.showWallpaper) "Wallpaper visible — Home background transparent." else "Wallpaper off — solid theme background.", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item{
                Text("Appearance", style=MaterialTheme.typography.titleMedium)
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                    LabeledSwitch("Show clock", ui.showClock){ vm.setShowClock(it) }
                    LabeledSwitch("Big clock", ui.bigClock, enabled = ui.showClock){ vm.setBigClock(it) }
                    LabeledSwitch("12-hour clock", ui.twelveHour, enabled = ui.showClock){ vm.setTwelveHour(it) }
                    LabeledSwitch("Show date", ui.showDate){ vm.setShowDate(it) }
                    LabeledSwitch("Show status bar", ui.showStatusBar){ vm.setShowStatusBar(it) }
                    LabeledSwitch("Show weather placeholder", ui.showWeather){ vm.setShowWeather(it) }
                    LabeledSwitch("Haptic feedback", ui.hapticFeedback){ vm.setHapticFeedback(it) }
                }
            }
            item{
                Text("Navigation", style=MaterialTheme.typography.titleMedium)
                LabeledSwitch("Enable pager: Home <-> Apps <-> Screen time swipe (opt-in)", ui.enablePager){ vm.setEnablePager(it) }
                Text("Off = single Home screen with drawer overlay (default). On = swipeable pager.", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item{
                Text("Alignment", style=MaterialTheme.typography.titleMedium)
                Text("Home horizontal • vertical, and app list alignment (Escape parity).", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically){ Text("Home H:", style=MaterialTheme.typography.labelSmall); HomeAlignment.values().forEach{ a -> FilterChip(selected=ui.homeAlignment==a, onClick={ vm.setHomeAlignment(a)}, label={ Text(a.name, style=MaterialTheme.typography.labelSmall)}) } }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically){ Text("Home V:", style=MaterialTheme.typography.labelSmall); HomeVAlignment.values().forEach{ a -> FilterChip(selected=ui.homeVAlignment==a, onClick={ vm.setHomeVAlignment(a)}, label={ Text(a.name, style=MaterialTheme.typography.labelSmall)}) } }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalAlignment=Alignment.CenterVertically){ Text("Apps:", style=MaterialTheme.typography.labelSmall); HomeAlignment.values().forEach{ a -> FilterChip(selected=ui.appsAlignment==a, onClick={ vm.setAppsAlignment(a)}, label={ Text(a.name, style=MaterialTheme.typography.labelSmall)}) } }
            }
            item{
                Text("Screen time", style=MaterialTheme.typography.titleMedium)
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                    LabeledSwitch("Show screen time on Home glance", ui.showScreenTimeHome){ vm.setShowScreenTimeHome(it) }
                    LabeledSwitch("Show per-app screen time", ui.showScreenTimeApp){ vm.setShowScreenTimeApp(it) }
                    LabeledSwitch("Hide ScreenTime dashboard page", ui.hideScreenTimePage){ vm.setHideScreenTimePage(it) }
                    Text("Dashboard keeps 2 days only; old data purged daily at midnight. Matches Escape history.", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                Text("Search & behaviour", style=MaterialTheme.typography.titleMedium)
                Column(verticalArrangement=Arrangement.spacedBy(2.dp)){
                    LabeledSwitch("Show search box", ui.showSearchBox){ vm.setShowSearchBox(it) }
                    LabeledSwitch("Auto-open search on Home", ui.searchAutoOpen){ vm.setSearchAutoOpen(it) }
                    LabeledSwitch("Bottom search", ui.bottomSearch){ vm.setBottomSearch(it) }
                    LabeledSwitch("Auto-open single result", ui.autoOpenApps){ vm.setAutoOpenApps(it) }
                    LabeledSwitch("Show hidden apps in search", ui.showHiddenInSearch){ vm.setShowHiddenInSearch(it) }
                    LabeledSwitch("Hide private space", ui.hidePrivateSpace){ vm.setHidePrivateSpace(it) }
                    LabeledSwitch("First-time help tip", ui.firstTimeHelp){ vm.setFirstTimeHelp(it) }
                }
            }
            item{
                Text("Hidden apps", style=MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                    Text("${ui.hiddenIds.size} hidden", style=MaterialTheme.typography.labelMedium)
                    if(ui.hiddenIds.isNotEmpty()) TextButton(onClick={ vm.clearHidden() }){ Text("Clear all") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick={ showHiddenPicker = true }){ Text("Manage") }
                }
                if(ui.hiddenIds.isNotEmpty()){
                    Text(ui.hiddenIds.take(6).joinToString(", "), style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                Text("Open challenges (friction)", style=MaterialTheme.typography.titleMedium)
                Text("5..1 countdown before opening. Per-app.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                    Text("${ui.challengeIds.size} challenged", style=MaterialTheme.typography.labelMedium)
                    if(ui.challengeIds.isNotEmpty()) TextButton(onClick={ vm.clearChallenges() }){ Text("Clear all") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick={ showChallengePicker = true }){ Text("Manage") }
                }
                if(ui.challengeIds.isNotEmpty()){
                    Text(ui.challengeIds.take(6).joinToString(", "), style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                Text("Double-tap to lock", style=MaterialTheme.typography.titleMedium)
                LabeledSwitch("Double-tap Home to lock", ui.doubleTapToLock){ vm.setDoubleTapToLock(it) }
                Text(if(ui.hasAccessibilityAccess) "Accessibility enabled • lock will work" else "Enable accessibility for lock to work", style=MaterialTheme.typography.labelSmall, color=if(ui.hasAccessibilityAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                TextButton(onClick={ try{ ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }catch(_:Exception){} }){ Text(if(ui.hasAccessibilityAccess) "Manage access" else "Enable access") }
            }
            item{
                Text("Gestures", style=MaterialTheme.typography.titleMedium)
                Text("Tap to choose action.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(LauncherGesture.all){ g ->
                val current = ui.gestureMap[g] ?: LauncherAction.None
                ListItem(
                    headlineContent={ Text(g::class.simpleName ?: "Gesture") },
                    supportingContent={ Text(LauncherAction.displayName(current)) },
                    trailingContent={ TextButton(onClick={ gesturePickerFor = g }){ Text("Choose") } },
                    modifier=Modifier.clickable{ gesturePickerFor = g }
                )
                Divider()
            }
            item{
                Text("Streams", style=MaterialTheme.typography.titleMedium)
                Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    var name by remember{ mutableStateOf("")}
                    OutlinedTextField(value=name, onValueChange={name=it}, label={Text("New stream")}, modifier=Modifier.weight(1f))
                    Button(onClick={ if(name.isNotBlank()){ vm.createStream(name); name=""} }){ Text("Add")}
                }
            }
            items(ui.streams, key={it.id}){ s ->
                ListItem(
                    headlineContent={ Text(s.name)},
                    supportingContent={ Text(s.id.take(8))},
                    trailingContent={
                        Row(horizontalArrangement=Arrangement.spacedBy(2.dp), verticalAlignment=Alignment.CenterVertically){
                            TextButton(onClick={ vm.moveStream(s.id, -1)}){ Text("\u2191") }
                            TextButton(onClick={ vm.moveStream(s.id, 1)}){ Text("\u2193") }
                            TextButton(onClick={ vm.deleteStream(s.id)}){ Text("Delete")}
                        }
                    },
                    modifier=Modifier.clickable{ navController.navigate("stream/${s.id}") }
                )
            }
            item{
                Text("Notifications", style=MaterialTheme.typography.titleMedium)
                Text(if(ui.hasNotificationAccess) "Listener enabled • badges live" else "NotificationListenerService processes locally, never uploads.", style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically){
                    Button(onClick={ try{ ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:Exception){} }){ Text(if(ui.hasNotificationAccess) "Manage access" else "Enable access")}
                    if(!ui.hasNotificationAccess) Text("Disabled", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.error)
                    else Text("Enabled", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.primary)
                }
                if(ui.notifications.isNotEmpty()){
                    ui.notifications.entries.take(6).forEach{ (pkg, list) ->
                        Text("$pkg: ${list.firstOrNull()?.title ?: ""} — ${list.firstOrNull()?.text ?: ""} (${list.size})", style=MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text("No notifications yet", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                Text("Widgets", style=MaterialTheme.typography.titleMedium)
                Text("Widget Deck hosts AppWidgetHost.", style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={
                        try {
                            val mgr = AppWidgetManager.getInstance(ctx)
                            vm.requestWidgetPicker(ctx)
                        } catch(_:Exception){ vm.requestWidgetPicker(ctx) }
                    }){ Text("Pick via Settings") }
                    OutlinedButton(onClick={
                        try {
                            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(ctx.applicationContext, com.adaptive.launcher.feature.home.WidgetEntryPoint::class.java)
                            val repo = entry.widgetRepo()
                            val id = repo.allocateId()
                            if(id!=-1){
                                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply{ putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
                                widgetPick.launch(intent)
                            }
                        } catch(_:Exception){ vm.requestWidgetPicker(ctx) }
                    }){ Text("Pick widget") }
                }
                Text("${ui.widgetInfo}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(ui.widgetIds.isNotEmpty()){
                    Text("Pinned IDs: ${ui.widgetIds.joinToString()}", style=MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        ui.widgetIds.take(4).forEach{ wid ->
                            AssistChip(onClick={ vm.removeWidget(wid) }, label={ Text("Remove $wid") })
                        }
                    }
                }
            }
            item{
                Text("Backup / Restore", style=MaterialTheme.typography.titleMedium)
                Text("Save to file or load from file.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={
                        scope.launch{
                            vm.exportBackup()
                            val name = "adaptive-backup-${System.currentTimeMillis()}.json"
                            createDoc.launch(name)
                        }
                    }){ Text("Export to file")}
                    OutlinedButton(onClick={ openDoc.launch(arrayOf("application/json")) }){ Text("Import from file")}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick={ scope.launch{ vm.exportBackup() } }){ Text("Export (copy)") }
                    OutlinedButton(onClick={ scope.launch{ vm.importBackup() } }){ Text("Import (paste)") }
                }
                ui.backupJson?.let{ Text(it.take(800), style=MaterialTheme.typography.labelSmall) }
                ui.backupStatus?.let{ Text(it, color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.labelMedium)}
                var paste by remember{ mutableStateOf("")}
                OutlinedTextField(value=paste, onValueChange={paste=it}, label={Text("Paste JSON to import")}, modifier=Modifier.fillMaxWidth(), minLines=2)
                Button(onClick={ if(paste.isNotBlank()){ vm.setBackupJson(paste); scope.launch{ vm.importBackup() } } }, modifier=Modifier.fillMaxWidth()){ Text("Apply pasted JSON")}
            }
            item{
                Text("Privacy", style=MaterialTheme.typography.titleMedium)
                Text("Local-first. No account. No cloud. No ads.", style=MaterialTheme.typography.bodySmall)
            }
        }
    }
    gesturePickerFor?.let { g ->
        AlertDialog(onDismissRequest={ gesturePickerFor=null }, title={ Text("Choose action for ${g::class.simpleName}") }, text={
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                val options = listOf(LauncherAction.None, LauncherAction.OpenSearch, LauncherAction.OpenAppList, LauncherAction.OpenSettings, LauncherAction.OpenNotifications)
                options.forEach{ a ->
                    Row(Modifier.fillMaxWidth().clickable{ vm.setGestureDirect(g, a); gesturePickerFor=null }.padding(vertical=8.dp), verticalAlignment=Alignment.CenterVertically){
                        RadioButton(selected = (ui.gestureMap[g]==a), onClick={ vm.setGestureDirect(g, a); gesturePickerFor=null })
                        Spacer(Modifier.width(8.dp))
                        Text(LauncherAction.displayName(a))
                    }
                }
            }
        }, confirmButton={ TextButton(onClick={ gesturePickerFor=null }){ Text("Close") } })
    }
    if(showHiddenPicker){
        AlertDialog(onDismissRequest={ showHiddenPicker=false }, title={ Text("Hidden apps") }, text={
            val filtered = if(query.isBlank()) ui.allAppsForMeta.take(200) else ui.allAppsForMeta.filter{ it.label.contains(query, true) || it.packageName.contains(query, true) }.take(200)
            Column{
                OutlinedTextField(value=query, onValueChange={ vm.setQuery(it)}, label={ Text("Search apps")}, modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max=380.dp)){
                    items(filtered, key={ it.packageName + "_h"}){ app ->
                        val isHidden = app.packageName in ui.hiddenIds
                        Row(Modifier.fillMaxWidth().clickable{ vm.toggleHidden(app.packageName)}.padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically){
                            Checkbox(checked=isHidden, onCheckedChange={ vm.toggleHidden(app.packageName)})
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){ Text(app.label, style=MaterialTheme.typography.bodySmall); Text(app.packageName, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }, confirmButton={ TextButton(onClick={ showHiddenPicker=false }){ Text("Done") }})
    }
    if(showChallengePicker){
        AlertDialog(onDismissRequest={ showChallengePicker=false }, title={ Text("Open challenges") }, text={
            val filtered = if(query.isBlank()) ui.allAppsForMeta.take(200) else ui.allAppsForMeta.filter{ it.label.contains(query, true) || it.packageName.contains(query, true) }.take(200)
            Column{
                OutlinedTextField(value=query, onValueChange={ vm.setQuery(it)}, label={ Text("Search apps")}, modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max=380.dp)){
                    items(filtered, key={ it.packageName + "_c"}){ app ->
                        val isC = app.packageName in ui.challengeIds
                        Row(Modifier.fillMaxWidth().clickable{ vm.toggleChallenge(app.packageName)}.padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically){
                            Checkbox(checked=isC, onCheckedChange={ vm.toggleChallenge(app.packageName)})
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)){ Text(app.label, style=MaterialTheme.typography.bodySmall); Text(app.packageName, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant) }
                            if(app.isSystemApp) Text("System", style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }, confirmButton={ TextButton(onClick={ showChallengePicker=false }){ Text("Done") }})
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean)->Unit){
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween){
        Text(label, style=MaterialTheme.typography.bodyMedium, modifier=Modifier.weight(1f))
        Switch(checked=checked, onCheckedChange=onChange, enabled=enabled)
    }
}
