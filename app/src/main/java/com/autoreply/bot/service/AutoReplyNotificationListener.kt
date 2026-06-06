package com.autoreply.bot.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autoreply.bot.AutoReplyApp
import com.autoreply.bot.domain.ReplyEngine
import com.autoreply.bot.domain.model.AutoReplySettings
import com.autoreply.bot.domain.model.ReplyLog
import com.autoreply.bot.domain.model.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Servicio que escucha las notificaciones de otras apps y responde
 * automaticamente usando la accion de respuesta directa (RemoteInput).
 */
class AutoReplyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache en memoria de ajustes y reglas, actualizado de forma reactiva.
    @Volatile
    private var settings: AutoReplySettings = AutoReplySettings()

    @Volatile
    private var rules: List<Rule> = emptyList()

    // Control anti-spam: ultima respuesta por (paquete + remitente).
    private val lastReplyAt = ConcurrentHashMap<String, Long>()

    private val app: AutoReplyApp
        get() = application as AutoReplyApp

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Observar ajustes y reglas mientras el servicio este conectado.
        app.container.settingsRepository.settings
            .onEach { settings = it }
            .launchIn(scope)

        app.container.ruleRepository.rules
            .onEach { rules = it }
            .launchIn(scope)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handleNotification(sbn)
        } catch (t: Throwable) {
            Log.w(TAG, "Error procesando notificacion", t)
        }
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        // Ignorar nuestras propias notificaciones para evitar bucles.
        if (sbn.packageName == packageName) return

        val s = settings
        if (!s.masterEnabled) return
        if (sbn.packageName in s.excludedPackages) return
        if (!ReplyEngine.isWithinSchedule(s, System.currentTimeMillis())) return

        val notification = sbn.notification ?: return

        // Saltar resumenes de grupo y notificaciones persistentes del sistema.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return

        // Detectar conversacion de grupo.
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        if (isGroup && !s.replyToGroups) return

        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = extractMessage(extras)
        if (message.isBlank()) return

        // Buscar la accion de respuesta directa (RemoteInput).
        val replyAction = findReplyAction(notification) ?: return

        // Anti-spam: respetar el cooldown por contacto.
        val key = sbn.packageName + "|" + sender
        val now = System.currentTimeMillis()
        val last = lastReplyAt[key]
        if (last != null && now - last < s.cooldownSeconds * 1000L) return

        val replyText = ReplyEngine.decideReply(message, rules, s) ?: return

        val sent = sendReply(replyAction, replyText)
        if (!sent) return

        lastReplyAt[key] = now

        // Registrar la respuesta enviada.
        val appLabel = loadAppLabel(sbn.packageName)
        scope.launch {
            app.container.logRepository.add(
                ReplyLog(
                    appPackage = sbn.packageName,
                    appLabel = appLabel,
                    sender = sender,
                    incomingMessage = message,
                    replyText = replyText,
                    timestamp = now
                )
            )
        }
    }

    /** Extrae el texto del mensaje de los distintos campos posibles. */
    private fun extractMessage(extras: Bundle): String {
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { if (it.isNotBlank()) return it.toString() }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { if (it.isNotBlank()) return it.toString() }
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!lines.isNullOrEmpty()) return lines.last().toString()
        return ""
    }

    /** Localiza la primera accion que tenga una entrada de texto (RemoteInput). */
    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.firstOrNull { action ->
            action.remoteInputs?.any { it.resultKey != null } == true
        }
    }

    /** Envia el texto usando el PendingIntent y RemoteInput de la accion. */
    private fun sendReply(action: Notification.Action, text: String): Boolean {
        val remoteInputs = action.remoteInputs ?: return false
        val intent = Intent()
        val results = Bundle()
        for (remoteInput in remoteInputs) {
            results.putCharSequence(remoteInput.resultKey, text)
        }
        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
        return try {
            action.actionIntent.send(this, 0, intent)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "PendingIntent cancelado, no se pudo responder", e)
            false
        }
    }

    private fun loadAppLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "AutoReplyListener"
    }
}
