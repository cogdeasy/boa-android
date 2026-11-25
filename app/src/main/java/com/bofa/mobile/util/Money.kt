package com.bofa.mobile.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Client-side helpers for the platform's decimal-string amounts ("125.40").
 * Mirrors the rounding used by boa-common Money (two places, HALF_EVEN).
 */
object Money {

    @JvmStatic
    fun parse(amount: String?): BigDecimal {
        if (amount.isNullOrBlank()) return BigDecimal.ZERO.setScale(2)
        return BigDecimal(amount.trim().replace(",", "").replace("$", "")).setScale(2, RoundingMode.HALF_EVEN)
    }

    @JvmStatic
    fun toWire(amount: BigDecimal): String = amount.setScale(2, RoundingMode.HALF_EVEN).toPlainString()

    @JvmStatic
    fun format(amount: String?): String = format(parse(amount))

    @JvmStatic
    fun format(amount: BigDecimal): String {
        val nf = NumberFormat.getCurrencyInstance(Locale.US)
        val abs = nf.format(amount.abs())
        return if (amount.signum() < 0) "-$abs" else abs
    }

    @JvmStatic
    fun sum(amounts: List<String?>): BigDecimal =
        amounts.fold(BigDecimal.ZERO.setScale(2)) { acc, a -> acc.add(parse(a)) }

    @JvmStatic
    fun isPositive(amount: String?): Boolean = try {
        parse(amount).signum() > 0
    } catch (e: NumberFormatException) {
        false
    }
}
