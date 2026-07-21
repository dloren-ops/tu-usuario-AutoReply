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
    const val HEX = "04471f17383c6f90a7348fe0227a0a06af8e701c328f25912546c23ddc3d2c2fc912a475ece06cf9a1f39dd0314227861c68241541d36935b1fc70bc28396cb837"

    val bytes: ByteArray by lazy {
        ByteArray(HEX.length / 2) { i -> HEX.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
