package com.autoreply.bot.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lista las apps instaladas para el selector de "responder solo a estas apps".
 * Da prioridad a las apps de mensajeria conocidas.
 */
class AppListRepository(private val context: Context) {

    /** Lista las apps lanzables del dispositivo, mensajeria conocida primero. */
    suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val own = context.packageName

        // Solo apps que tienen icono en el lanzador (las que el usuario "ve").
        val launchable = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { info ->
                info.packageName != own &&
                    pm.getLaunchIntentForPackage(info.packageName) != null
            }

        launchable.map { info ->
            val pkg = info.packageName
            InstalledApp(
                packageName = pkg,
                label = loadLabel(pm, info),
                icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
                isKnownMessaging = pkg in KNOWN_MESSAGING_APPS
            )
        }.sortedWith(
            // Mensajeria conocida primero, luego por nombre.
            compareByDescending<InstalledApp> { it.isKnownMessaging }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun loadLabel(pm: PackageManager, info: ApplicationInfo): String =
        runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName)

    companion object {
        /** Paquetes de apps de mensajeria populares (se muestran arriba). */
        val KNOWN_MESSAGING_APPS = setOf(
            "com.whatsapp",                       // WhatsApp
            "com.whatsapp.w4b",                   // WhatsApp Business
            "com.facebook.orca",                  // Messenger
            "com.facebook.mlite",                 // Messenger Lite
            "com.facebook.katana",                // Facebook
            "com.instagram.android",              // Instagram
            "org.telegram.messenger",             // Telegram
            "org.telegram.messenger.web",         // Telegram Web/alt
            "org.thoughtcrime.securesms",         // Signal
            "com.viber.voip",                     // Viber
            "jp.naver.line.android",              // LINE
            "com.skype.raider",                   // Skype
            "com.google.android.apps.messaging",  // Mensajes (SMS/RCS)
            "com.discord",                        // Discord
            "com.snapchat.android",               // Snapchat
            "com.kakao.talk",                     // KakaoTalk
            "com.tencent.mm",                     // WeChat
            "com.microsoft.teams",                // Teams
            "com.slack"                           // Slack
        )
    }
}
