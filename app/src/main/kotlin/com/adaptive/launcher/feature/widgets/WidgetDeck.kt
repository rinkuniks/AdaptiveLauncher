package com.adaptive.launcher.feature.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.domain.widgets.WidgetHostHelper

@Composable
fun WidgetDeck(
    widgetIds: List<Int>,
    helper: WidgetHostHelper,
    repository: WidgetRepository,
    modifier: Modifier = Modifier,
    onRemove: (Int) -> Unit = {},
    onAddClicked: () -> Unit = {},
) {
    val context = LocalContext.current
    if (widgetIds.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Widgets", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onAddClicked) { Text("Add widget") }
            }
        }
        return
    }
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
        items(widgetIds, key = { it }) { id ->
            val info: AppWidgetProviderInfo? = remember(id) { helper.providerInfo(id) }
            Card(modifier = Modifier.width(300.dp).height(180.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(Modifier.fillMaxSize()) {
                    if (info != null) {
                        AndroidView(factory = { ctx ->
                            helper.createView(ctx, id, info).apply {
                                // Ensure layout params allow widget to measure
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        }, modifier = Modifier.fillMaxSize(), update = { view ->
                            try { view.updateAppWidgetOptions(info.let { null }) } catch (_: Exception) {}
                        })
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Widget unavailable #$id", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { onRemove(id) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onAddClicked, modifier = Modifier.height(180.dp).width(120.dp)) { Text("+ Add") }
        }
    }
}
