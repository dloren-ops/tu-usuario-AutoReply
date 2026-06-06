package com.autoreply.bot.update

/**
 * Comparacion de versiones tipo "1.2.3" sin dependencias externas.
 */
object VersionUtils {

    /** Quita prefijos como "v" y espacios. */
    fun normalize(version: String): String =
        version.trim().removePrefix("v").removePrefix("V").trim()

    /**
     * Devuelve true si [remote] es estrictamente mayor que [current].
     * Compara numericamente segmento a segmento (1.10 > 1.9).
     */
    fun isNewer(remote: String, current: String): Boolean {
        val r = parse(normalize(remote))
        val c = parse(normalize(current))
        val max = maxOf(r.size, c.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    private fun parse(version: String): List<Int> =
        version.split(".", "-", "_")
            .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }
}
