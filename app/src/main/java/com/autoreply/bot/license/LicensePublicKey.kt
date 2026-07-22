package com.autoreply.bot.license

/**
 * Clave PUBLICA (ECDSA P-256, punto sin comprimir 0x04||X||Y) usada para
 * VERIFICAR codigos de activacion. No es sensible: con esto no se pueden
 * firmar codigos nuevos, solo comprobar si uno ya firmado es valido. Por
 * eso puede vivir tranquila en `main` (se compila en ambas variantes,
 * "client" y "owner").
 *
 * Generada junto a su clave privada con `tools/generate_keypair.py`. Si
 * alguna vez rotas el par de claves, actualiza esto Y
 * `app/src/owner/.../license/LicensePrivateKey.kt` Y `tools/owner_private_key.pem`
 * a la vez, o los codigos viejos y nuevos dejaran de coincidir entre si.
 */
internal object LicensePublicKey {
    const val HEX = "0420038973625aacb0e0de099b669efe051790bbbe9ac080716be35050c2d3ac885f4b51b3ea02938c72aa3108773bc48ce78f0426cfa8b92857d6813d569999b6"

    val bytes: ByteArray by lazy {
        ByteArray(HEX.length / 2) { i -> HEX.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
