package com.autoreply.bot.apps

import android.graphics.drawable.Drawable

/**
 * Representa una app instalada que puede mostrarse en el selector.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    /** true si es una app de mensajeria conocida (se muestra arriba). */
    val isKnownMessaging: Boolean
)
