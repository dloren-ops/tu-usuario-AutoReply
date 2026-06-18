package com.autoreply.bot.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.autoreply.bot.domain.model.KnownGroup

@Entity(
    tableName = "known_groups",
    indices = [Index(value = ["conversationKey"], unique = true)]
)
data class KnownGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val groupName: String,
    val conversationKey: String,
    val lastSeenAt: Long,
    val communityParent: String? = null
)

fun KnownGroupEntity.toDomain(): KnownGroup = KnownGroup(
    id = id,
    packageName = packageName,
    groupName = groupName,
    conversationKey = conversationKey,
    lastSeenAt = lastSeenAt,
    communityParent = communityParent
)

fun KnownGroup.toEntity(): KnownGroupEntity = KnownGroupEntity(
    id = id,
    packageName = packageName,
    groupName = groupName,
    conversationKey = conversationKey,
    lastSeenAt = lastSeenAt,
    communityParent = communityParent
)
