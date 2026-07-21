package com.autoreply.bot.admin

import androidx.compose.runtime.Composable
import com.autoreply.bot.ui.admin.AdminGenerateScreen

/**
 * Variante "owner": expone la pantalla real de generacion de codigos.
 * Contraparte no-op en `src/client/.../admin/AdminTools.kt`.
 */
object AdminTools {
    const val AVAILABLE = true

    @Composable
    fun Screen() {
        AdminGenerateScreen()
    }
}
