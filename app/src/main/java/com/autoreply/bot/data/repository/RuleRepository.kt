package com.autoreply.bot.data.repository

import com.autoreply.bot.data.local.RuleDao
import com.autoreply.bot.data.local.toDomain
import com.autoreply.bot.data.local.toEntity
import com.autoreply.bot.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepository(private val dao: RuleDao) {

    val rules: Flow<List<Rule>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getEnabledRules(): List<Rule> = dao.getEnabledRules().map { it.toDomain() }

    suspend fun save(rule: Rule): Long = dao.upsert(rule.toEntity())

    suspend fun delete(rule: Rule) = dao.delete(rule.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
