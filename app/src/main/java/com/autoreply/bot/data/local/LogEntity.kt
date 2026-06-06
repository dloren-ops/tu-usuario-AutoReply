package com.autoreply.bot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoreply.bot.domain.model.ReplyLog

@Entity(tableName = "reply_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val appLabel: String,
    val sender: String,
    val incomingMessage: String,
    val replyText: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun LogEntity.toDomain(): ReplyLog = ReplyLog(
    id = id,
    appPackage = appPackage,
    appLabel = appLabel,
    sender = sender,
    incomingMessage = incomingMessage,
    replyText = replyText,
    timestamp = timestamp
)

fun ReplyLog.toEntity(): LogEntity = LogEntity(
    id = id,
    appPackage = appPackage,
    appLabel = appLabel,
    sender = sender,
    incomingMessage = incomingMessage,
    replyText = replyText,
    timestamp = timestamp
)
