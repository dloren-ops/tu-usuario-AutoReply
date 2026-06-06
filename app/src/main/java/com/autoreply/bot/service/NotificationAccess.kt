package com.autoreply.bot.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Utilidades para comprobar y abrir el permiso de "Acceso a las notificaciones".
 */
object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(context, AutoReplyNotificationListener::class.java)
        return enabled.split(":").any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn == component
        }
    }

    /** Intent para abrir la pantalla de ajustes de acceso a notificaciones. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
