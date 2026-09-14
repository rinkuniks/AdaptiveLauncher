package com.adaptive.launcher.domain.search

import com.adaptive.launcher.core.model.LauncherApp
import javax.inject.Inject

class AppSearchProvider @Inject constructor() : SearchProvider {
    override suspend fun search(query: String, apps: List<LauncherApp>): List<SearchProvider.ScoredApp> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val qNoSpace = q.replace(" ", "")
        return apps.mapNotNull { app ->
            val score = scoreApp(q, qNoSpace, app) ?: return@mapNotNull null
            SearchProvider.ScoredApp(app, score)
        }.sortedByDescending { it.score }.take(20)
    }

    private fun scoreApp(q: String, qNoSpace: String, app: LauncherApp): Float? {
        val n = app.normalizedLabel
        val labelLower = app.label.lowercase()
        // exact prefix
        if (n.startsWith(q)) return 100f - n.length * 0.1f
        if (labelLower.startsWith(q)) return 95f
        // substring
        val idx = n.indexOf(q)
        if (idx >= 0) return 80f - idx * 2f
        // all chars in order (fuzzy)
        if (isSubsequence(qNoSpace, n)) return 60f - qNoSpace.length * 0.5f
        // acronym: e.g. yt -> youtube
        if (q.length <= 4 && isAcronym(q, app.label)) return 55f
        // token prefix
        val tokens = n.split(Regex("[^a-z0-9]+"))
        if (tokens.any { it.startsWith(q) }) return 70f
        return null
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var i = 0
        for (c in haystack) {
            if (i < needle.length && c == needle[i]) i++
            if (i == needle.length) return true
        }
        return false
    }

    private fun isAcronym(q: String, label: String): Boolean {
        val words = label.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        if (words.size < q.length) return false
        // first letters match
        val acr = words.map { it.first().lowercaseChar() }.joinToString("")
        if (acr.startsWith(q)) return true
        // camelCase boundaries
        val caps = label.filter { it.isUpperCase() }.map { it.lowercaseChar() }.joinToString("")
        return caps.contains(q)
    }
}
