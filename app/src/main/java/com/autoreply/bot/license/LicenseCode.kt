package com.autoreply.bot.license

import com.autoreply.bot.domain.model.LicensePlan
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Codigo de activacion offline: sin servidor, firmado con HMAC-SHA256 y
 * ligado al dispositivo (hash corto del Android ID).
 *
 * Formato de los bytes firmados (7 bytes de carga + 5 bytes de firma = 12):
 *   [0..3]  hash del dispositivo (4 bytes)
 *   [4..5]  dia de vencimiento, epoch day, unsigned 16 bits big-endian
 *   [6]     plan (0 = demo, 1 = alquiler)
 *   [7..11] HMAC-SHA256(secreto, bytes[0..6]) truncado a 5 bytes
 *
 * Los 12 bytes se codifican en Base32 Crockford para que el codigo sea corto
 * y facil de transcribir a mano. El mismo esquema esta implementado en
 * `tools/generate_license.py` (que es quien genera los codigos reales).
 */
object LicenseCode {

    private const val PAYLOAD_SIZE = 7
    private const val MAC_SIZE = 5

    data class Parsed(
        val deviceHash: ByteArray,
        val expiresEpochDay: Long,
        val plan: LicensePlan
    )

    /** Verifica la firma y, si es valida, devuelve los datos codificados. Null si el codigo es invalido. */
    fun verifyAndParse(code: String): Parsed? {
        val bytes = Base32Crockford.decode(code) ?: return null
        if (bytes.size < PAYLOAD_SIZE + MAC_SIZE) return null

        val payload = bytes.copyOfRange(0, PAYLOAD_SIZE)
        val mac = bytes.copyOfRange(PAYLOAD_SIZE, PAYLOAD_SIZE + MAC_SIZE)
        val expectedMac = hmac(payload).copyOf(MAC_SIZE)
        if (!constantTimeEquals(mac, expectedMac)) return null

        val deviceHash = payload.copyOfRange(0, 4)
        val expiresEpochDay = (((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)).toLong()
        val plan = LicensePlan.fromWireValue(payload[6].toInt() and 0xFF) ?: return null

        return Parsed(deviceHash, expiresEpochDay, plan)
    }

    private fun hmac(payload: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(LicenseSecret.bytes, "HmacSHA256"))
        return mac.doFinal(payload)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
