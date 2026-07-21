package com.autoreply.bot.domain.model

/** Tipo de plan codificado dentro del codigo de activacion. */
enum class LicensePlan(val wireValue: Int) {
    DEMO(0),
    RENTAL(1);

    companion object {
        fun fromWireValue(value: Int): LicensePlan? = entries.find { it.wireValue == value }
    }
}
