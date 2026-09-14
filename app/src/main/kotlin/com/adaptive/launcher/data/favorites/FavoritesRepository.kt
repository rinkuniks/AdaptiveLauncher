package com.adaptive.launcher.data.favorites

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dao: FavoriteDao
) {
    val favorites: Flow<List<FavoriteEntity>> = dao.observeAll()

    suspend fun pin(packageName: String, activityName: String, userSerial: Long) {
        val all = dao.getAll()
        if (all.any { it.packageName == packageName && it.userSerial == userSerial }) return
        val pos = (all.maxOfOrNull { it.position } ?: -1) + 1
        val id = "${packageName}#${userSerial}"
        dao.upsert(FavoriteEntity(id, packageName, activityName, pos, userSerial))
    }

    suspend fun unpin(id: String) = dao.deleteById(id)

    suspend fun reorder(orderedIds: List<String>) {
        val all = dao.getAll().associateBy { it.id }
        orderedIds.forEachIndexed { idx, id ->
            all[id]?.let { dao.upsert(it.copy(position = idx)) }
        }
    }
}
