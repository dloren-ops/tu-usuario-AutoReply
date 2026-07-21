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
import com.autoreply.bot.data.repository.KnownGroupRepository
import com.autoreply.bot.domain.ReplyEngine
import com.autoreply.bot.domain.model.AutoReplySettings
import com.autoreply.bot.domain.model.KnownGroup
import com.autoreply.bot.domain.model.LicenseStatus
import com.autoreply.bot.domain.model.ReplyFrequency
import com.autoreply.bot.domain.model.ReplyLog
import com.autoreply.bot.domain.model.Rule
import com.autoreply.bot.license.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Servicio que escucha las notificaciones de otras apps y responde
 * automaticamente usando la accion de respuesta directa (RemoteInput).
 *
 * IMPORTANTE: el estado anti-duplicado vive en [ReplyGuard] (singleton estatico)
 * y NO en campos de esta clase. Android puede tener mas de una instancia de este
 * servicio viva a la vez (sobre todo tras actualizar la app); si el estado fuera
 * por-instancia, ambas responderian y se enviarian mensajes duplicados.
 */
class AutoReplyNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cache en memoria de ajustes y reglas, actualizado de forma reactiva.
    @Volatile
    private var settings: AutoReplySettings = AutoReplySettings()

    @Volatile
    private var rules: List<Rule> = emptyList()

    // Licencia: sin ella, el servicio no responde. Se recalcula tambien de forma
    // periodica (no solo al cambiar el registro) porque el vencimiento depende
    // de la fecha actual, no de un evento.
    @Volatile
    private var licenseActive: Boolean = false

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

        // Precargar el estado de frecuencia persistido en el guard estatico.
        scope.launch {
            val map = app.container.replyStateRepository.getAll()
                .associate { it.key to it.lastReplyAt }
            ReplyGuard.preloadFrequency(map)
        }

        observeLicense()
    }

    private fun observeLicense() {
        app.container.licenseRepository.record
            .onEach { record ->
                licenseActive = LicenseManager.computeStatus(applicationContext, record) is LicenseStatus.Active
            }
            .launchIn(scope)

        // Reevalua el vencimiento aunque el registro no cambie (el tiempo si).
        scope.launch {
            while (isActive) {
                LicenseManager.refreshLastSeen(app.container.licenseRepository)
                val record = app.container.licenseRepository.record.first()
                licenseActive = LicenseManager.computeStatus(applicationContext, record) is LicenseStatus.Active
                delay(LICENSE_RECHECK_INTERVAL_MS)
            }
        }
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

        if (!licenseActive) return

        val s = settings
        if (!s.masterEnabled) return
        if (sbn.packageName in s.excludedPackages) return
        // Filtro por apps: si esta activo el modo restringido, solo respondemos
        // a las apps que el usuario eligio explicitamente.
        if (s.restrictToApps && sbn.packageName !in s.allowedPackages) return
        if (!ReplyEngine.isWithinSchedule(s, System.currentTimeMillis())) return

        val notification = sbn.notification ?: return

        // Saltar resumenes de grupo: en grupos, WhatsApp publica una notificacion
        // "resumen" ademas de la del mensaje. Si respondieramos a ambas saldria
        // duplicado. Solo procesamos la notificacion concreta del mensaje.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return

        // Detectar conversacion de grupo.
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        // Filtro global de grupos (ajuste general). Las reglas tienen ademas su
        // propio alcance (todos/solo grupos/solo individuales).
        if (isGroup && !s.replyToGroups) return

        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val message = extractMessage(extras)
        if (message.isBlank()) return

        // Solo respondemos a notificaciones que admiten respuesta directa.
        val replyAction = findReplyAction(notification) ?: return

        val now = System.currentTimeMillis()

        // Clave de CONVERSACION estable e independiente del texto del mensaje.
        val conversationKey = conversationKey(sbn, sender)
        val contactKey = sbn.packageName + "|" + sender

        // Auto-descubrimiento de grupos: registrar el grupo en la BD local y
        // obtener su ID para el filtrado de reglas. Ambas operaciones se ejecutan
        // en un unico bloque runBlocking para evitar la race condition donde el
        // upsert (fire-and-forget) no ha completado cuando se intenta leer el ID.
        // Estamos en el binder thread del sistema (off-main), y el upsert de Room
        // es sub-millisecond, asi que el bloqueo es despreciable.
        var groupId: Long? = null
        if (isGroup) {
            val communityParent = extras.getCharSequence("android.subText")?.toString()?.trim()
            groupId = runBlocking {
                app.container.knownGroupRepository.upsert(
                    KnownGroup(
                        packageName = sbn.packageName,
                        groupName = sender,
                        conversationKey = conversationKey,
                        lastSeenAt = now,
                        communityParent = communityParent?.ifBlank { null }
                    )
                )
                app.container.knownGroupRepository.getByConversationKey(conversationKey)?.id
            }
        }

        // HUELLA del mensaje entrante = texto + hora real del mensaje. Es estable:
        // si WhatsApp re-publica la notificacion (p. ej. al enviarse nuestra
        // respuesta), el ultimo mensaje entrante sigue siendo el mismo y la huella
        // no cambia -> no respondemos de nuevo. Solo cambia con un mensaje nuevo.
        val messageStamp = extractMessageTimestamp(extras, sbn)
        val signature = message + "|" + messageStamp

        // Decidir respuesta segun reglas + alcance (grupo/individual) + filtro de grupo.
        val decision = ReplyEngine.decideReply(message, rules, s, isGroup, groupId) ?: return
        val rule = decision.rule

        // Reservar el turno de forma atomica y compartida entre instancias.
        // Si no se autoriza (duplicado, cooldown o frecuencia), salimos.
        val reserved = ReplyGuard.tryReserve(
            conversationKey = conversationKey,
            contactKey = contactKey,
            signature = signature,
            rule = rule,
            cooldownMillis = s.cooldownSeconds * 1000L,
            now = now
        )
        if (!reserved) return

        val sent = sendReply(replyAction, decision.text)
        if (!sent) {
            ReplyGuard.rollback(conversationKey, contactKey, rule)
            return
        }

        // Persistir el estado de frecuencia por regla+conversacion.
        val freqKey = ReplyGuard.freqKey(rule, conversationKey)
        if (rule != null && rule.frequency != ReplyFrequency.ALWAYS && freqKey != null) {
            scope.launch { app.container.replyStateRepository.markReplied(freqKey, now) }
        }

        // Registrar la respuesta enviada.
        val appLabel = loadAppLabel(sbn.packageName)
        scope.launch {
            app.container.logRepository.add(
                ReplyLog(
                    appPackage = sbn.packageName,
                    appLabel = appLabel,
                    sender = sender,
                    incomingMessage = message,
                    replyText = decision.text,
                    timestamp = now
                )
            )
        }
    }

    /**
     * Identificador estable de una conversacion.
     *
     * Usa [StatusBarNotification.getKey], el identificador interno y UNICO que
     * Android asigna a cada notificacion/chat. Es el mismo mientras la
     * conversacion exista y NO cambia aunque el titulo muestre "(N mensajes)"
     * ni cuando la app re-publica la notificacion. Asi distinguimos de forma
     * fiable "este chat" de "otro chat" (cada grupo/persona tiene su propia key).
     */
    private fun conversationKey(sbn: StatusBarNotification, sender: String): String {
        return sbn.packageName + "|" + sbn.key
    }

    /** Extrae el texto del mensaje de los distintos campos posibles. */
    private fun extractMessage(extras: Bundle): String {
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { if (it.isNotBlank()) return it.toString() }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { if (it.isNotBlank()) return it.toString() }
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!lines.isNullOrEmpty()) return lines.last().toString()
        return ""
    }

    /**
     * Marca de tiempo del ultimo mensaje entrante. Es estable: identifica al
     * mensaje y NO cambia cuando la app re-publica la notificacion.
     *
     * Preferimos el timestamp del ultimo mensaje de MessagingStyle (lo mas
     * fiable). Si no esta disponible, usamos when del notification, y por ultimo
     * el postTime de la notificacion.
     */
    private fun extractMessageTimestamp(extras: Bundle, sbn: StatusBarNotification): Long {
        // MessagingStyle: lista de mensajes con su timestamp real.
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            var maxStamp = 0L
            for (item in messages) {
                val bundle = item as? Bundle ?: continue
                val time = bundle.getLong("time", 0L)
                if (time > maxStamp) maxStamp = time
            }
            if (maxStamp > 0L) return maxStamp
        }
        // "when" de la notificacion (suele ser la hora del mensaje).
        val whenTime = sbn.notification.`when`
        if (whenTime > 0L) return whenTime
        // Ultimo recurso: hora en que se publico la notificacion.
        return sbn.postTime
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
        private const val LICENSE_RECHECK_INTERVAL_MS = 15 * 60 * 1000L
    }
}
