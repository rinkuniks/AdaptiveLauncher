package com.adaptive.launcher.data.apps

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AppMetaRepository {
    fun getHiddenPackageIdsFlow(): Flow<List<String>>
    fun getChallengePackageIdsFlow(): Flow<List<String>>
    suspend fun isHidden(packageId: String): Boolean
    suspend fun isChallenge(packageId: String): Boolean
    suspend fun setHidden(packageId: String, isHidden: Boolean)
    suspend fun setChallenge(packageId: String, isChallenge: Boolean)
    suspend fun getHiddenPackageIds(): List<String>
    suspend fun getChallengePackageIds(): List<String>
    suspend fun purgeNotIn(keep: List<String>)
    suspend fun purgeEmpty(): Int
}

@Singleton
class AppMetaRepositoryImpl @Inject constructor(private val dao: AppMetaDao) : AppMetaRepository {
    override fun getHiddenPackageIdsFlow(): Flow<List<String>> = dao.getHiddenPackageIdsFlow()
    override fun getChallengePackageIdsFlow(): Flow<List<String>> = dao.getChallengePackageIdsFlow()
    override suspend fun isHidden(packageId: String): Boolean = dao.isHidden(packageId)
    override suspend fun isChallenge(packageId: String): Boolean = dao.isChallenge(packageId)
    override suspend fun setHidden(packageId: String, isHidden: Boolean) = dao.setIsHidden(packageId, isHidden)
    override suspend fun setChallenge(packageId: String, isChallenge: Boolean) = dao.setIsChallenge(packageId, isChallenge)
    override suspend fun getHiddenPackageIds(): List<String> = dao.getHiddenPackageIds()
    override suspend fun getChallengePackageIds(): List<String> = dao.getChallengePackageIds()
    override suspend fun purgeNotIn(keep: List<String>) { if (keep.isNotEmpty()) dao.purgeNotIn(keep) else dao.purgeEmpty() }
    override suspend fun purgeEmpty(): Int = dao.purgeEmpty()
}
