package com.adaptive.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adaptive.launcher.feature.home.HomeRoute
import com.adaptive.launcher.feature.settings.SettingsRoute
import com.adaptive.launcher.feature.onboarding.OnboardingRoute

object Destinations{
    const val Home = "home"
    const val Settings = "settings"
    const val Onboarding = "onboarding"
}

@Composable
fun AppNavHost(start: String = Destinations.Home){
    val nav = rememberNavController()
    NavHost(navController=nav, startDestination=start){
        composable(Destinations.Home){ HomeRoute(onOpenSettings={ nav.navigate(Destinations.Settings)}, onOpenOnboarding={ nav.navigate(Destinations.Onboarding)}) }
        composable(Destinations.Settings){ SettingsRoute(nav) }
        composable(Destinations.Onboarding){ OnboardingRoute(nav) }
    }
}
