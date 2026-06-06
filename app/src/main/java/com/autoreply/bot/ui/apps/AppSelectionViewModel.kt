package com.autoreply.bot.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.apps.AppListRepository
import com.autoreply.bot.apps.InstalledApp
import com.autoreply.bot.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSelectionViewModel(
    private val appListRepository: AppListRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Modo restringido activo (solo apps elegidas). */
    val restrictToApps: StateFlow<Boolean> = settingsRepository.settings
        .map { it.restrictToApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Conjunto de paquetes permitidos. */
    val allowedPackages: StateFlow<Set<String>> = settingsRepository.settings
        .map { it.allowedPackages }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _loading.value = true
            _apps.value = appListRepository.loadApps()
            _loading.value = false
        }
    }

    fun setRestrictToApps(value: Boolean) {
        viewModelScope.launch { settingsRepository.setRestrictToApps(value) }
    }

    fun toggleApp(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleAllowedPackage(packageName, allowed)
        }
    }
}
