package com.adaptive.launcher.domain

import android.os.UserHandle
import io.mockk.mockk
import com.adaptive.launcher.core.model.LauncherApp
import com.adaptive.launcher.domain.search.AppSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppSearchProviderTest {
    private val provider = AppSearchProvider()
    private fun testUser(): UserHandle = mockk(relaxed = true)
    private fun app(label: String, pkg: String="com.test.$label", isSystem:Boolean=false): LauncherApp {
        val n = label.lowercase()
        val sec = if(n.firstOrNull() in 'a'..'z') n.first().uppercaseChar() else '#'
        return LauncherApp(pkg, "$pkg.Main", label, n, testUser(), sec, isSystemApp=isSystem)
    }

    @Test fun prefix_scoresHigherThanSubstring() = runTest {
        val apps = listOf(app("Chrome"), app("Super Chrome Plus"), app("Firefox"))
        val results = provider.search("chr", apps)
        assertTrue(results.isNotEmpty())
        assertEquals("Chrome", results.first().app.label)
    }

    @Test fun substring_found() = runTest {
        val apps = listOf(app("Chrome"), app("Firefox"))
        val results = provider.search("rom", apps)
        assertTrue(results.any{ it.app.label=="Chrome"})
    }

    @Test fun acronym_yt() = runTest {
        val apps = listOf(app("YouTube"), app("YouTube Music"), app("Chrome"))
        val results = provider.search("yt", apps)
        assertTrue(results.any{ it.app.label=="YouTube"})
    }

    @Test fun empty_returnsEmpty() = runTest {
        val results = provider.search("", listOf(app("Chrome")))
        assertTrue(results.isEmpty())
    }

    @Test fun token_prefix() = runTest {
        val apps = listOf(app("Google Maps"), app("Gmail"))
        val results = provider.search("map", apps)
        assertTrue(results.any{ it.app.label=="Google Maps"})
    }

    @Test fun ranking_limit20() = runTest {
        val apps = (0..30).map{ app("App $it", "com.test.app$it") }
        val results = provider.search("app", apps)
        assertTrue(results.size <= 20)
    }

    @Test fun caseInsensitive() = runTest {
        val apps = listOf(app("Chrome"))
        assertTrue(provider.search("CHROME", apps).isNotEmpty())
        assertTrue(provider.search("chrome", apps).isNotEmpty())
    }
}
