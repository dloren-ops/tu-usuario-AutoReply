package com.autoreply.bot.data.repository

import com.autoreply.bot.data.local.ReplyStateDao
import com.autoreply.bot.data.local.ReplyStateEntity

/**
 * Guarda y consulta cuando respondio cada regla a cada conversacion.
 */
class ReplyStateRepository(private val dao: ReplyStateDao) {

    suspend fun lastReplyAt(key: String): Long? = dao.getLastReplyAt(key)

    suspend fun getAll(): List<ReplyStateEntity> = dao.getAll()

    suspend fun markReplied(key: String, timeMillis: Long) {
        dao.upsert(ReplyStateEntity(key = key, lastReplyAt = timeMillis))
    }

    suspend fun clear() = dao.clear()

    /** Reinicia el estado de una regla para que vuelva a responder desde cero. */
    suspend fun resetForRule(ruleId: Long) {
        dao.clearForRulePrefix("rule:$ruleId|")
    }
}
