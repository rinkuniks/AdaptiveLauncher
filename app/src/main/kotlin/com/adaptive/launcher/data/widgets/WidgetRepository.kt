package com.adaptive.launcher.data.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, 0xCAFE)
    fun host() = host
    fun manager() = manager
    fun startListening(){ try{ host.startListening()}catch(_:Exception){} }
    fun stopListening(){ try{ host.stopListening()}catch(_:Exception){} }
    fun allocateId(): Int = host.allocateAppWidgetId()
    fun deleteId(id: Int){ try{ host.deleteAppWidgetId(id)}catch(_:Exception){} }
}
