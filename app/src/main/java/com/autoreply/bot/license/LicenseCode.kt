package com.autoreply.bot.license

import com.autoreply.bot.domain.model.LicensePlan
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Codigo de activacion offline: sin servidor, firmado con ECDSA P-256 y
 * ligado al dispositivo (hash corto del Android ID).
 *
 * Bytes firmados (7 bytes de carga):
 *   [0..3]  hash del dispositivo (4 bytes)
 *   [4..5]  dia de vencimiento, epoch day, unsigned 16 bits big-endian
 *   [6]     plan (0 = demo, 1 = alquiler)
 *
 * El codigo final son esos 7 bytes + la firma en formato "raw" fijo de 64
 * bytes (r||s, 32 bytes cada uno), codificados en Base32 Crockford.
 *
 * A diferencia de un HMAC, una firma no se puede truncar para acortar el
 * codigo sin invalidarla -> el codigo queda mas largo que en un esquema
 * simetrico, pero a cambio la clave que viaja en la app (client Y owner)
 * es PUBLICA: no sirve para fabricar codigos nuevos, solo para verificarlos.
 * Solo quien tiene la clave PRIVADA (tu computadora, via
 * `tools/generate_license.py`, y la variante "owner" de la app) puede
 * generar codigos validos.
 */
object LicenseCode {

    private const val PAYLOAD_SIZE = 7
    private const val SIGNATURE_SIZE = 64
    private const val CURVE_NAME = "secp256r1"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

    data class Parsed(
        val deviceHash: ByteArray,
        val expiresEpochDay: Long,
        val plan: LicensePlan
    )

    /**
     * Firma y genera un codigo formateado en grupos de 5 caracteres. Requiere
     * la clave PRIVADA como parametro: este archivo (en `main`, compartido
     * por ambas variantes) nunca la embebe. Solo la llama la pantalla admin
     * de la variante "owner", pasandole `LicensePrivateKey.bytes` (que ese
     * si es un archivo exclusivo de esa variante).
     */
    fun generate(
        deviceHash: ByteArray,
        expiresEpochDay: Long,
        plan: LicensePlan,
        privateKeyPkcs8: ByteArray
    ): String {
        val payload = buildPayload(deviceHash, expiresEpochDay, plan)
        val privateKey = decodePrivateKey(privateKeyPkcs8)
        val derSignature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(privateKey)
            update(payload)
            sign()
        }
        val raw = derToRaw(derSignature)
        val encoded = Base32Crockford.encode(payload + raw)
        return encoded.chunked(5).joinToString("-")
    }

    /** Verifica la firma (con la clave PUBLICA) y, si es valida, devuelve los datos. Null si el codigo es invalido. */
    fun verifyAndParse(code: String): Parsed? {
        val bytes = Base32Crockford.decode(code) ?: return null
        if (bytes.size < PAYLOAD_SIZE + SIGNATURE_SIZE) return null

        val payload = bytes.copyOfRange(0, PAYLOAD_SIZE)
        val rawSignature = bytes.copyOfRange(PAYLOAD_SIZE, PAYLOAD_SIZE + SIGNATURE_SIZE)

        val valid = try {
            val derSignature = rawToDer(rawSignature)
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(publicKey)
                update(payload)
                verify(derSignature)
            }
        } catch (_: Exception) {
            false
        }
        if (!valid) return null

        val deviceHash = payload.copyOfRange(0, 4)
        val expiresEpochDay = (((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)).toLong()
        val plan = LicensePlan.fromWireValue(payload[6].toInt() and 0xFF) ?: return null

        return Parsed(deviceHash, expiresEpochDay, plan)
    }

    private fun buildPayload(deviceHash: ByteArray, expiresEpochDay: Long, plan: LicensePlan): ByteArray {
        require(deviceHash.size == 4) { "deviceHash debe tener 4 bytes" }
        require(expiresEpochDay in 0..0xFFFF) { "expiresEpochDay fuera de rango (16 bits)" }
        val payload = ByteArray(PAYLOAD_SIZE)
        deviceHash.copyInto(payload, 0)
        payload[4] = ((expiresEpochDay shr 8) and 0xFF).toByte()
        payload[5] = (expiresEpochDay and 0xFF).toByte()
        payload[6] = plan.wireValue.toByte()
        return payload
    }

    private val publicKey: PublicKey by lazy { decodePublicKey(LicensePublicKey.bytes) }

    private fun decodePublicKey(uncompressedPoint: ByteArray): PublicKey {
        require(uncompressedPoint.size == 65 && uncompressedPoint[0] == 0x04.toByte()) {
            "Formato de clave publica invalido"
        }
        val x = BigInteger(1, uncompressedPoint.copyOfRange(1, 33))
        val y = BigInteger(1, uncompressedPoint.copyOfRange(33, 65))
        val spec = ECPublicKeySpec(ECPoint(x, y), ecParameterSpec())
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    private fun decodePrivateKey(pkcs8: ByteArray): PrivateKey =
        KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8))

    private fun ecParameterSpec(): ECParameterSpec {
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec(CURVE_NAME))
        return params.getParameterSpec(ECParameterSpec::class.java)
    }

    /** raw fijo de 64 bytes (r||s) -> DER ASN.1 SEQUENCE{INTEGER r, INTEGER s}, lo que espera [Signature]. */
    private fun rawToDer(raw: ByteArray): ByteArray {
        require(raw.size == SIGNATURE_SIZE)
        val rEnc = encodeDerInteger(raw.copyOfRange(0, 32))
        val sEnc = encodeDerInteger(raw.copyOfRange(32, 64))
        val body = rEnc + sEnc
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    /** DER ASN.1 SEQUENCE{INTEGER r, INTEGER s} (lo que produce [Signature]) -> raw fijo de 64 bytes (r||s). */
    private fun derToRaw(der: ByteArray): ByteArray {
        require(der.size >= 8 && der[0] == 0x30.toByte()) { "Firma DER invalida" }
        var offset = 2
        require(der[offset] == 0x02.toByte()) { "Firma DER invalida (r)" }
        offset++
        val rLen = der[offset].toInt() and 0xFF
        offset++
        val r = der.copyOfRange(offset, offset + rLen)
        offset += rLen
        require(der[offset] == 0x02.toByte()) { "Firma DER invalida (s)" }
        offset++
        val sLen = der[offset].toInt() and 0xFF
        offset++
        val s = der.copyOfRange(offset, offset + sLen)
        return toFixedLength(r, 32) + toFixedLength(s, 32)
    }

    private fun encodeDerInteger(bytes: ByteArray): ByteArray {
        var i = 0
        while (i < bytes.size - 1 && bytes[i] == 0.toByte()) i++
        var trimmed = bytes.copyOfRange(i, bytes.size)
        if (trimmed[0].toInt() and 0x80 != 0) trimmed = byteArrayOf(0) + trimmed
        return byteArrayOf(0x02, trimmed.size.toByte()) + trimmed
    }

    private fun toFixedLength(bytes: ByteArray, length: Int): ByteArray {
        var trimmed = bytes
        while (trimmed.size > length && trimmed[0] == 0.toByte()) trimmed = trimmed.copyOfRange(1, trimmed.size)
        require(trimmed.size <= length) { "Componente de firma mas largo de lo esperado" }
        return ByteArray(length - trimmed.size) + trimmed
    }
}
