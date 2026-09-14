package com.adaptive.launcher.data.favorites

import androidx.room.Database
import com.adaptive.launcher.data.streams.StreamAppCrossRef
import com.adaptive.launcher.data.streams.StreamDao
import com.adaptive.launcher.data.streams.StreamEntity
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class, StreamEntity::class, StreamAppCrossRef::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun streamDao(): StreamDao
}
