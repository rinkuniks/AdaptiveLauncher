package com.adaptive.launcher.domain

import com.adaptive.launcher.data.home.AppListFilter
import org.junit.Assert.*
import org.junit.Test

class AppListFilterTest {
    @Test fun allValues_present(){ assertEquals(3, AppListFilter.values().size) }
    @Test fun default_isInstalled(){ assertEquals(AppListFilter.Installed, AppListFilter.valueOf("Installed")) }
    @Test fun parseInvalid_defaults(){ val f = runCatching{ AppListFilter.valueOf("Bogus")}.getOrDefault(AppListFilter.Installed); assertEquals(AppListFilter.Installed, f) }
}
