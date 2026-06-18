package com.autoreply.bot.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.repository.KnownGroupRepository
import com.autoreply.bot.data.repository.ReplyStateRepository
import com.autoreply.bot.data.repository.RuleRepository
import com.autoreply.bot.domain.model.KnownGroup
import com.autoreply.bot.domain.model.Rule
import com.autoreply.bot.service.ReplyGuard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val ruleRepository: RuleRepository,
    private val replyStateRepository: ReplyStateRepository,
    private val knownGroupRepository: KnownGroupRepository
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.rules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val knownGroups: StateFlow<List<KnownGroup>> = knownGroupRepository.groups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun save(rule: Rule) {
        viewModelScope.launch { ruleRepository.save(rule) }
    }

    fun toggleEnabled(rule: Rule, enabled: Boolean) {
        viewModelScope.launch {
            ruleRepository.save(rule.copy(enabled = enabled))
            // Al REACTIVAR una regla, reiniciamos su estado de envio para que
            // vuelva a responder (incluso a chats a los que ya habia respondido),
            // sin tener que borrarla y recrearla.
            if (enabled) {
                replyStateRepository.resetForRule(rule.id)
                ReplyGuard.resetRule(rule.id)
            }
        }
    }

    fun delete(rule: Rule) {
        viewModelScope.launch { ruleRepository.delete(rule) }
    }
}
