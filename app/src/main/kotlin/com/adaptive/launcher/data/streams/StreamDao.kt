package com.adaptive.launcher.data.streams

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao{
    @Query("SELECT * FROM streams ORDER BY position ASC") fun observeStreams(): Flow<List<StreamEntity>>
    @Query("SELECT * FROM streams ORDER BY position ASC") suspend fun getStreams(): List<StreamEntity>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(s: StreamEntity)
    @Query("DELETE FROM streams WHERE id=:id") suspend fun delete(id:String)
    @Query("SELECT * FROM stream_apps WHERE streamId=:streamId") fun observeApps(streamId:String): Flow<List<StreamAppCrossRef>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addApp(r: StreamAppCrossRef)
    @Query("DELETE FROM stream_apps WHERE streamId=:streamId AND packageName=:pkg") suspend fun removeApp(streamId:String, pkg:String)
}
