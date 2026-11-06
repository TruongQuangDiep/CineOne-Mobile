package com.example.cineone.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.cineone.domain.model.Cinema
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class TimeSlotFilter(
    val label: String,
    val startHour: Int,
    val endHour: Int
) {
    ALL("Tất cả", 0, 24),
    SLOT_8_12("8:00 - 12:00", 8, 12),
    SLOT_12_17("12:00 - 17:00", 12, 17),
    SLOT_17_21("17:00 - 21:00", 17, 21),
    SLOT_21_24("21:00 - 24:00", 21, 24);

    fun matches(isoDateTime: String?): Boolean {
        if (this == ALL) return true
        val timeText = DateTimeUtils.formatTime(isoDateTime)
        val parts = timeText.split(":")
        if (parts.size < 2) return false
        val hour = parts[0].trim().toIntOrNull() ?: return false
        val minute = parts[1].trim().take(2).toIntOrNull() ?: 0
        val totalMinutes = hour * 60 + minute
        val minMinutes = startHour * 60
        val maxMinutes = endHour * 60
        return totalMinutes in minMinutes..maxMinutes
    }

    companion object {
        fun fromCode(code: String?): TimeSlotFilter {
            if (code.isNullOrBlank()) return ALL
            return entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) } ?: ALL
        }
    }
}

enum class LocationFilterMode {
    NONE,
    PROVINCE,
    CURRENT_LOCATION,
    HOME_LOCATION;

    companion object {
        fun fromCode(code: String?): LocationFilterMode {
            if (code.isNullOrBlank()) return NONE
            return entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) } ?: NONE
        }
    }
}

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

object LocationUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_KM * c
    }

    fun formatDistanceKm(distanceKm: Double?): String {
        if (distanceKm == null || distanceKm.isNaN() || distanceKm < 0.0) return ""
        return String.format(Locale.US, "%.1fkm", distanceKm)
    }

    fun extractAreaBracket(cinema: Cinema): String {
        val addr = cinema.address.trim()
        if (addr.isNotBlank()) {
            val parts = addr.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val district = parts.firstOrNull {
                val lower = normalizeSearchText(it)
                lower.startsWith("quan ") ||
                    lower.startsWith("q.") ||
                    lower.startsWith("huyen ") ||
                    lower.startsWith("tp.") ||
                    lower.startsWith("tp ") ||
                    lower.contains("go vap") ||
                    lower.contains("tan binh") ||
                    lower.contains("binh thanh") ||
                    lower.contains("thu duc") ||
                    lower.contains("phu nhuan") ||
                    lower.contains("cau giay") ||
                    lower.contains("dong da") ||
                    lower.contains("hai chau")
            } ?: parts.getOrNull(parts.size - 2)
            if (!district.isNullOrBlank()) {
                return district
                    .replace(Regex("^(Quận|Huyện)\\s+", RegexOption.IGNORE_CASE), "")
                    .trim()
            }
        }
        return cinema.city.trim()
    }

    fun normalizeSearchText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
    }

    fun normalizeCityKey(input: String?): String {
        val base = normalizeSearchText(input)
            .replace(Regex("[.,\\-_/]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (base.isBlank()) return ""
        val withoutPrefix = base
            .replace(Regex("^(tp|thanh pho|tinh)\\s+"), "")
            .trim()
        return when (withoutPrefix) {
            "hcm", "tphcm", "tp hcm", "sai gon", "saigon" -> "ho chi minh"
            "hn", "hanoi" -> "ha noi"
            "brvt", "vung tau", "ba ria" -> "ba ria vung tau"
            "hue" -> "thua thien hue"
            else -> withoutPrefix
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchDeviceLocation(context: Context): Pair<Double, Double>? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
                if (loc != null) {
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }
            bestLocation?.let { Pair(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    fun resolveAddressCoordinates(
        context: Context?,
        address: String?,
        cinemas: List<Cinema>
    ): Pair<Double, Double>? {
        val raw = address?.trim().orEmpty()
        if (raw.isBlank()) return null

        // 1. Nếu chuỗi địa chỉ có chứa sẵn tọa độ "lat, lon"
        val coordParts = raw.split(",")
        if (coordParts.size == 2) {
            val lat = coordParts[0].trim().toDoubleOrNull()
            val lon = coordParts[1].trim().toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return Pair(lat, lon)
            }
        }

        // 2. Sử dụng Android Geocoder nếu khả dụng trên thiết bị
        if (context != null && Geocoder.isPresent()) {
            val geocoded = runCatching {
                val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
                val results = geocoder.getFromLocationName(raw, 1)
                val first = results?.firstOrNull()
                if (first != null) Pair(first.latitude, first.longitude) else null
            }.getOrNull()
            if (geocoded != null) return geocoded
        }

        // 3. Đối chiếu với tọa độ thực của các rạp trong cùng khu vực / thành phố từ DB
        val normalizedAddr = normalizeSearchText(raw)
        val cinemasWithCoords = cinemas.filter { it.latitude != null && it.longitude != null }
        if (cinemasWithCoords.isNotEmpty()) {
            val bestCinema = cinemasWithCoords.maxByOrNull { cinema ->
                val cinemaAddrTokens = normalizeSearchText("${cinema.address} ${cinema.city}")
                    .split(Regex("[,\\s]+"))
                    .filter { it.length >= 3 }
                cinemaAddrTokens.count { token -> normalizedAddr.contains(token) }
            }
            if (bestCinema != null) {
                val matchCount = normalizeSearchText("${bestCinema.address} ${bestCinema.city}")
                    .split(Regex("[,\\s]+"))
                    .filter { it.length >= 3 }
                    .count { token -> normalizedAddr.contains(token) }
                if (matchCount > 0) {
                    return Pair(bestCinema.latitude!!, bestCinema.longitude!!)
                }
            }
        }

        return null
    }
}
