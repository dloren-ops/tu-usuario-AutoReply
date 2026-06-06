package com.autoreply.bot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recuerda la ultima vez que una regla respondio a una conversacion concreta.
 * Permite implementar "responder una sola vez" y "cada X horas" por conversacion,
 * sobreviviendo a reinicios del servicio.
 *
 * La clave [key] combina: idRegla + paquete + remitente/conversacion.
 */
@Entity(tableName = "reply_state")
data class ReplyStateEntity(
    @PrimaryKey val key: String,
    val lastReplyAt: Long
)
