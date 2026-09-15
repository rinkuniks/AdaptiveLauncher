package com.adaptive.launcher.domain.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.adaptive.launcher.data.widgets.WidgetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetHostHelper @Inject constructor(private val repo: WidgetRepository) {

    fun host() = repo.host()
    fun manager(): AppWidgetManager = repo.manager()

    fun createView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView {
        val host = repo.host()
        val view = host.createView(context, appWidgetId, info)
        view.setAppWidget(appWidgetId, info)
        return view
    }

    fun providerInfo(appWidgetId: Int): AppWidgetProviderInfo? = try {
        repo.manager().getAppWidgetInfo(appWidgetId)
    } catch (_: Exception) { null }

    fun isBound(appWidgetId: Int): Boolean = providerInfo(appWidgetId) != null
}
