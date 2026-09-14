package com.adaptive.launcher.domain.gestures

sealed interface LauncherGesture {
    data object SwipeUp : LauncherGesture
    data object SwipeDown : LauncherGesture
    data object SwipeLeft : LauncherGesture
    data object SwipeRight : LauncherGesture
    data object DoubleTap : LauncherGesture
    data object LongPress : LauncherGesture
    companion object { val all = listOf(SwipeUp, SwipeDown, SwipeLeft, SwipeRight, DoubleTap, LongPress) }
}

sealed interface LauncherAction {
    data object None : LauncherAction
    data object OpenSearch : LauncherAction
    data object OpenAppList : LauncherAction
    data object OpenSettings : LauncherAction
    data object OpenNotifications : LauncherAction
    data class LaunchApp(val packageName: String, val activityName: String) : LauncherAction
    companion object {
        fun displayName(a: LauncherAction): String = when(a){
            None -> "None"; OpenSearch -> "Open search"; OpenAppList -> "Open app list"
            OpenSettings -> "Open settings"; OpenNotifications -> "Open notifications"
            is LaunchApp -> "Launch ${a.packageName}"
        }
    }
}
