package com.adaptive.launcher.data.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetStore by preferencesDataStore(name = "widgets")
private val IDS_KEY = stringSetPreferencesKey("widget_ids")

@Singleton
class WidgetRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, 0xCAFE)

    fun host() = host
    fun manager() = manager
    fun startListening() { try { host.startListening() } catch (_: Exception) {} }
    fun stopListening() { try { host.stopListening() } catch (_: Exception) {} }
    fun allocateId(): Int = try { host.allocateAppWidgetId() } catch (_: Exception) { -1 }
    fun deleteId(id: Int) { try { host.deleteAppWidgetId(id) } catch (_: Exception) {} }

    val widgetIds: Flow<List<Int>> = context.widgetStore.data.map { prefs ->
        val raw = prefs[IDS_KEY] ?: emptySet()
        raw.mapNotNull { it.toIntOrNull() }.sorted()
    }

    suspend fun addWidgetId(id: Int) {
        if (id < 0) return
        context.widgetStore.edit { prefs ->
            val cur = prefs[IDS_KEY] ?: emptySet()
            prefs[IDS_KEY] = cur + id.toString()
        }
    }

    suspend fun removeWidgetId(id: Int) {
        context.widgetStore.edit { prefs ->
            val cur = prefs[IDS_KEY] ?: emptySet()
            prefs[IDS_KEY] = cur - id.toString()
        }
        deleteId(id)
    }

    suspend fun clearAll() {
        val ids = try { widgetIds } catch (_: Exception) { null }
        context.widgetStore.edit { it[IDS_KEY] = emptySet() }
    }
}
