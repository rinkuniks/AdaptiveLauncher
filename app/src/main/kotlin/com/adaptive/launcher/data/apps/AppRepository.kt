package com.adaptive.launcher.data.apps

import com.adaptive.launcher.core.common.AppLabelNormalizer
import com.adaptive.launcher.core.model.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val provider: LauncherAppsProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps.asStateFlow()

    private val callback = object : LauncherAppsProvider.Callback {
        override fun onPackagesChanged() { refresh() }
    }

    init {
        provider.registerCallback(callback)
        refresh()
    }

    fun refresh() { scope.launch { load() } }

    private suspend fun load() = withContext(Dispatchers.IO) {
        val infos = try { provider.getLauncherActivities() } catch (_: Exception) { emptyList() }
        val mapped = infos.mapNotNull { info ->
            try {
                val label = info.label.toString()
                val normalized = AppLabelNormalizer.normalize(label)
                val section = AppLabelNormalizer.sectionFor(normalized)
                val isSystem = try { provider.isSystemApp(info.componentName.packageName) } catch(_:Exception){ false }
                LauncherApp(
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    label = label,
                    normalizedLabel = normalized,
                    user = info.user,
                    section = section,
                    isSystemApp = isSystem
                )
            } catch (_: Exception) { null }
        }.sortedWith(compareBy({ it.section == '#' }, { it.normalizedLabel }))
        _apps.value = mapped
    }

    fun launch(app: LauncherApp) { provider.startMainActivity(app.packageName, app.activityName, app.user) }
}
