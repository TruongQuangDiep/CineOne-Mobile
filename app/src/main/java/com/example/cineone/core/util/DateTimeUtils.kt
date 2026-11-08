package com.example.cineone.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import com.example.cineone.core.auth.SessionManager
import com.example.cineone.domain.model.Cinema
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

    // Bảng tọa độ chuẩn cho các quận/huyện/tỉnh thành phổ biến để bản đồ luôn di chuyển chính xác kể cả khi offline
    private val VIETNAM_AREA_COORDS: List<Pair<String, Pair<Double, Double>>> = listOf(
        "tan phong, quan 7" to Pair(10.7295, 106.7068),
        "tan phu, quan 7" to Pair(10.7289, 106.7218),
        "tan quy, quan 7" to Pair(10.7385, 106.7112),
        "tan hung, quan 7" to Pair(10.7431, 106.7019),
        "phu my, quan 7" to Pair(10.7082, 106.7351),
        "nguyen van linh, quan 7" to Pair(10.7292, 106.7055),
        "quan 7" to Pair(10.7340, 106.7216),
        "ben nghe, quan 1" to Pair(10.7797, 106.7019),
        "ben thanh, quan 1" to Pair(10.7725, 106.6980),
        "quan 1" to Pair(10.7756, 106.7004),
        "quan 3" to Pair(10.7844, 106.6844),
        "quan 10" to Pair(10.7746, 106.6670),
        "quan binh thanh" to Pair(10.8106, 106.7091),
        "binh thanh" to Pair(10.8106, 106.7091),
        "quan go vap" to Pair(10.8387, 106.6653),
        "go vap" to Pair(10.8387, 106.6653),
        "quan tan binh" to Pair(10.8015, 106.6526),
        "tan binh" to Pair(10.8015, 106.6526),
        "thao dien" to Pair(10.8038, 106.7341),
        "thu duc" to Pair(10.8494, 106.7537),
        "quan cau giay" to Pair(21.0362, 105.7906),
        "cau giay" to Pair(21.0362, 105.7906),
        "quan dong da" to Pair(21.0181, 105.8292),
        "dong da" to Pair(21.0181, 105.8292),
        "quan hoan kiem" to Pair(21.0285, 105.8542),
        "hoan kiem" to Pair(21.0285, 105.8542),
        "quan hai ba trung" to Pair(21.0091, 105.8535),
        "quan thanh xuan" to Pair(20.9938, 105.8114),
        "quan hai chau" to Pair(16.0544, 108.2022),
        "quan thanh khe" to Pair(16.0642, 108.1856),
        "quan son tra" to Pair(16.0860, 108.2398),
        "quan ninh kieu" to Pair(10.0342, 105.7805),
        "quan cai rang" to Pair(10.0028, 105.7689),
        "bien hoa" to Pair(10.9574, 106.8427),
        "thu dau mot" to Pair(10.9805, 106.6519),
        "thuan an" to Pair(10.9089, 106.7145),
        "di an" to Pair(10.8920, 106.7683),
        "ho chi minh" to Pair(10.7769, 106.7009),
        "ha noi" to Pair(21.0285, 105.8542),
        "da nang" to Pair(16.0544, 108.2022),
        "can tho" to Pair(10.0452, 105.7469),
        "dong nai" to Pair(10.9574, 106.8427),
        "binh duong" to Pair(10.9805, 106.6519)
    )

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

    /**
     * Tìm tọa độ chính xác cho địa chỉ vừa nhập để di chuyển bản đồ và ghim vị trí:
     * 1. OpenStreetMap Nominatim Geocoding (hỗ trợ tìm đến tận đường/phường/quận tại Việt Nam)
     * 2. Android Geocoder trên thiết bị
     * 3. Bảng tọa độ khu vực Việt Nam & cụm rạp trong DB
     */
    @Suppress("DEPRECATION")
    fun geocodeAddressForMap(
        context: Context?,
        streetDetail: String,
        ward: String,
        district: String,
        province: String,
        cinemas: List<Cinema>
    ): Pair<Double, Double>? {
        val fullAddress = listOf(streetDetail.trim(), ward.trim(), district.trim(), province.trim())
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (fullAddress.isBlank()) return null

        // 1. Thử truy vấn Nominatim OpenStreetMap (từ chi tiết nhất tới cấp phường/quận)
        val queriesToTry = buildList {
            add("$fullAddress, Việt Nam")
            if (streetDetail.isNotBlank() && (district.isNotBlank() || province.isNotBlank())) {
                val withoutWard = listOf(streetDetail.trim(), district.trim(), province.trim())
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                add("$withoutWard, Việt Nam")
            }
            if (ward.isNotBlank() || district.isNotBlank()) {
                val areaOnly = listOf(ward.trim(), district.trim(), province.trim())
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                if (areaOnly.isNotBlank() && areaOnly != fullAddress) {
                    add("$areaOnly, Việt Nam")
                }
            }
        }.distinct()

        for (query in queriesToTry) {
            val nominatimResult = queryNominatimCoordinates(query)
            if (nominatimResult != null) return nominatimResult
        }

        // 2. Sử dụng Android Geocoder nếu khả dụng
        if (context != null && Geocoder.isPresent()) {
            for (query in queriesToTry) {
                val geocoded = runCatching {
                    val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
                    val results = geocoder.getFromLocationName(query, 1)
                    val first = results?.firstOrNull()
                    if (first != null) Pair(first.latitude, first.longitude) else null
                }.getOrNull()
                if (geocoded != null) return geocoded
            }
        }

        // 3. Tra cứu bảng tọa độ quận/huyện/phường tại Việt Nam
        val normFull = normalizeSearchText(fullAddress)
        for ((areaKey, coords) in VIETNAM_AREA_COORDS) {
            if (normFull.contains(areaKey)) {
                return coords
            }
        }

        // 4. Đối chiếu với các rạp trong cùng khu vực từ DB
        return matchCinemaAreaCoordinates(normFull, cinemas)
    }

    private fun queryNominatimCoordinates(query: String): Pair<Double, Double>? {
        return runCatching {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&countrycodes=vn&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "CineOneMobileApp/1.0 (Android)")
                setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9")
            }
            try {
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        val lat = obj.optString("lat").toDoubleOrNull()
                        val lon = obj.optString("lon").toDoubleOrNull()
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            return Pair(lat, lon)
                        }
                    }
                }
                null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun matchCinemaAreaCoordinates(
        normalizedAddr: String,
        cinemas: List<Cinema>
    ): Pair<Double, Double>? {
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

    @Suppress("DEPRECATION")
    fun resolveAddressCoordinates(
        context: Context?,
        address: String?,
        cinemas: List<Cinema>
    ): Pair<Double, Double>? {
        val raw = address?.trim().orEmpty()
        if (raw.isBlank()) return null

        // 0. Ưu tiên lấy tọa độ ghim chính xác mà người dùng đã kéo ghim và lưu trên bản đồ
        if (context != null) {
            val pinned = runCatching {
                SessionManager(context).fetchUserHomeCoordinates(forAddress = raw)
            }.getOrNull()
            if (pinned != null) return pinned
        }

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

        // 3. Thử Nominatim hoặc bảng tọa độ khu vực Việt Nam
        val nominatim = queryNominatimCoordinates("$raw, Việt Nam")
        if (nominatim != null) return nominatim

        val normalizedAddr = normalizeSearchText(raw)
        for ((areaKey, coords) in VIETNAM_AREA_COORDS) {
            if (normalizedAddr.contains(areaKey)) {
                return coords
            }
        }

        // 4. Đối chiếu với tọa độ thực của các rạp trong cùng khu vực / thành phố từ DB
        return matchCinemaAreaCoordinates(normalizedAddr, cinemas)
    }
}
