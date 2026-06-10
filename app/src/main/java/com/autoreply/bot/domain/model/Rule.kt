package com.autoreply.bot.domain.model

/**
 * Regla de respuesta automatica: si el mensaje coincide con [keyword] segun
 * [matchType], se responde con [response].
 */
data class Rule(
    val id: Long = 0,
    /** Nombre/titulo para identificar la regla facilmente (opcional). */
    val title: String = "",
    val keyword: String,
    val response: String,
    val matchType: MatchType = MatchType.CONTAINS,
    val enabled: Boolean = true,
    /** Prioridad: mayor valor se evalua primero. */
    val priority: Int = 0,
    /** A que conversaciones aplica: todas, solo grupos o solo individuales. */
    val scope: ReplyScope = ReplyScope.ALL,
    /** Con que frecuencia responde a una misma conversacion. */
    val frequency: ReplyFrequency = ReplyFrequency.ALWAYS,
    /** Horas de espera cuando [frequency] es EVERY_HOURS. */
    val everyHours: Int = 24,
    val createdAt: Long = System.currentTimeMillis()
)
