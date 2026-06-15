package com.utopia.finance.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) {
    init {
        require(minorUnits >= 0) { "Money cannot be negative; use transaction type for direction." }
    }

    fun format(locale: Locale = Locale.CHINA): String {
        val major = BigDecimal(minorUnits).movePointLeft(2)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = java.util.Currency.getInstance(currency.name)
        return formatter.format(major)
    }

    companion object {
        fun fromMajor(amount: String, currency: CurrencyCode): Money {
            val minor = BigDecimal(amount.trim())
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
            return Money(minor, currency)
        }
    }
}

data class SignedMoney(
    val minorUnits: Long,
    val currency: CurrencyCode,
)
