package com.autoreply.bot.domain.model

/**
 * Registro de una respuesta automatica enviada.
 */
data class ReplyLog(
    val id: Long = 0,
    val appPackage: String,
    val appLabel: String,
    val sender: String,
    val incomingMessage: String,
    val replyText: String,
    val timestamp: Long = System.currentTimeMillis()
)
