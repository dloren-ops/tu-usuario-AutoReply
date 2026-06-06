package com.autoreply.bot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.settings.SettingsRepository
import com.autoreply.bot.domain.model.AutoReplySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AutoReplySettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoReplySettings()
    )

    fun setScheduleEnabled(value: Boolean) =
        launch { settingsRepository.setScheduleEnabled(value) }

    fun setSchedule(startMinutes: Int, endMinutes: Int) =
        launch { settingsRepository.setSchedule(startMinutes, endMinutes) }

    fun toggleDay(day: Int) = launch {
        val current = settings.value.activeDays.toMutableSet()
        if (!current.add(day)) current.remove(day)
        settingsRepository.setActiveDays(current)
    }

    fun setReplyToGroups(value: Boolean) =
        launch { settingsRepository.setReplyToGroups(value) }

    fun setCooldownSeconds(value: Int) =
        launch { settingsRepository.setCooldownSeconds(value) }

    fun setAwayEnabled(value: Boolean) =
        launch { settingsRepository.setAwayEnabled(value) }

    fun setAwayMessage(value: String) =
        launch { settingsRepository.setAwayMessage(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
