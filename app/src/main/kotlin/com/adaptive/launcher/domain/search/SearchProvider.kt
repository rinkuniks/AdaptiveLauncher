package com.adaptive.launcher.domain.search

import com.adaptive.launcher.core.model.LauncherApp

interface SearchProvider {
    suspend fun search(query: String, apps: List<LauncherApp>): List<ScoredApp>
    data class ScoredApp(val app: LauncherApp, val score: Float)
}
