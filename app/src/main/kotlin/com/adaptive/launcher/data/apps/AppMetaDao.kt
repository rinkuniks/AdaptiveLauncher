package com.adaptive.launcher.data.apps

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppMetaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(app: AppMetaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AppMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppMetaEntity>)

    @Query("SELECT * FROM app_meta WHERE packageId = :packageId LIMIT 1")
    suspend fun getByPackageId(packageId: String): AppMetaEntity?

    @Query("UPDATE app_meta SET isHidden = :isHidden WHERE packageId = :packageId")
    suspend fun updateIsHidden(packageId: String, isHidden: Boolean)

    @Query("UPDATE app_meta SET isChallenge = :isChallenge WHERE packageId = :packageId")
    suspend fun updateIsChallenge(packageId: String, isChallenge: Boolean)

    @Query("SELECT packageId FROM app_meta WHERE isHidden = 1 ORDER BY COALESCE(displayName, packageId) COLLATE NOCASE ASC")
    fun getHiddenPackageIdsFlow(): Flow<List<String>>

    @Query("SELECT packageId FROM app_meta WHERE isChallenge = 1 ORDER BY COALESCE(displayName, packageId) COLLATE NOCASE ASC")
    fun getChallengePackageIdsFlow(): Flow<List<String>>

    @Query("SELECT packageId FROM app_meta WHERE isHidden = 1 ORDER BY COALESCE(displayName, packageId) COLLATE NOCASE ASC")
    suspend fun getHiddenPackageIds(): List<String>

    @Query("SELECT packageId FROM app_meta WHERE isChallenge = 1 ORDER BY COALESCE(displayName, packageId) COLLATE NOCASE ASC")
    suspend fun getChallengePackageIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM app_meta WHERE packageId = :packageId AND isHidden = 1)")
    suspend fun isHidden(packageId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM app_meta WHERE packageId = :packageId AND isChallenge = 1)")
    suspend fun isChallenge(packageId: String): Boolean

    @Transaction
    suspend fun setIsHidden(packageId: String, isHidden: Boolean) {
        ensureRowExists(packageId)
        updateIsHidden(packageId, isHidden)
    }

    @Transaction
    suspend fun setIsChallenge(packageId: String, isChallenge: Boolean) {
        ensureRowExists(packageId)
        updateIsChallenge(packageId, isChallenge)
    }

    @Transaction
    suspend fun ensureRowExists(packageId: String) {
        insertIfMissing(AppMetaEntity(packageId = packageId))
    }

    @Query("DELETE FROM app_meta WHERE packageId = :packageId")
    suspend fun deleteByPackageId(packageId: String)

    @Query("DELETE FROM app_meta WHERE displayName IS NULL AND isHidden = 0 AND isChallenge = 0")
    suspend fun purgeEmpty(): Int

    @Query("DELETE FROM app_meta WHERE packageId NOT IN (:keep) AND isHidden = 0 AND isChallenge = 0")
    suspend fun purgeNotIn(keep: List<String>): Int
}
