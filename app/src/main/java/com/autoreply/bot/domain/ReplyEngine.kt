package com.autoreply.bot.domain

import com.autoreply.bot.domain.model.AutoReplySettings
import com.autoreply.bot.domain.model.MatchType
import com.autoreply.bot.domain.model.Rule
import java.util.Calendar

/**
 * Logica pura (sin dependencias de Android) para decidir que responder.
 * Al ser pura, es facil de probar y reutilizar.
 */
object ReplyEngine {

    /**
     * Decide la respuesta para un mensaje entrante.
     *
     * @param message texto recibido
     * @param rules reglas habilitadas, ya ordenadas por prioridad
     * @param settings ajustes actuales
     * @return el texto a responder, o null si no se debe responder
     */
    fun decideReply(
        message: String,
        rules: List<Rule>,
        settings: AutoReplySettings
    ): String? {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return null

        // 1) Buscar la primera regla que coincida.
        val matched = rules.firstOrNull { it.enabled && matches(trimmed, it) }
        if (matched != null) return matched.response

        // 2) Si ninguna regla coincide, usar el mensaje de ausencia (si esta activo).
        if (settings.awayMessageEnabled && settings.awayMessage.isNotBlank()) {
            return settings.awayMessage
        }

        return null
    }

    /** Comprueba si un mensaje coincide con una regla, ignorando mayusculas. */
    fun matches(message: String, rule: Rule): Boolean {
        val msg = message.trim().lowercase()
        val key = rule.keyword.trim().lowercase()
        return when (rule.matchType) {
            MatchType.ANY -> true
            MatchType.CONTAINS -> key.isNotEmpty() && msg.contains(key)
            MatchType.EXACT -> msg == key
            MatchType.STARTS_WITH -> key.isNotEmpty() && msg.startsWith(key)
        }
    }

    /**
     * Indica si en el instante [timeMillis] esta dentro de la ventana de actividad
     * configurada. Si el horario esta desactivado, siempre es true.
     * Soporta ventanas que cruzan la medianoche (ej. 22:00 a 06:00).
     */
    fun isWithinSchedule(settings: AutoReplySettings, timeMillis: Long): Boolean {
        if (!settings.scheduleEnabled) return true

        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        // Convertir Calendar.DAY_OF_WEEK (Dom=1..Sab=7) a ISO (Lun=1..Dom=7).
        val isoDay = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        if (isoDay !in settings.activeDays) return false

        val minutesNow = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = settings.startMinutes
        val end = settings.endMinutes

        return if (start <= end) {
            minutesNow in start..end
        } else {
            // Ventana que cruza medianoche.
            minutesNow >= start || minutesNow <= end
        }
    }
}
