package com.autoreply.bot.data.repository

import com.autoreply.bot.data.local.KnownGroupDao
import com.autoreply.bot.data.local.toDomain
import com.autoreply.bot.data.local.toEntity
import com.autoreply.bot.domain.model.KnownGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KnownGroupRepository(private val dao: KnownGroupDao) {

    val groups: Flow<List<KnownGroup>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun upsert(group: KnownGroup) {
        dao.upsert(group.toEntity())
    }

    suspend fun getByConversationKey(key: String): KnownGroup? {
        return dao.getByConversationKey(key)?.toDomain()
    }

    suspend fun pruneOld(maxAgeMillis: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        dao.deleteOlderThan(cutoff)
    }
}
