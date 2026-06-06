package com.autoreply.bot.data.repository

import com.autoreply.bot.data.local.LogDao
import com.autoreply.bot.data.local.toDomain
import com.autoreply.bot.data.local.toEntity
import com.autoreply.bot.domain.model.ReplyLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepository(private val dao: LogDao) {

    val recentLogs: Flow<List<ReplyLog>> =
        dao.observeRecent().map { list -> list.map { it.toDomain() } }

    suspend fun add(log: ReplyLog) {
        dao.insert(log.toEntity())
        dao.trim()
    }

    suspend fun clear() = dao.clear()
}
