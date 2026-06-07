package com.autoreply.bot.service

import com.autoreply.bot.domain.model.ReplyFrequency
import com.autoreply.bot.domain.model.Rule
import java.util.concurrent.ConcurrentHashMap

/**
 * Guarda de respuestas a nivel de PROCESO (estatico).
 *
 * Es un singleton para que el estado se comparta entre TODAS las instancias
 * del NotificationListenerService. Android a veces mantiene dos instancias del
 * servicio vivas a la vez (tipico tras actualizar/reinstalar), y si cada una
 * tuviera su propio estado en memoria, ambas responderian -> mensajes duplicados.
 *
 * El de-duplicado principal NO se basa en tiempo ni en texto, sino en la MARCA
 * DE TIEMPO del mensaje entrante (timestamp). Cuando la app de mensajeria
 * actualiza/re-publica la notificacion del mismo mensaje (por ejemplo al enviar
 * nuestra respuesta, o si tarda en marcarse como enviada), el timestamp del
 * mensaje entrante NO cambia. Por eso solo respondemos cuando llega un mensaje
 * con timestamp ESTRICTAMENTE mayor al ultimo al que ya respondimos en ese chat.
 */
object ReplyGuard {

    /** Timestamp del ultimo mensaje entrante al que YA respondimos, por conversacion. */
    private val lastRepliedStamp = ConcurrentHashMap<String, Long>()

    /** Momento (reloj) de la ultima respuesta, por conversacion (respaldo anti-rafaga). */
    private val lastReplyTime = ConcurrentHashMap<String, Long>()

    /** Momento de la ultima respuesta, por contacto (cooldown configurable). */
    private val lastReplyByContact = ConcurrentHashMap<String, Long>()

    /** Estado de frecuencia por regla+conversacion. */
    private val freqState = ConcurrentHashMap<String, Long>()

    /**
     * Ventana dura minima entre respuestas a la MISMA conversacion. Solo se usa
     * como respaldo cuando no hay timestamp de mensaje disponible.
     */
    private const val HARD_DEDUP_WINDOW_MS = 10_000L

    /** Precarga el estado de frecuencia persistido (al conectar el servicio). */
    fun preloadFrequency(entries: Map<String, Long>) {
        freqState.putAll(entries)
    }

    /**
     * Decide si se debe responder AHORA y, si si, reserva el turno de inmediato.
     *
     * @param messageStamp marca de tiempo del mensaje entrante (0 si se desconoce)
     * @return true si se autoriza responder (y ya quedo reservado).
     */
    @Synchronized
    fun tryReserve(
        conversationKey: String,
        contactKey: String,
        messageStamp: Long,
        rule: Rule?,
        cooldownMillis: Long,
        now: Long
    ): Boolean {
        // 1) De-duplicado por IDENTIDAD del mensaje (lo principal).
        // Solo respondemos a un mensaje entrante mas nuevo que el ultimo
        // respondido. Re-publicaciones del mismo mensaje tienen el mismo stamp.
        if (messageStamp > 0L) {
            val lastStamp = lastRepliedStamp[conversationKey]
            if (lastStamp != null && messageStamp <= lastStamp) return false
        } else {
            // Respaldo: si no hay stamp, usar ventana de tiempo dura.
            val lastConv = lastReplyTime[conversationKey]
            if (lastConv != null && now - lastConv < HARD_DEDUP_WINDOW_MS) return false
        }

        // 2) Cooldown configurable por contacto (anti-spam del usuario).
        val lastContact = lastReplyByContact[contactKey]
        if (lastContact != null && now - lastContact < cooldownMillis) return false

        // 3) Frecuencia por regla (una vez / cada X horas) por conversacion.
        val freqKey = freqKey(rule, conversationKey)
        if (rule != null && rule.frequency != ReplyFrequency.ALWAYS && freqKey != null) {
            val last = freqState[freqKey]
            if (last != null) {
                val allowed = when (rule.frequency) {
                    ReplyFrequency.ONCE -> false
                    ReplyFrequency.EVERY_HOURS -> now - last >= rule.everyHours * 3_600_000L
                    ReplyFrequency.ALWAYS -> true
                }
                if (!allowed) return false
            }
        }

        // Reservar el turno YA (antes de enviar).
        if (messageStamp > 0L) lastRepliedStamp[conversationKey] = messageStamp
        lastReplyTime[conversationKey] = now
        lastReplyByContact[contactKey] = now
        if (rule != null && rule.frequency != ReplyFrequency.ALWAYS && freqKey != null) {
            freqState[freqKey] = now
        }

        pruneIfNeeded(now)
        return true
    }

    /**
     * Revierte la reserva si el envio fallo (para permitir reintento).
     * Nota: no revertimos [lastRepliedStamp] a proposito; si el envio fallo por
     * red, no queremos spamear reintentos del mismo mensaje.
     */
    @Synchronized
    fun rollback(conversationKey: String, contactKey: String, rule: Rule?) {
        lastReplyTime.remove(conversationKey)
        lastReplyByContact.remove(contactKey)
        val freqKey = freqKey(rule, conversationKey)
        if (freqKey != null) freqState.remove(freqKey)
    }

    fun freqKey(rule: Rule?, conversationKey: String): String? =
        if (rule != null) "rule:${rule.id}|$conversationKey" else null

    private fun pruneIfNeeded(now: Long) {
        if (lastReplyTime.size > 300) {
            lastReplyTime.entries.removeAll { now - it.value > 3_600_000L }
        }
        if (lastReplyByContact.size > 300) {
            lastReplyByContact.entries.removeAll { now - it.value > 3_600_000L }
        }
        if (lastRepliedStamp.size > 500) {
            // Conserva solo las 250 conversaciones mas recientes por reloj.
            val cutoff = lastReplyTime.values.sortedDescending().getOrNull(250) ?: return
            lastRepliedStamp.keys.retainAll { (lastReplyTime[it] ?: 0L) >= cutoff }
        }
    }
}
