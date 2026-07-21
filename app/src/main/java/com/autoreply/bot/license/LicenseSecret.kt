package com.autoreply.bot.license

/**
 * Clave secreta compartida con el generador de codigos offline
 * (`tools/generate_license.py`). Debe coincidir EXACTAMENTE en ambos lados o
 * ningun codigo generado sera valido.
 *
 * Si vas a distribuir el codigo fuente de la app (no solo el APK), genera una
 * clave nueva con `python3 -c "import secrets; print(secrets.token_hex(32))"`
 * y actualizala aqui y en `tools/generate_license.py`.
 */
internal object LicenseSecret {
    const val HEX = "8430ee64d95fc201eec30053e36a007919ff236a16e4733c0c80d11351aad6b6"

    val bytes: ByteArray by lazy {
        ByteArray(HEX.length / 2) { i ->
            HEX.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
