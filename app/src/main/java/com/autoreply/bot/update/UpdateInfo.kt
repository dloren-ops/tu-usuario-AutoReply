package com.autoreply.bot.update

/**
 * Informacion de una version publicada en GitHub Releases.
 */
data class UpdateInfo(
    /** Nombre del tag, ej. "v1.1". */
    val versionTag: String,
    /** Version "limpia" sin la 'v', ej. "1.1". */
    val versionName: String,
    /** URL de descarga directa del APK. */
    val apkUrl: String,
    /** Notas de la version (cuerpo del release). */
    val releaseNotes: String,
    /** URL de la pagina del release en GitHub. */
    val htmlUrl: String
)
