package com.adaptive.launcher.domain.widgets

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.adaptive.launcher.data.widgets.WidgetRepository
import javax.inject.Inject

class WidgetHostHelper @Inject constructor(private val repo: WidgetRepository) {
    fun createView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView {
        val host = repo.host()
        val view = host.createView(context, appWidgetId, info)
        view.setAppWidget(appWidgetId, info)
        return view
    }
}
