package com.autoreply.bot.admin

import androidx.compose.runtime.Composable

/**
 * Variante "client" (la que reciben los inquilinos): no incluye la pantalla
 * de generacion de codigos. [Screen] nunca se monta (ver MainActivity, que la
 * oculta con [AVAILABLE]); existe solo para que el codigo compartido compile.
 * La contraparte real esta en `src/owner/.../admin/AdminTools.kt`.
 */
object AdminTools {
    const val AVAILABLE = false

    @Composable
    fun Screen() {
    }
}
