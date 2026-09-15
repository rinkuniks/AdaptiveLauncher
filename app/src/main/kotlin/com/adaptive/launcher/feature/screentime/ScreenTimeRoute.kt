package com.adaptive.launcher.feature.screentime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.adaptive.launcher.core.common.formatScreenTime

@Composable
fun ScreenTimeRoute(navController: NavController, vm: ScreenTimeViewModel = hiltViewModel()) {
    val todayUsage by vm.totalUsage.collectAsStateWithLifecycle()
    val yesterdayUsage by vm.yesterdayTotalUsage.collectAsStateWithLifecycle()
    val appList by vm.appUsageUiList.collectAsStateWithLifecycle()
    ScreenTimeDashboardContent(todayUsage, yesterdayUsage, appList, onBack = { navController.popBackStack() })
}

fun calculateOveragePercentage(screenTime: Long): Int {
    val recommendedMs = 0.5 * 60 * 60 * 1000 // 30 min? Escape uses 0.5h comment says 1h but code is 0.5h
    if (screenTime <= recommendedMs) return 0
    val overage = screenTime - recommendedMs
    return ((overage / recommendedMs) * 100).toInt()
}

@Composable
fun ScreenTimeDashboardContent(todayUsage: Long, yesterdayUsage: Long, appList: List<com.adaptive.launcher.domain.screentime.AppUsageUiModel>, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("Screen Time", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ScreenTimeRow(formatScreenTime(todayUsage), increased = todayUsage > yesterdayUsage)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.widthIn(max = 400.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalDayHours = 16
            val totalMs = totalDayHours * 60L * 60 * 1000
            val percentOfDay = ((todayUsage.toDouble() / totalMs) * 100).toInt().coerceIn(0, 100)
            val green = Color(0xFF4CAF50)
            val red = MaterialTheme.colorScheme.error
            ScreenTimeInfoBox(text = "of your day on your phone", percent = percentOfDay, percentageColour = if (percentOfDay < 10) green else red, modifier = Modifier.weight(1f).aspectRatio(1f))
            val pctRec = calculateOveragePercentage(todayUsage)
            ScreenTimeInfoBox(text = "higher than recommended", percent = pctRec, percentageColour = if (pctRec < 1) green else red, modifier = Modifier.weight(1f).aspectRatio(1f))
        }
        Spacer(Modifier.height(16.dp))
        AppUsagesBox {
            if (appList.isNotEmpty()) {
                appList.forEach { u ->
                    AppUsageRow(u.appName, u.usageIncreased, if (u.totalTime > 60000) formatScreenTime(u.totalTime) else "<1m")
                }
            } else {
                Text("No apps used today", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ScreenTimeRow(time: String, increased: Boolean) {
    val green = Color(0xFF4CAF50)
    val red = MaterialTheme.colorScheme.error
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = if (increased) red else green, modifier = Modifier.size(36.dp).rotate(if (increased) 0f else 180f))
        Spacer(Modifier.width(6.dp))
        Text(time, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ScreenTimeInfoBox(text: String, percent: Int, percentageColour: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(24.dp)).aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)) {
        val pad = maxWidth * 0.1f
        val titleSize = maxWidth * 0.22f
        val bodySize = maxWidth * 0.09f
        Column(Modifier.align(Alignment.Center).padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.titleLarge.copy(fontSize = with(LocalDensity.current){ titleSize.toSp() }, fontWeight = FontWeight.Bold), color = percentageColour)
            Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = with(LocalDensity.current){ bodySize.toSp() }, lineHeight = with(LocalDensity.current){ (bodySize + 5.dp).toSp() }, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AppUsageRow(appName: String, increased: Boolean, time: String) {
    val green = Color(0xFF4CAF50)
    val red = MaterialTheme.colorScheme.error
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(if (appName.length > 14) appName.take(14) + "..." else appName, modifier = Modifier.align(Alignment.CenterStart), style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = if (increased) red else green, modifier = Modifier.size(28.dp).rotate(if (increased) 0f else 180f))
            Spacer(Modifier.width(4.dp))
            Text(time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AppUsagesBox(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}
