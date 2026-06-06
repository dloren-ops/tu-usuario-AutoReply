package com.autoreply.bot.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoreply.bot.domain.model.AutoReplySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persiste los ajustes globales con DataStore Preferences.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val START_MINUTES = intPreferencesKey("start_minutes")
        val END_MINUTES = intPreferencesKey("end_minutes")
        val ACTIVE_DAYS = stringSetPreferencesKey("active_days")
        val REPLY_TO_GROUPS = booleanPreferencesKey("reply_to_groups")
        val COOLDOWN_SECONDS = intPreferencesKey("cooldown_seconds")
        val AWAY_ENABLED = booleanPreferencesKey("away_enabled")
        val AWAY_MESSAGE = stringPreferencesKey("away_message")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
    }

    val settings: Flow<AutoReplySettings> = context.dataStore.data.map { prefs ->
        val defaults = AutoReplySettings()
        AutoReplySettings(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: defaults.masterEnabled,
            scheduleEnabled = prefs[Keys.SCHEDULE_ENABLED] ?: defaults.scheduleEnabled,
            startMinutes = prefs[Keys.START_MINUTES] ?: defaults.startMinutes,
            endMinutes = prefs[Keys.END_MINUTES] ?: defaults.endMinutes,
            activeDays = prefs[Keys.ACTIVE_DAYS]?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: defaults.activeDays,
            replyToGroups = prefs[Keys.REPLY_TO_GROUPS] ?: defaults.replyToGroups,
            cooldownSeconds = prefs[Keys.COOLDOWN_SECONDS] ?: defaults.cooldownSeconds,
            awayMessageEnabled = prefs[Keys.AWAY_ENABLED] ?: defaults.awayMessageEnabled,
            awayMessage = prefs[Keys.AWAY_MESSAGE] ?: defaults.awayMessage,
            excludedPackages = prefs[Keys.EXCLUDED_PACKAGES] ?: defaults.excludedPackages
        )
    }

    suspend fun setMasterEnabled(value: Boolean) = edit { it[Keys.MASTER_ENABLED] = value }

    suspend fun setScheduleEnabled(value: Boolean) = edit { it[Keys.SCHEDULE_ENABLED] = value }

    suspend fun setSchedule(startMinutes: Int, endMinutes: Int) = edit {
        it[Keys.START_MINUTES] = startMinutes
        it[Keys.END_MINUTES] = endMinutes
    }

    suspend fun setActiveDays(days: Set<Int>) = edit {
        it[Keys.ACTIVE_DAYS] = days.map { d -> d.toString() }.toSet()
    }

    suspend fun setReplyToGroups(value: Boolean) = edit { it[Keys.REPLY_TO_GROUPS] = value }

    suspend fun setCooldownSeconds(value: Int) = edit { it[Keys.COOLDOWN_SECONDS] = value }

    suspend fun setAwayEnabled(value: Boolean) = edit { it[Keys.AWAY_ENABLED] = value }

    suspend fun setAwayMessage(value: String) = edit { it[Keys.AWAY_MESSAGE] = value }

    suspend fun setExcludedPackages(packages: Set<String>) = edit {
        it[Keys.EXCLUDED_PACKAGES] = packages
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
