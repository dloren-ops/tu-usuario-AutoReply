package com.autoreply.bot.license

/**
 * Codificacion Base32 (variante Crockford) sin relleno. Se usa para que los
 * codigos de activacion sean cortos y faciles de transcribir a mano.
 */
object Base32Crockford {

    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder()
        var bitBuffer = 0L
        var bitCount = 0
        for (b in bytes) {
            bitBuffer = (bitBuffer shl 8) or (b.toLong() and 0xFF)
            bitCount += 8
            while (bitCount >= 5) {
                bitCount -= 5
                val index = ((bitBuffer ushr bitCount) and 0x1F).toInt()
                out.append(ALPHABET[index])
            }
        }
        if (bitCount > 0) {
            val index = ((bitBuffer shl (5 - bitCount)) and 0x1F).toInt()
            out.append(ALPHABET[index])
        }
        return out.toString()
    }

    /** Decodifica; devuelve null si el texto contiene caracteres invalidos. */
    fun decode(text: String): ByteArray? {
        val normalized = normalize(text)
        val out = ArrayList<Byte>()
        var bitBuffer = 0L
        var bitCount = 0
        for (ch in normalized) {
            val value = ALPHABET.indexOf(ch)
            if (value < 0) return null
            bitBuffer = (bitBuffer shl 5) or value.toLong()
            bitCount += 5
            if (bitCount >= 8) {
                bitCount -= 8
                out.add(((bitBuffer ushr bitCount) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    /** Quita separadores y normaliza confusiones tipograficas comunes. */
    private fun normalize(text: String): String {
        return text
            .uppercase()
            .filterNot { it == '-' || it.isWhitespace() }
            .map { ch ->
                when (ch) {
                    'O' -> '0'
                    'I', 'L' -> '1'
                    else -> ch
                }
            }
            .joinToString("")
    }
}
