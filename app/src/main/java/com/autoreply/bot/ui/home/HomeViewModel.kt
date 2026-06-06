package com.autoreply.bot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.repository.LogRepository
import com.autoreply.bot.data.settings.SettingsRepository
import com.autoreply.bot.domain.model.AutoReplySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AutoReplySettings = AutoReplySettings(),
    val totalReplies: Int = 0
)

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    logRepository: LogRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(settingsRepository.settings, logRepository.recentLogs) { settings, logs ->
            HomeUiState(settings = settings, totalReplies = logs.size)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMasterEnabled(enabled) }
    }
}
