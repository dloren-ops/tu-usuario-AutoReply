package com.autoreply.bot.domain.model

/**
 * Con que frecuencia responde una regla a una misma conversacion.
 */
enum class ReplyFrequency {
    /** Responde cada vez que entra un mensaje que coincide. */
    ALWAYS,

    /** Responde UNA sola vez por conversacion, aunque sigan llegando mensajes. */
    ONCE,

    /** Responde como mucho una vez cada cierto numero de horas por conversacion. */
    EVERY_HOURS;

    val label: String
        get() = when (this) {
            ALWAYS -> "Siempre"
            ONCE -> "Una sola vez"
            EVERY_HOURS -> "Cada ciertas horas"
        }
}
