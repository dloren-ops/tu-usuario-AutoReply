package com.autoreply.bot.license

import android.content.Context
import android.provider.Settings
import com.autoreply.bot.data.settings.LicenseRecord
import com.autoreply.bot.data.settings.LicenseRepository
import com.autoreply.bot.domain.model.LicenseStatus
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.time.LocalDate

sealed class ActivationResult {
    data class Success(val status: LicenseStatus.Active) : ActivationResult()
    object InvalidCode : ActivationResult()
    object WrongDevice : ActivationResult()
    object AlreadyExpired : ActivationResult()
}

/**
 * Logica de activacion y validacion de licencia. Todo ocurre en el
 * dispositivo, sin red: el codigo trae su propia firma y fecha de
 * vencimiento (ver [LicenseCode]).
 */
object LicenseManager {

    /** Identificador estable del dispositivo (cambia solo si se restaura de fabrica). */
    @Suppress("HardwareIds")
    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown-device"

    fun deviceHash(deviceId: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(deviceId.toByteArray(Charsets.UTF_8)).copyOf(4)

    fun deviceHashHex(context: Context): String = deviceHash(deviceId(context)).toHex()

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    suspend fun activate(
        context: Context,
        repository: LicenseRepository,
        rawCode: String
    ): ActivationResult {
        val parsed = LicenseCode.verifyAndParse(rawCode) ?: return ActivationResult.InvalidCode

        val myHash = deviceHash(deviceId(context))
        if (!parsed.deviceHash.contentEquals(myHash)) return ActivationResult.WrongDevice

        val today = todayEpochDay()
        if (parsed.expiresEpochDay < today) return ActivationResult.AlreadyExpired

        repository.save(myHash.toHex(), parsed.plan, parsed.expiresEpochDay, today)
        val daysLeft = (parsed.expiresEpochDay - today + 1).toInt()
        return ActivationResult.Success(LicenseStatus.Active(parsed.plan, parsed.expiresEpochDay, daysLeft))
    }

    /** Recalcula el estado a partir del registro guardado y la fecha/dispositivo actuales. */
    fun computeStatus(context: Context, record: LicenseRecord?): LicenseStatus {
        if (record == null) return LicenseStatus.NotActivated
        // Si los datos vienen de una copia de seguridad restaurada en OTRO telefono,
        // el hash del dispositivo actual no coincidira: exigimos reactivar aqui.
        if (record.deviceHashHex != deviceHashHex(context)) return LicenseStatus.NotActivated

        val today = todayEpochDay()
        // El reloj retrocedio respecto al ultimo dia visto: trato el plazo como agotado
        // (evita extender una demo/alquiler adelantando y luego atrasando la fecha).
        if (today < record.lastSeenEpochDay - 1) {
            return LicenseStatus.Expired(record.plan, record.expiresEpochDay)
        }
        if (today > record.expiresEpochDay) return LicenseStatus.Expired(record.plan, record.expiresEpochDay)

        val daysLeft = (record.expiresEpochDay - today + 1).toInt()
        return LicenseStatus.Active(record.plan, record.expiresEpochDay, daysLeft)
    }

    /** Actualiza la marca "ultimo dia visto"; llamar periodicamente mientras la app/servicio esta viva. */
    suspend fun refreshLastSeen(repository: LicenseRepository) {
        repository.record.first() ?: return
        repository.bumpLastSeen(todayEpochDay())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
