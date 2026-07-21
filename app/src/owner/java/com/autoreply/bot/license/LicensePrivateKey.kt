package com.autoreply.bot.license

/**
 * Clave PRIVADA (PKCS8/DER, ECDSA P-256) usada SOLO por la variante "owner"
 * para firmar codigos de activacion desde el propio telefono, sin depender
 * de la computadora. Este archivo NO existe en la variante "client": los
 * telefonos de los inquilinos nunca la reciben.
 *
 * Generada junto a la clave publica con `tools/generate_keypair.py`, que
 * tambien deja una copia en `tools/owner_private_key.pem` (usada por
 * `tools/generate_license.py`, el generador de linea de comandos).
 *
 * Riesgo asumido a proposito: como esta app no tiene servidor, firmar desde
 * el celular exige tener la clave privada ahi. Si perdes o te roban el
 * telefono con la variante "owner" instalada, rota el par de claves (volve
 * a correr generate_keypair.py, actualiza este archivo y LicensePublicKey.kt,
 * y recompila) para invalidar la clave vieja.
 */
internal object LicensePrivateKey {
    const val HEX = "308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b0201010420081a1287938b36d0ff81979c435146d41c6fa3047856d716a68124aa2d4f4990a14403420004471f17383c6f90a7348fe0227a0a06af8e701c328f25912546c23ddc3d2c2fc912a475ece06cf9a1f39dd0314227861c68241541d36935b1fc70bc28396cb837"

    val bytes: ByteArray by lazy {
        ByteArray(HEX.length / 2) { i -> HEX.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
