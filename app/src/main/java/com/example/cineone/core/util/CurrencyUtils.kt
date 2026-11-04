package com.example.cineone.core.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val viLocale = Locale.forLanguageTag("vi-VN")

    fun formatVnd(amount: Number): String {
        val number = amount.toLong()
        val formatted = NumberFormat.getNumberInstance(viLocale).format(number)
        return "${formatted}đ"
    }
}

