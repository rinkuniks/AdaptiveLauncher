package com.adaptive.launcher.domain

import android.os.UserHandle
import io.mockk.mockk
import com.adaptive.launcher.core.model.LauncherApp
import com.adaptive.launcher.data.home.AppListFilter
import com.adaptive.launcher.data.home.HomeMode
import com.adaptive.launcher.data.home.HomePrefsRepository
import org.junit.Assert.*
import org.junit.Test

class HomeFilteringLogicTest {
    private fun app(pkg:String, label:String, isSystem:Boolean): LauncherApp {
        val n = label.lowercase()
        val sec = if(n.firstOrNull() in 'a'..'z') n.first().uppercaseChar() else '#'
        return LauncherApp(pkg, "$pkg.Main", label, n, testUser(), sec, isSystemApp=isSystem)
    }
    private fun testUser(): UserHandle = mockk(relaxed = true)

    @Test fun installed_excludesSystem(){
        val apps = listOf(app("com.chrome","Chrome",false), app("com.android.settings","Settings",true))
        val filtered = when(AppListFilter.Installed){ AppListFilter.Installed -> apps.filter{ !it.isSystemApp }; AppListFilter.System -> apps.filter{ it.isSystemApp }; else -> apps }
        assertEquals(1, filtered.size)
        assertEquals("Chrome", filtered.first().label)
    }

    @Test fun system_onlySystem(){
        val apps = listOf(app("com.chrome","Chrome",false), app("com.android.settings","Settings",true))
        val filtered = when(AppListFilter.System){ AppListFilter.Installed -> apps.filter{ !it.isSystemApp }; AppListFilter.System -> apps.filter{ it.isSystemApp }; else -> apps }
        assertEquals(1, filtered.size)
        assertEquals("Settings", filtered.first().label)
    }

    @Test fun autoMajor_keepsMessagingAndDialer(){
        val apps = listOf(
            app("com.google.android.dialer","Phone",true),
            app("com.android.settings","Settings",true),
            app("com.whatsapp","WhatsApp",false),
        )
        val homeVisible = apps.filter{ !it.isSystemApp || it.packageName in HomePrefsRepository.MajorPackages }
        assertTrue(homeVisible.any{ it.packageName=="com.google.android.dialer"})
        assertTrue(homeVisible.any{ it.packageName=="com.whatsapp"})
        assertFalse(homeVisible.any{ it.packageName=="com.android.settings"})
    }

    @Test fun custom_onlyChosen(){
        val apps = listOf(app("com.a","A",false), app("com.b","B",false), app("com.c","C",false))
        val chosen = setOf("com.a","com.c")
        val visible = apps.filter{ it.packageName in chosen }
        assertEquals(2, visible.size)
    }

    @Test fun showAll_returnsAll(){
        val apps = listOf(app("com.a","A",false), app("com.b","B",true))
        val mode = HomeMode.ShowAll
        val visible = when(mode){ HomeMode.ShowAll->apps; HomeMode.AutoMajor->apps.filter{ !it.isSystemApp}; HomeMode.Custom->apps.filter{ it.packageName in emptySet<String>()} }
        assertEquals(2, visible.size)
    }
}
