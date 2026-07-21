package com.autoreply.bot.ui.license

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.data.settings.LicenseRepository
import com.autoreply.bot.domain.model.LicenseStatus
import com.autoreply.bot.license.ActivationResult
import com.autoreply.bot.license.LicenseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LicenseUiState(
    val deviceId: String = "",
    val status: LicenseStatus = LicenseStatus.NotActivated,
    val isActivating: Boolean = false,
    val feedback: String? = null
)

class LicenseViewModel(
    private val context: Context,
    private val licenseRepository: LicenseRepository
) : ViewModel() {

    private val feedback = MutableStateFlow<String?>(null)
    private val activating = MutableStateFlow(false)

    val uiState: StateFlow<LicenseUiState> = combine(
        licenseRepository.record,
        feedback,
        activating
    ) { record, msg, isActivating ->
        LicenseUiState(
            deviceId = LicenseManager.deviceId(context),
            status = LicenseManager.computeStatus(context, record),
            isActivating = isActivating,
            feedback = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LicenseUiState(deviceId = LicenseManager.deviceId(context))
    )

    init {
        viewModelScope.launch { LicenseManager.refreshLastSeen(licenseRepository) }
    }

    fun activate(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            activating.value = true
            feedback.value = when (LicenseManager.activate(context, licenseRepository, code)) {
                is ActivationResult.Success -> null
                ActivationResult.InvalidCode -> "Codigo invalido. Revisa que lo copiaste completo."
                ActivationResult.WrongDevice -> "Este codigo es para otro telefono."
                ActivationResult.AlreadyExpired -> "Este codigo ya vencio. Pide uno nuevo."
            }
            activating.value = false
        }
    }

    fun dismissFeedback() {
        feedback.value = null
    }
}
