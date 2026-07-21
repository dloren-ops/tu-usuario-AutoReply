package com.autoreply.bot.di

import android.content.Context
import com.autoreply.bot.apps.AppListRepository
import com.autoreply.bot.data.local.AppDatabase
import com.autoreply.bot.data.repository.KnownGroupRepository
import com.autoreply.bot.data.repository.LogRepository
import com.autoreply.bot.data.repository.ReplyStateRepository
import com.autoreply.bot.data.repository.RuleRepository
import com.autoreply.bot.data.settings.LicenseRepository
import com.autoreply.bot.data.settings.SettingsRepository
import com.autoreply.bot.update.UpdateRepository

/**
 * Inyeccion de dependencias manual y ligera (sin frameworks) para mantener
 * la app rapida y con tiempos de compilacion bajos.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database = AppDatabase.getInstance(context)

    val ruleRepository: RuleRepository = RuleRepository(database.ruleDao())
    val logRepository: LogRepository = LogRepository(database.logDao())
    val replyStateRepository: ReplyStateRepository = ReplyStateRepository(database.replyStateDao())
    val knownGroupRepository: KnownGroupRepository = KnownGroupRepository(database.knownGroupDao())
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val licenseRepository: LicenseRepository = LicenseRepository(context)
    val updateRepository: UpdateRepository = UpdateRepository(context)
    val appListRepository: AppListRepository = AppListRepository(context)
}
