package com.autoreply.bot.domain.model

/**
 * Regla de respuesta automatica: si el mensaje coincide con [keyword] segun
 * [matchType], se responde con [response].
 */
data class Rule(
    val id: Long = 0,
    val keyword: String,
    val response: String,
    val matchType: MatchType = MatchType.CONTAINS,
    val enabled: Boolean = true,
    /** Prioridad: mayor valor se evalua primero. */
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
