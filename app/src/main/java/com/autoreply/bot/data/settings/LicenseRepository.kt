package com.autoreply.bot.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoreply.bot.domain.model.LicensePlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.licenseDataStore by preferencesDataStore(name = "license")

/** Registro de licencia guardado para este dispositivo. */
data class LicenseRecord(
    val deviceHashHex: String,
    val plan: LicensePlan,
    val expiresEpochDay: Long,
    val lastSeenEpochDay: Long
)

/**
 * Persiste el resultado de la ultima activacion valida. No hay servidor: todo
 * se valida localmente contra el codigo firmado (ver [com.autoreply.bot.license.LicenseCode]).
 */
class LicenseRepository(private val context: Context) {

    private object Keys {
        val DEVICE_HASH = stringPreferencesKey("device_hash")
        val PLAN = stringPreferencesKey("plan")
        val EXPIRES_EPOCH_DAY = longPreferencesKey("expires_epoch_day")
        val LAST_SEEN_EPOCH_DAY = longPreferencesKey("last_seen_epoch_day")
    }

    val record: Flow<LicenseRecord?> = context.licenseDataStore.data.map { prefs ->
        val deviceHash = prefs[Keys.DEVICE_HASH] ?: return@map null
        val planName = prefs[Keys.PLAN] ?: return@map null
        val plan = runCatching { LicensePlan.valueOf(planName) }.getOrNull() ?: return@map null
        val expires = prefs[Keys.EXPIRES_EPOCH_DAY] ?: return@map null
        LicenseRecord(
            deviceHashHex = deviceHash,
            plan = plan,
            expiresEpochDay = expires,
            lastSeenEpochDay = prefs[Keys.LAST_SEEN_EPOCH_DAY] ?: 0L
        )
    }

    suspend fun save(deviceHashHex: String, plan: LicensePlan, expiresEpochDay: Long, today: Long) {
        context.licenseDataStore.edit { prefs ->
            prefs[Keys.DEVICE_HASH] = deviceHashHex
            prefs[Keys.PLAN] = plan.name
            prefs[Keys.EXPIRES_EPOCH_DAY] = expiresEpochDay
            val previousSeen = prefs[Keys.LAST_SEEN_EPOCH_DAY] ?: 0L
            prefs[Keys.LAST_SEEN_EPOCH_DAY] = maxOf(previousSeen, today)
        }
    }

    /** Avanza la marca "ultimo dia visto" para detectar si alguien retrasa el reloj del telefono. */
    suspend fun bumpLastSeen(today: Long) {
        context.licenseDataStore.edit { prefs ->
            val previous = prefs[Keys.LAST_SEEN_EPOCH_DAY] ?: 0L
            if (today > previous) prefs[Keys.LAST_SEEN_EPOCH_DAY] = today
        }
    }
}
