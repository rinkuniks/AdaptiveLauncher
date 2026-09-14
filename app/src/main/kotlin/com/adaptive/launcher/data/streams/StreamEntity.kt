package com.adaptive.launcher.data.streams

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="streams")
data class StreamEntity(@PrimaryKey val id: String, val name: String, val position: Int, val icon: String = "")

@Entity(tableName="stream_apps", primaryKeys=["streamId","packageName"])
data class StreamAppCrossRef(val streamId: String, val packageName: String, val activityName: String, val userSerial: Long)
