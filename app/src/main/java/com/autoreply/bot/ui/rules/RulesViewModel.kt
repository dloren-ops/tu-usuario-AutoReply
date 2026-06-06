package com.autoreply.bot.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.repository.RuleRepository
import com.autoreply.bot.domain.model.Rule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.rules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun save(rule: Rule) {
        viewModelScope.launch { ruleRepository.save(rule) }
    }

    fun toggleEnabled(rule: Rule, enabled: Boolean) {
        viewModelScope.launch { ruleRepository.save(rule.copy(enabled = enabled)) }
    }

    fun delete(rule: Rule) {
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}
