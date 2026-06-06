package com.autoreply.bot.domain.model

/**
 * Tipo de coincidencia de una regla con el mensaje entrante.
 */
enum class MatchType {
    /** El mensaje contiene la palabra clave en cualquier parte. */
    CONTAINS,

    /** El mensaje es exactamente igual a la palabra clave. */
    EXACT,

    /** El mensaje empieza con la palabra clave. */
    STARTS_WITH,

    /** Coincide con cualquier mensaje (regla comodin / por defecto). */
    ANY;

    val label: String
        get() = when (this) {
            CONTAINS -> "Contiene"
            EXACT -> "Exacto"
            STARTS_WITH -> "Empieza con"
            ANY -> "Cualquier mensaje"
        }
}
