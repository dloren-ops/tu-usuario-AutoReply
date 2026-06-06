package com.autoreply.bot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoreply.bot.domain.model.MatchType
import com.autoreply.bot.domain.model.Rule

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val response: String,
    val matchType: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

fun RuleEntity.toDomain(): Rule = Rule(
    id = id,
    keyword = keyword,
    response = response,
    matchType = runCatching { MatchType.valueOf(matchType) }.getOrDefault(MatchType.CONTAINS),
    enabled = enabled,
    priority = priority,
    createdAt = createdAt
)

fun Rule.toEntity(): RuleEntity = RuleEntity(
    id = id,
    keyword = keyword,
    response = response,
    matchType = matchType.name,
    enabled = enabled,
    priority = priority,
    createdAt = createdAt
)
