package com.adaptive.launcher.domain.gestures

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gestureDataStore by preferencesDataStore(name="gestures")

@Singleton
class GestureRepository @Inject constructor(@ApplicationContext private val ctx: Context){
    private fun key(g: LauncherGesture)= stringPreferencesKey("gesture_"+g::class.simpleName)
    fun observe(g: LauncherGesture): Flow<LauncherAction> = ctx.gestureDataStore.data.map{ prefs ->
        val v = prefs[key(g)] ?: "None"
        parse(v)
    }
    suspend fun set(g: LauncherGesture, a: LauncherAction){ ctx.gestureDataStore.edit{ it[key(g)] = serialize(a)} }
    fun defaults(): Map<LauncherGesture, LauncherAction> = mapOf(
        LauncherGesture.SwipeDown to LauncherAction.OpenSearch,
        LauncherGesture.SwipeUp to LauncherAction.OpenAppList,
        LauncherGesture.DoubleTap to LauncherAction.None
    )
    private fun serialize(a: LauncherAction): String = when(a){
        LauncherAction.None->"None"; LauncherAction.OpenSearch->"OpenSearch"; LauncherAction.OpenAppList->"OpenAppList"
        LauncherAction.OpenSettings->"OpenSettings"; LauncherAction.OpenNotifications->"OpenNotifications"
        is LauncherAction.LaunchApp->"LaunchApp:"+a.packageName+","+a.activityName
    }
    private fun parse(s:String): LauncherAction = when{
        s=="None"->LauncherAction.None; s=="OpenSearch"->LauncherAction.OpenSearch; s=="OpenAppList"->LauncherAction.OpenAppList
        s=="OpenSettings"->LauncherAction.OpenSettings; s=="OpenNotifications"->LauncherAction.OpenNotifications
        s.startsWith("LaunchApp:")-> { val p=s.removePrefix("LaunchApp:").split(","); if(p.size==2) LauncherAction.LaunchApp(p[0],p[1]) else LauncherAction.None }
        else->LauncherAction.None
    }
}
