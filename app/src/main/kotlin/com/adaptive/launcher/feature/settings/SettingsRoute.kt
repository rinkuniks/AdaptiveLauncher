package com.adaptive.launcher.feature.settings

import android.content.Intent
import android.provider.Settings
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
                        Text("Refresh rate ${ui.refreshHz}Hz • sw${ui.swDp}dp • ${ui.layoutMode}", style=MaterialTheme.typography.labelSmall)
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
                Text("Tap a gesture to cycle its action. SwipeDown opens search by default.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(LauncherGesture.all){ g ->
                val current = ui.gestureMap[g] ?: LauncherAction.None
                ListItem(
                    headlineContent={ Text(g::class.simpleName ?: "Gesture") },
                    supportingContent={ Text(LauncherAction.displayName(current)) },
                    modifier=Modifier.clickable{ vm.cycleGesture(g) }
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
                    trailingContent={ TextButton(onClick={ vm.deleteStream(s.id)}){ Text("Delete")}}
                )
            }
            item{
                Text("Notifications", style=MaterialTheme.typography.titleMedium)
                Text("NotificationListenerService processes locally, never uploads. Tap to open system permission.", style=MaterialTheme.typography.bodySmall)
                Button(onClick={ try{ ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:Exception){} }){ Text("Open notification access")}
                if(ui.notifications.isNotEmpty()){
                    ui.notifications.forEach{ (pkg, list) ->
                        Text("$pkg: ${list.firstOrNull()?.title ?: ""} — ${list.firstOrNull()?.text ?: ""}", style=MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text("No notifications yet", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item{
                Text("Widgets", style=MaterialTheme.typography.titleMedium)
                Text("Widget Deck hosts AppWidgetHost. Use Add widget below (picker intent).", style=MaterialTheme.typography.bodySmall)
                Button(onClick={ vm.requestWidgetPicker(ctx)}){ Text("Pick widget")}
                Text("${ui.widgetInfo}", style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item{
                Text("Backup / Restore", style=MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={ scope.launch{ vm.exportBackup()} }){ Text("Export")}
                    OutlinedButton(onClick={ scope.launch{ vm.importBackup()} }){ Text("Import")}
                }
                ui.backupJson?.let{ Text(it.take(600), style=MaterialTheme.typography.labelSmall) }
                ui.backupStatus?.let{ Text(it, color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.labelMedium)}
            }
            item{
                Text("Privacy", style=MaterialTheme.typography.titleMedium)
                Text("Local-first. No account. No cloud. No ads. Contacts/calendar only with permission.", style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
