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
 * Todas las decisiones se toman en el hilo principal del servicio (las callbacks
 * de notificaciones son secuenciales), por lo que con estado compartido el
 * de-duplicado es fiable.
 */
object ReplyGuard {

    /** Ultima vez que respondimos a una CONVERSACION (independiente del texto). */
    private val lastReplyByConversation = ConcurrentHashMap<String, Long>()

    /** Ultima vez que respondimos a un CONTACTO (para el cooldown del usuario). */
    private val lastReplyByContact = ConcurrentHashMap<String, Long>()

    /** Estado de frecuencia por regla+conversacion. */
    private val freqState = ConcurrentHashMap<String, Long>()

    /**
     * Ventana dura minima entre respuestas a la MISMA conversacion. Mata los
     * duplicados que llegan con milisegundos de diferencia, sin importar el
     * cooldown configurado por el usuario.
     */
    private const val HARD_DEDUP_WINDOW_MS = 6_000L

    /** Precarga el estado de frecuencia persistido (al conectar el servicio). */
    fun preloadFrequency(entries: Map<String, Long>) {
        freqState.putAll(entries)
    }

    /**
     * Decide si se debe responder AHORA y, si si, reserva el turno de inmediato
     * (antes de enviar) para que una segunda notificacion casi simultanea -o
     * una segunda instancia del servicio- no vuelva a responder.
     *
     * @return true si se autoriza responder (y ya quedo reservado).
     */
    @Synchronized
    fun tryReserve(
        conversationKey: String,
        contactKey: String,
        rule: Rule?,
        cooldownMillis: Long,
        now: Long
    ): Boolean {
        // 1) De-duplicado duro por conversacion (mata duplicados ms-apart).
        val lastConv = lastReplyByConversation[conversationKey]
        if (lastConv != null && now - lastConv < HARD_DEDUP_WINDOW_MS) return false

        // 2) Cooldown configurable por contacto.
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
        lastReplyByConversation[conversationKey] = now
        lastReplyByContact[contactKey] = now
        if (rule != null && rule.frequency != ReplyFrequency.ALWAYS && freqKey != null) {
            freqState[freqKey] = now
        }

        pruneIfNeeded(now)
        return true
    }

    /** Revierte la reserva si el envio fallo (para permitir reintento). */
    @Synchronized
    fun rollback(conversationKey: String, contactKey: String, rule: Rule?) {
        lastReplyByConversation.remove(conversationKey)
        lastReplyByContact.remove(contactKey)
        val freqKey = freqKey(rule, conversationKey)
        if (freqKey != null) freqState.remove(freqKey)
    }

    fun freqKey(rule: Rule?, conversationKey: String): String? =
        if (rule != null) "rule:${rule.id}|$conversationKey" else null

    private fun pruneIfNeeded(now: Long) {
        if (lastReplyByConversation.size > 300) {
            lastReplyByConversation.entries.removeAll { now - it.value > 60_000L }
        }
        if (lastReplyByContact.size > 300) {
            lastReplyByContact.entries.removeAll { now - it.value > 3_600_000L }
        }
    }
}
