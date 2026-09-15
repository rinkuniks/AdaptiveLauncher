package com.adaptive.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adaptive.launcher.feature.home.HomeRoute
import com.adaptive.launcher.feature.settings.SettingsRoute
import com.adaptive.launcher.feature.onboarding.OnboardingRoute
import com.adaptive.launcher.feature.screentime.ScreenTimeRoute
import com.adaptive.launcher.feature.streams.StreamDetailRoute

object Destinations{
    const val Home = "home"
    const val Settings = "settings"
    const val Onboarding = "onboarding"
    const val ScreenTime = "screentime"
    const val StreamDetail = "stream/{id}"
    fun stream(id: String) = "stream/$id"
}

@Composable
fun AppNavHost(start: String = Destinations.Home){
    val nav = rememberNavController()
    NavHost(navController=nav, startDestination=start){
        composable(Destinations.Home){ HomeRoute(onOpenSettings={ nav.navigate(Destinations.Settings)}, onOpenOnboarding={ nav.navigate(Destinations.Onboarding)}, onOpenStream={ id -> nav.navigate(Destinations.stream(id)) }, onOpenScreenTime={ nav.navigate(Destinations.ScreenTime) }) }
        composable(Destinations.Settings){ SettingsRoute(nav) }
        composable(Destinations.Onboarding){ OnboardingRoute(nav) }
        composable(Destinations.ScreenTime){ ScreenTimeRoute(nav) }
        composable(Destinations.StreamDetail){ backStack ->
            val id = backStack.arguments?.getString("id") ?: ""
            StreamDetailRoute(nav, id)
        }
    }
}
