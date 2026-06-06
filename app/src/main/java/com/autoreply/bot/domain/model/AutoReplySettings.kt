package com.autoreply.bot.domain.model

/**
 * Ajustes globales de la app de respuestas automaticas.
 */
data class AutoReplySettings(
    /** Interruptor maestro: activa/desactiva todas las respuestas. */
    val masterEnabled: Boolean = false,

    /** Si se respeta un horario de actividad. */
    val scheduleEnabled: Boolean = false,
    /** Hora de inicio en minutos desde medianoche (0..1439). */
    val startMinutes: Int = 0,
    /** Hora de fin en minutos desde medianoche (0..1439). */
    val endMinutes: Int = 1439,
    /** Dias activos (1=Lunes .. 7=Domingo, ISO-8601). */
    val activeDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),

    /** Si se responde tambien a chats de grupo. */
    val replyToGroups: Boolean = false,

    /** Tiempo minimo (segundos) entre respuestas al mismo contacto. Anti-spam. */
    val cooldownSeconds: Int = 60,

    /** Si se usa el mensaje de ausencia cuando ninguna regla coincide. */
    val awayMessageEnabled: Boolean = true,
    /** Texto del mensaje de ausencia. */
    val awayMessage: String = "Hola, en este momento no estoy disponible. Te respondere lo antes posible.",

    /** Paquetes de apps excluidos (no se responden). Vacio = responder a todas. */
    val excludedPackages: Set<String> = emptySet(),

    /**
     * Si esta activo, SOLO se responde a las apps de [allowedPackages].
     * Si esta desactivado, se responde a todas (salvo las excluidas).
     */
    val restrictToApps: Boolean = false,
    /** Paquetes de apps a los que SI se responde cuando [restrictToApps] es true. */
    val allowedPackages: Set<String> = emptySet()
)
