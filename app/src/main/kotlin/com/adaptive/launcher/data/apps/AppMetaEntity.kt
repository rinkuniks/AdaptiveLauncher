package com.adaptive.launcher.data.apps

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val packageId: String,
    val displayName: String? = null,
    val isHidden: Boolean = false,
    val isChallenge: Boolean = false
)
