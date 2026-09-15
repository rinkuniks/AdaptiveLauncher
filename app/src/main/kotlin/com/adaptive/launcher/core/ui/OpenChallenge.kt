package com.adaptive.launcher.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun OpenChallenge(
    haptics: HapticFeedback,
    enabled: Boolean,
    openApp: () -> Unit,
    goBack: () -> Unit
) {
    val steps = listOf("5", "4", "3", "2", "1")
    var stepIndex by remember { mutableIntStateOf(0) }
    var showText by remember { mutableStateOf(true) }
    var nextScreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (nextScreen) return@LaunchedEffect
        while (stepIndex < steps.size) {
            if (showText) { delay(3000); showText = false }
            delay(1000)
            stepIndex++
            if (stepIndex < steps.size) {
                showText = true
                if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                nextScreen = true
                if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(500)
                openApp()
            }
        }
    }

    val currentText = if (stepIndex < steps.size) steps[stepIndex] else ""
    val gradient = Brush.linearGradient(colors = listOf(Color(0xFFB2D8D8), Color(0xFF004C4C)), start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    if (!nextScreen) {
        Box(Modifier.fillMaxSize().background(gradient).pointerInput(Unit) {}, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = showText, enter = fadeIn(tween(1000)), exit = fadeOut(tween(1000))) {
                    Text(currentText, Modifier.padding(32.dp), color = Color.White, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress); goBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface)
                ) { Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back") }
            }
        }
    } else {
        Box(Modifier.fillMaxSize().background(gradient).pointerInput(Unit) {}, contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = true, enter = fadeIn(tween(1000)), exit = fadeOut(tween(1000))) {
                Box(Modifier.fillMaxSize().background(gradient), contentAlignment = Alignment.Center) {}
            }
        }
    }
}
