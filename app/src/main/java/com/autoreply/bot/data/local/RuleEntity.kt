package com.autoreply.bot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoreply.bot.domain.model.MatchType
import com.autoreply.bot.domain.model.ReplyFrequency
import com.autoreply.bot.domain.model.ReplyScope
import com.autoreply.bot.domain.model.Rule

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val keyword: String,
    val response: String,
    val matchType: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val scope: String = ReplyScope.ALL.name,
    val frequency: String = ReplyFrequency.ALWAYS.name,
    val everyHours: Int = 24,
    val createdAt: Long = System.currentTimeMillis(),
    val allowedGroupIds: String = ""
)

fun RuleEntity.toDomain(): Rule = Rule(
    id = id,
    title = title,
    keyword = keyword,
    response = response,
    matchType = runCatching { MatchType.valueOf(matchType) }.getOrDefault(MatchType.CONTAINS),
    enabled = enabled,
    priority = priority,
    scope = runCatching { ReplyScope.valueOf(scope) }.getOrDefault(ReplyScope.ALL),
    frequency = runCatching { ReplyFrequency.valueOf(frequency) }.getOrDefault(ReplyFrequency.ALWAYS),
    everyHours = everyHours,
    createdAt = createdAt,
    allowedGroupIds = if (allowedGroupIds.isBlank()) emptySet()
        else allowedGroupIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
)

fun Rule.toEntity(): RuleEntity = RuleEntity(
    id = id,
    title = title,
    keyword = keyword,
    response = response,
    matchType = matchType.name,
    enabled = enabled,
    priority = priority,
    scope = scope.name,
    frequency = frequency.name,
    everyHours = everyHours,
    createdAt = createdAt,
    allowedGroupIds = allowedGroupIds.joinToString(",")
)
