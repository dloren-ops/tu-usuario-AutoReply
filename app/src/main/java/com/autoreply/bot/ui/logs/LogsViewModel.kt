package com.autoreply.bot.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.repository.LogRepository
import com.autoreply.bot.domain.model.ReplyLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogsViewModel(
    private val logRepository: LogRepository
) : ViewModel() {

    val logs: StateFlow<List<ReplyLog>> = logRepository.recentLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun clear() {
        viewModelScope.launch { logRepository.clear() }
    }
}
