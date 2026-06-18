package com.autoreply.bot.domain

import com.autoreply.bot.domain.model.AutoReplySettings
import com.autoreply.bot.domain.model.MatchType
import com.autoreply.bot.domain.model.ReplyScope
import com.autoreply.bot.domain.model.Rule
import java.util.Calendar

/**
 * Logica pura (sin dependencias de Android) para decidir que responder.
 * Al ser pura, es facil de probar y reutilizar.
 */
object ReplyEngine {

    /** Resultado de evaluar un mensaje: que texto enviar y por que regla. */
    data class Decision(
        val text: String,
        /** Regla que coincidio, o null si es el mensaje de ausencia global. */
        val rule: Rule?
    )

    /**
     * Decide la respuesta para un mensaje entrante.
     *
     * @param message texto recibido
     * @param rules reglas habilitadas, ya ordenadas por prioridad
     * @param settings ajustes actuales
     * @param isGroup si la conversacion es un grupo
     * @param groupId ID del grupo en la base de datos local (null si no se conoce aun)
     * @return la decision (texto + regla), o null si no se debe responder
     */
    fun decideReply(
        message: String,
        rules: List<Rule>,
        settings: AutoReplySettings,
        isGroup: Boolean,
        groupId: Long? = null
    ): Decision? {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return null

        // 1) Primera regla que coincida en texto, alcance (grupo/individual) y filtro de grupo.
        val matched = rules.firstOrNull {
            it.enabled && scopeMatches(it.scope, isGroup) && groupAllowed(it, isGroup, groupId) && matches(trimmed, it)
        }
        if (matched != null) return Decision(matched.response, matched)

        // 2) Mensaje de ausencia global (si esta activo).
        if (settings.awayMessageEnabled && settings.awayMessage.isNotBlank()) {
            return Decision(settings.awayMessage, null)
        }

        return null
    }

    /**
     * Comprueba si el grupo esta permitido por la regla.
     * - Si allowedGroupIds esta vacio, la regla aplica a todos (comportamiento actual).
     * - Si no es grupo, no se filtra (el scope ya controla eso).
     * - Si groupId es null (grupo no esta aun en la BD), se permite (no bloquear en primer contacto).
     */
    fun groupAllowed(rule: Rule, isGroup: Boolean, groupId: Long?): Boolean {
        if (rule.allowedGroupIds.isEmpty()) return true
        if (!isGroup) return true
        if (groupId == null) return true
        return groupId in rule.allowedGroupIds
    }

    /** Comprueba si el alcance de la regla aplica a este tipo de conversacion. */
    fun scopeMatches(scope: ReplyScope, isGroup: Boolean): Boolean = when (scope) {
        ReplyScope.ALL -> true
        ReplyScope.GROUPS_ONLY -> isGroup
        ReplyScope.INDIVIDUAL_ONLY -> !isGroup
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
