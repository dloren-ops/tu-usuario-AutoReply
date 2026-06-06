package com.autoreply.bot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autoreply.bot.di.AppContainer
import com.autoreply.bot.ui.apps.AppSelectionViewModel
import com.autoreply.bot.ui.home.HomeViewModel
import com.autoreply.bot.ui.logs.LogsViewModel
import com.autoreply.bot.ui.rules.RulesViewModel
import com.autoreply.bot.ui.settings.SettingsViewModel
import com.autoreply.bot.ui.update.UpdateViewModel

/**
 * Fabrica de ViewModels que inyecta los repositorios del [AppContainer].
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(container.settingsRepository, container.logRepository)

            modelClass.isAssignableFrom(RulesViewModel::class.java) ->
                RulesViewModel(container.ruleRepository)

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.settingsRepository)

            modelClass.isAssignableFrom(LogsViewModel::class.java) ->
                LogsViewModel(container.logRepository)

            modelClass.isAssignableFrom(UpdateViewModel::class.java) ->
                UpdateViewModel(container.updateRepository)

            modelClass.isAssignableFrom(AppSelectionViewModel::class.java) ->
                AppSelectionViewModel(container.appListRepository, container.settingsRepository)

            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        } as T
    }
}
