package com.adaptive.launcher.domain.streams

import com.adaptive.launcher.data.streams.StreamAppCrossRef
import com.adaptive.launcher.data.streams.StreamDao
import com.adaptive.launcher.data.streams.StreamEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(private val dao: StreamDao){
    val streams: Flow<List<StreamEntity>> = dao.observeStreams()
    suspend fun create(name: String){ val id=UUID.randomUUID().toString(); val pos=(dao.getStreams().maxOfOrNull{it.position}?:-1)+1; dao.upsert(StreamEntity(id,name,pos)) }
    suspend fun delete(id:String)= dao.delete(id)
    fun apps(streamId:String): Flow<List<StreamAppCrossRef>> = dao.observeApps(streamId)
    suspend fun addApp(streamId:String, pkg:String, activity:String, serial:Long)= dao.addApp(StreamAppCrossRef(streamId,pkg,activity,serial))
    suspend fun removeApp(streamId:String, pkg:String)= dao.removeApp(streamId,pkg)
    suspend fun seedDefaults(){
        if(dao.getStreams().isNotEmpty()) return
        listOf("Work","Social","Travel","Finance","Entertainment","Health","Study").forEachIndexed{ i,n -> dao.upsert(StreamEntity(UUID.randomUUID().toString(), n, i)) }
    }
}
