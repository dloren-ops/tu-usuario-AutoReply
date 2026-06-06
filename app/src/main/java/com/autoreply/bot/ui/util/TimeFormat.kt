package com.autoreply.bot.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {

    private val dateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /** Convierte minutos desde medianoche a "HH:mm". */
    fun minutesToHHmm(minutes: Int): String {
        val h = (minutes / 60).coerceIn(0, 23)
        val m = (minutes % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    fun formatTimestamp(millis: Long): String = dateTime.format(Date(millis))

    val dayLabels = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
}
