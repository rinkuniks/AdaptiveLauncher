package com.adaptive.launcher.data.backup

import android.content.Context
import com.adaptive.launcher.data.favorites.FavoriteDao
import com.adaptive.launcher.data.streams.StreamDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(@ApplicationContext private val ctx: Context, private val favoriteDao: FavoriteDao, private val streamDao: StreamDao){
    suspend fun export(): String {
        val favs = favoriteDao.getAll()
        val streams = streamDao.getStreams()
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("launcherVersion", "1.0.0")
        val favArr = JSONArray()
        favs.forEach{ f -> favArr.put(JSONObject().put("packageName", f.packageName).put("activityName", f.activityName).put("position", f.position).put("userSerial", f.userSerial)) }
        root.put("favorites", favArr)
        val streamArr = JSONArray()
        streams.forEach{ s ->
            val apps = try{ streamDao.observeApps(s.id).first() }catch(_:Exception){ emptyList() }
            val appArr = JSONArray()
            apps.forEach{ a -> appArr.put(JSONObject().put("packageName", a.packageName).put("activityName", a.activityName).put("userSerial", a.userSerial)) }
            streamArr.put(JSONObject().put("id", s.id).put("name", s.name).put("position", s.position).put("apps", appArr))
        }
        root.put("streams", streamArr)
        return root.toString(2)
    }
    suspend fun importBackup(jsonStr: String): Boolean = try{
        val root = JSONObject(jsonStr)
        val favArr = root.optJSONArray("favorites") ?: JSONArray()
        favoriteDao.clear()
        for(i in 0 until favArr.length()){
            val o = favArr.getJSONObject(i)
            favoriteDao.upsert(com.adaptive.launcher.data.favorites.FavoriteEntity(o.getString("packageName")+"#"+o.getLong("userSerial"), o.getString("packageName"), o.getString("activityName"), o.getInt("position"), o.getLong("userSerial")))
        }
        val streamArr = root.optJSONArray("streams") ?: JSONArray()
        for(i in 0 until streamArr.length()){
            val s = streamArr.getJSONObject(i)
            streamDao.upsert(com.adaptive.launcher.data.streams.StreamEntity(s.getString("id"), s.getString("name"), s.getInt("position")))
            val apps = s.optJSONArray("apps") ?: JSONArray()
            for(j in 0 until apps.length()){
                val a = apps.getJSONObject(j)
                streamDao.addApp(com.adaptive.launcher.data.streams.StreamAppCrossRef(s.getString("id"), a.getString("packageName"), a.getString("activityName"), a.getLong("userSerial")))
            }
        }
        true
    }catch(_:Exception){ false }
}
