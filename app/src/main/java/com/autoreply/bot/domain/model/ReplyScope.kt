package com.autoreply.bot.domain.model

/**
 * A que tipo de conversaciones aplica una regla.
 */
enum class ReplyScope {
    /** Responde tanto a chats individuales como a grupos. */
    ALL,

    /** Responde SOLO en chats de grupo. */
    GROUPS_ONLY,

    /** Responde SOLO en chats individuales (uno a uno). */
    INDIVIDUAL_ONLY;

    val label: String
        get() = when (this) {
            ALL -> "Todos"
            GROUPS_ONLY -> "Solo grupos"
            INDIVIDUAL_ONLY -> "Solo individuales"
        }
}
