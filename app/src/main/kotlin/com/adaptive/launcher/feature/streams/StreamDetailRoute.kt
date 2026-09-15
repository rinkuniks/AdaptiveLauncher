package com.adaptive.launcher.feature.streams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adaptive.launcher.data.streams.StreamAppCrossRef
import com.adaptive.launcher.feature.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDetailRoute(navController: NavController, streamId: String, homeViewModel: HomeViewModel = hiltViewModel()) {
    val ui by homeViewModel.uiState.collectAsStateWithLifecycle()
    val stream = ui.streams.find { it.id == streamId }
    // Collect apps in this stream via StreamRepository; for now derive from HomeViewModel by observing via LaunchedEffect
    // Simplified: show all apps with add/remove toggle backed by streamDao flows exposed via HomeViewModel
    Scaffold(topBar = {
        TopAppBar(title = { Text(stream?.name ?: "Stream") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { pad ->
        if (stream == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text("Stream not found") }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add or remove apps in '${stream.name}'", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            var filter by remember { mutableStateOf("") }
            OutlinedTextField(value = filter, onValueChange = { filter = it }, label = { Text("Filter apps") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            val filtered = remember(ui.allApps, filter) {
                val q = filter.trim().lowercase()
                if (q.isEmpty()) ui.allApps else ui.allApps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.packageName + it.user.hashCode() }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.labelSmall) },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(onClick = { homeViewModel.addToStream(streamId, app) }) { Text("Add", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(onClick = { homeViewModel.removeFromStream(streamId, app.packageName) }) { Text("Remove", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
