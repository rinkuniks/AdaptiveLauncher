package com.adaptive.launcher.data.favorites

import androidx.room.Database
import com.adaptive.launcher.data.streams.StreamAppCrossRef
import com.adaptive.launcher.data.streams.StreamDao
import com.adaptive.launcher.data.streams.StreamEntity
import com.adaptive.launcher.data.apps.AppMetaDao
import com.adaptive.launcher.data.apps.AppMetaEntity
import com.adaptive.launcher.data.screentime.AppUsageDao
import com.adaptive.launcher.data.screentime.AppUsageEntity
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class, StreamEntity::class, StreamAppCrossRef::class, AppMetaEntity::class, AppUsageEntity::class],
    version = 3, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun streamDao(): StreamDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun appUsageDao(): AppUsageDao
}
