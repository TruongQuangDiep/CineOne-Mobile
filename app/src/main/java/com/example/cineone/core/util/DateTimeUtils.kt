package com.example.cineone.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val isoDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")

    fun formatReleaseDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "Đang cập nhật"
        return try {
            val clean = raw.trim().take(10)
            LocalDate.parse(clean, isoDateFormatter).format(displayDateFormatter)
        } catch (_: Exception) {
            raw
        }
    }

    fun formatTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return ""
        return try {
            val dt = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            if (isoDateTime.length >= 16 && isoDateTime.contains("T")) {
                isoDateTime.substring(11, 16)
            } else {
                isoDateTime
            }
        }
    }
}

