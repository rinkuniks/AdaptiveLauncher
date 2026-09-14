package com.adaptive.launcher.data.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String, // packageName#serial
    val packageName: String,
    val activityName: String,
    val position: Int,
    val userSerial: Long
)
