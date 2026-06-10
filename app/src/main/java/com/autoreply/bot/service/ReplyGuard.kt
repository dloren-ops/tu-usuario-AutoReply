package com.autoreply.bot.service

import com.autoreply.bot.domain.model.ReplyFrequency
import com.autoreply.bot.domain.model.Rule
import java.util.concurrent.ConcurrentHashMap

/**
 * Guarda de respuestas a nivel de PROCESO (estatico), compartida por todas las
 * instancias del NotificationListenerService.
 *
 * DE-DUPLICADO POR HUELLA DEL MENSAJE
 * -----------------------------------
 * El problema de los duplicados venia de que, cuando WhatsApp ENVIA finalmente
 * nuestra respuesta (a veces con retraso por la red), ACTUALIZA/re-publica la
 * notificacion del chat. Eso re-dispara onNotificationPosted con el MISMO
 * mensaje entrante, y respondiamos de nuevo.
 *
 * Solucion: guardamos una "huella" del ultimo mensaje al que YA respondimos en
 * cada conversacion (texto + hora del mensaje). Si llega otra notificacion con
 * la MISMA huella (re-publicacion del mismo mensaje), NO respondemos. Solo
 * respondemos cuando la huella cambia (mensaje realmente nuevo).
 *
 * Esto no depende de cuanto tarde en enviarse la respuesta.
 */
object ReplyGuard {

    /** Huella del ultimo mensaje respondido, por conversacion. */
    private val lastRepliedSignature = ConcurrentHashMap<String, String>()

    /** Momento de la ultima respuesta, por conversacion (ventana dura de respaldo). */
    private val lastReplyTime = ConcurrentHashMap<String, Long>()

    /** Momento de la ultima respuesta, por contacto (cooldown configurable). */
    private val lastReplyByContact = ConcurrentHashMap<String, Long>()

    /** Estado de frecuencia por regla+conversacion. */
    private val freqState = ConcurrentHashMap<String, Long>()

    /**
     * Ventana dura minima entre respuestas a la MISMA conversacion. Atrapa
     * cualquier doble disparo aunque la huella fallara por algun motivo.
     */
    private const val HARD_DEDUP_WINDOW_MS = 10_000L

    /** Precarga el estado de frecuencia persistido (al conectar el servicio). */
    fun preloadFrequency(entries: Map<String, Long>) {
        freqState.putAll(entries)
    }

    /**
     * Reinicia SOLO el estado de frecuencia de una regla (los contadores de
     * "una vez" / "cada X horas") para que vuelva a responder tras reactivarla.
     *
     * IMPORTANTE: NO tocamos el anti-duplicados (lastRepliedSignature /
     * lastReplyTime). Esa memoria es la que evita responder 2 veces al MISMO
     * mensaje; si la borraramos, volverian los duplicados. Como esa proteccion
     * solo bloquea el mensaje identico mas reciente, no impide responder a
     * mensajes nuevos tras el reinicio.
     */
    @Synchronized
    fun resetRule(ruleId: Long) {
        val prefix = "rule:$ruleId|"
        freqState.keys.removeAll { it.startsWith(prefix) }
    }

    /**
     * Decide si se debe responder AHORA y, si si, reserva el turno de inmediato.
     *
     * @param signature huella estable del mensaje entrante (texto + hora)
     * @return true si se autoriza responder (y ya quedo reservado).
     */
    @Synchronized
    fun tryReserve(
        conversationKey: String,
        contactKey: String,
        signature: String,
        rule: Rule?,
        cooldownMillis: Long,
        now: Long
    ): Boolean {
        // 1) De-duplicado por HUELLA: si ya respondimos a este mismo mensaje
        // (misma huella) en esta conversacion, no repetir. Esto mata el
        // duplicado por re-publicacion al enviarse nuestra respuesta.
        if (lastRepliedSignature[conversationKey] == signature) return false

        // 2) Ventana dura por conversacion: ademas, nunca respondemos dos veces
        // a la misma conversacion en menos de HARD_DEDUP_WINDOW_MS (respaldo).
        val lastConv = lastReplyTime[conversationKey]
        if (lastConv != null && now - lastConv < HARD_DEDUP_WINDOW_MS) return false

        // 3) Cooldown configurable por contacto (anti-spam del usuario).
        val lastContact = lastReplyByContact[contactKey]
        if (lastContact != null && now - lastContact < cooldownMillis) return false

        // 4) Frecuencia por regla (una vez / cada X horas) por conversacion.
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
        lastRepliedSignature[conversationKey] = signature
        lastReplyTime[conversationKey] = now
        lastReplyByContact[contactKey] = now
        if (rule != null && rule.frequency != ReplyFrequency.ALWAYS && freqKey != null) {
            freqState[freqKey] = now
        }

        pruneIfNeeded(now)
        return true
    }

    /**
     * Revierte la reserva si el envio fallo. No revertimos la huella a proposito:
     * si fallo por red, no queremos spamear reintentos del mismo mensaje.
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
        if (lastRepliedSignature.size > 500) {
            val cutoff = lastReplyTime.values.sortedDescending().getOrNull(250) ?: return
            lastRepliedSignature.keys.retainAll { (lastReplyTime[it] ?: 0L) >= cutoff }
        }
    }
}
