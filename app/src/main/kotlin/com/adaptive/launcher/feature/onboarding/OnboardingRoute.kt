package com.adaptive.launcher.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.adaptive.launcher.navigation.Destinations

@Composable
fun OnboardingRoute(nav: NavController){
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Welcome to Adaptive Launcher", style=MaterialTheme.typography.headlineSmall)
        Text("Fast • Minimal • One-handed • Adaptive • Private", style=MaterialTheme.typography.bodyMedium)
        Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(14.dp)){ Text("Your favorites stay stable. Context suggestions appear in a separate card."); Text("Swipe the alphabet rail on the right to jump.", style=MaterialTheme.typography.bodySmall) } }
        Button(onClick={ nav.navigate(Destinations.Home){ popUpTo(Destinations.Onboarding){ inclusive=true } } }){ Text("Get started")}
        OutlinedButton(onClick={ nav.navigate(Destinations.Settings)}){ Text("Open settings")}
    }
}
