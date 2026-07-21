package com.autoreply.bot.domain.model

/** Estado de la licencia, calculado a partir del registro guardado y la fecha actual. */
sealed class LicenseStatus {

    /** Nunca se activo un codigo en este dispositivo (o los datos no son validos para el). */
    object NotActivated : LicenseStatus()

    /** Licencia activa. [daysLeft] incluye el dia de hoy (minimo 1). */
    data class Active(
        val plan: LicensePlan,
        val expiresEpochDay: Long,
        val daysLeft: Int
    ) : LicenseStatus()

    /** El plazo ya paso. */
    data class Expired(
        val plan: LicensePlan,
        val expiresEpochDay: Long
    ) : LicenseStatus()

    val isUsable: Boolean get() = this is Active
}
