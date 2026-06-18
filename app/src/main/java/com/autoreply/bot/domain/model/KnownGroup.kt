package com.autoreply.bot.domain.model

/**
 * Grupo descubierto automaticamente a partir de notificaciones recibidas.
 * Se usa para mostrar al usuario una lista de grupos donde puede aplicar cada regla.
 */
data class KnownGroup(
    val id: Long = 0,
    val packageName: String,
    val groupName: String,
    val conversationKey: String,
    val lastSeenAt: Long,
    val communityParent: String? = null
)
