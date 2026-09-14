package com.adaptive.launcher.domain.context

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.usageStore by preferencesDataStore(name="usage")

@Singleton
class UsageRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    suspend fun recordLaunch(packageName: String){
        ctx.usageStore.edit{ p ->
            p[longPreferencesKey(packageName+"_last")] = System.currentTimeMillis()
            val c = p[intPreferencesKey(packageName+"_count")] ?: 0
            p[intPreferencesKey(packageName+"_count")] = c+1
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)/3 // 0-7 buckets
            p[intPreferencesKey(packageName+"_hour")] = hour
            p[intPreferencesKey(packageName+"_day")] = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        }
    }
    fun signals(): Flow<List<ContextSignal>> = ctx.usageStore.data.map{ prefs ->
        prefs.asMap().keys.mapNotNull{ k ->
            val name = (k as? androidx.datastore.preferences.core.Preferences.Key<*>)?.name ?: return@mapNotNull null
            if(!name.endsWith("_last")) return@mapNotNull null
            val pkg = name.removeSuffix("_last")
            ContextSignal(pkg, prefs[longPreferencesKey(pkg+"_last")]?:0L, prefs[intPreferencesKey(pkg+"_count")]?:0, prefs[intPreferencesKey(pkg+"_hour")]?:-1, prefs[intPreferencesKey(pkg+"_day")]?:-1)
        }
    }
}
