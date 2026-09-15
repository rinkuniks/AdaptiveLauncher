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
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.themes.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(navController: NavController, vm: SettingsViewModel = hiltViewModel()){
    val ui by vm.ui.collectAsStateWithLifecycle()
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
                        Text("Refresh rate ${ui.refreshHz}Hz \u2022 sw${ui.swDp}dp \u2022 ${ui.layoutMode}", style=MaterialTheme.typography.labelSmall)
                        Text("Profiles: ${ui.profilesLabel}", style=MaterialTheme.typography.labelSmall)
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
            }
            item{
                Text("Gestures", style=MaterialTheme.typography.titleMedium)
                Text("Tap to choose action. Long-press gesture on Home uses these bindings; swipe up/down and double-tap live-update.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(if(ui.hasNotificationAccess) "Listener enabled \u2022 badges live" else "NotificationListenerService processes locally, never uploads. Enable to show workspace badges.", style=MaterialTheme.typography.bodySmall)
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
                Text("Widget Deck hosts AppWidgetHost. Added widgets appear on Home. Use the picker below.", style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={
                        try {
                            val mgr = AppWidgetManager.getInstance(ctx)
                            val idField = vm.javaClass.getDeclaredField("widgetRepository")
                            // fallback: direct intent pick via vm
                            vm.requestWidgetPicker(ctx)
                        } catch(_:Exception){ vm.requestWidgetPicker(ctx) }
                    }){ Text("Pick via Settings") }
                    OutlinedButton(onClick={
                        // Direct pick with result callback for persistence
                        try {
                            val mgr = AppWidgetManager.getInstance(ctx)
                            // allocate via repo through vm helper - we do inline allocation here via EntryPoint
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
                Text("Save to file or load from file. Import validates schemaVersion.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Local-first. No account. No cloud. No ads. Contacts/calendar only with permission.", style=MaterialTheme.typography.bodySmall)
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
}
