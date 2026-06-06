package com.autoreply.bot.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoreply.bot.BuildConfig
import com.autoreply.bot.update.UpdateInfo
import com.autoreply.bot.update.UpdateRepository
import com.autoreply.bot.update.VersionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Estado del flujo de actualizacion. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val progress: Int) : UpdateUiState
    data class ReadyToInstall(val info: UpdateInfo, val apk: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    /** Consulta GitHub para ver si hay una version mas nueva. */
    fun checkForUpdates(silent: Boolean = false) {
        if (_state.value is UpdateUiState.Downloading) return
        viewModelScope.launch {
            if (!silent) _state.value = UpdateUiState.Checking
            val latest = repository.fetchLatest()
            _state.value = when {
                latest == null ->
                    if (silent) UpdateUiState.Idle
                    else UpdateUiState.Error("No se pudo consultar. Revisa tu conexion.")

                VersionUtils.isNewer(latest.versionName, currentVersion) ->
                    UpdateUiState.Available(latest)

                else ->
                    if (silent) UpdateUiState.Idle else UpdateUiState.UpToDate
            }
        }
    }

    /** Descarga el APK de la actualizacion disponible. */
    fun download(info: UpdateInfo) {
        viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(info, 0)
            val apk = repository.downloadApk(info) { progress ->
                _state.update { current ->
                    if (current is UpdateUiState.Downloading) current.copy(progress = progress)
                    else current
                }
            }
            _state.value = if (apk != null) {
                UpdateUiState.ReadyToInstall(info, apk)
            } else {
                UpdateUiState.Error("Fallo la descarga del APK.")
            }
        }
    }

    fun dismiss() {
        _state.value = UpdateUiState.Idle
    }
}
