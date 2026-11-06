package com.example.cineone.domain.model

import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.TimeSlotFilter

enum class AiSender {
    USER,
    ASSISTANT
}

enum class AiIntentType {
    SEARCH_SHOWTIME_OR_MOVIE,
    REVIEW_SUMMARY,
    FAQ_POLICY,
    CLARIFICATION,
    GREETING_HELP
}

data class AiExtractedCriteria(
    val movieTitle: String? = null,
    val genre: String? = null,
    val date: String? = null,
    val dateLabel: String? = null,
    val timeSlot: TimeSlotFilter = TimeSlotFilter.ALL,
    val timeRangeLabel: String? = null,
    val locationMode: LocationFilterMode = LocationFilterMode.NONE,
    val city: String = "",
    val locationLabel: String? = null,
    val brand: String = "",
    val cinemaName: String = "",
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null
) {
    fun hasAnyFilter(): Boolean {
        return !movieTitle.isNullOrBlank() ||
            !genre.isNullOrBlank() ||
            !date.isNullOrBlank() ||
            timeSlot != TimeSlotFilter.ALL ||
            locationMode != LocationFilterMode.NONE ||
            city.isNotBlank() ||
            brand.isNotBlank() ||
            cinemaName.isNotBlank()
    }

    fun activeFilterTags(): List<String> {
        val tags = mutableListOf<String>()
        if (!movieTitle.isNullOrBlank()) tags.add("Phim: $movieTitle")
        if (!genre.isNullOrBlank()) tags.add("Thể loại: $genre")
        if (!dateLabel.isNullOrBlank()) {
            tags.add("Ngày: $dateLabel")
        } else if (!date.isNullOrBlank()) {
            tags.add("Ngày: $date")
        }
        if (timeSlot != TimeSlotFilter.ALL) {
            tags.add("Giờ: ${timeRangeLabel ?: timeSlot.label}")
        } else if (!timeRangeLabel.isNullOrBlank()) {
            tags.add("Giờ: $timeRangeLabel")
        }
        if (cinemaName.isNotBlank()) {
            tags.add("Rạp: $cinemaName")
        } else if (brand.isNotBlank()) {
            tags.add("Hãng rạp: $brand")
        }
        when (locationMode) {
            LocationFilterMode.CURRENT_LOCATION -> tags.add(locationLabel ?: "Gần vị trí hiện tại")
            LocationFilterMode.HOME_LOCATION -> tags.add(locationLabel ?: "Gần vị trí nhà tôi")
            LocationFilterMode.PROVINCE -> if (city.isNotBlank()) tags.add("Khu vực: $city")
            LocationFilterMode.NONE -> if (city.isNotBlank()) tags.add("Khu vực: $city")
        }
        return tags
    }

    fun mergeWith(newer: AiExtractedCriteria): AiExtractedCriteria {
        val resolvedLocationMode = if (newer.locationMode != LocationFilterMode.NONE) {
            newer.locationMode
        } else {
            this.locationMode
        }
        val resolvedCity = when {
            newer.city.isNotBlank() -> newer.city
            newer.locationMode == LocationFilterMode.CURRENT_LOCATION ||
                newer.locationMode == LocationFilterMode.HOME_LOCATION -> ""
            else -> this.city
        }
        val resolvedLocationLabel = when {
            !newer.locationLabel.isNullOrBlank() -> newer.locationLabel
            newer.locationMode != LocationFilterMode.NONE -> null
            else -> this.locationLabel
        }
        return AiExtractedCriteria(
            movieTitle = newer.movieTitle?.takeIf { it.isNotBlank() } ?: this.movieTitle,
            genre = newer.genre?.takeIf { it.isNotBlank() } ?: this.genre,
            date = newer.date?.takeIf { it.isNotBlank() } ?: this.date,
            dateLabel = newer.dateLabel?.takeIf { it.isNotBlank() } ?: this.dateLabel,
            timeSlot = if (newer.timeSlot != TimeSlotFilter.ALL) newer.timeSlot else this.timeSlot,
            timeRangeLabel = newer.timeRangeLabel?.takeIf { it.isNotBlank() } ?: this.timeRangeLabel,
            locationMode = resolvedLocationMode,
            city = resolvedCity,
            locationLabel = resolvedLocationLabel,
            brand = newer.brand.ifBlank { this.brand },
            cinemaName = newer.cinemaName.ifBlank { this.cinemaName },
            requiresClarification = newer.requiresClarification,
            clarificationQuestion = newer.clarificationQuestion
        )
    }
}

data class AiClarificationOption(
    val label: String,
    val queryToSend: String
)

data class AiFaqSource(
    val title: String,
    val category: String
)

data class AiReviewSummary(
    val movieId: Long,
    val movieTitle: String,
    val reviewCount: Int,
    val averageRating: Double,
    val summary: String,
    val highlights: List<String>,
    val movie: Movie? = null
)

data class AiMatchedCinemaShowtime(
    val cinemaId: Long,
    val cinemaName: String,
    val brandName: String,
    val address: String,
    val city: String,
    val distanceKm: Double? = null,
    val showtimes: List<Showtime> = emptyList()
)

data class AiSuggestedMovieCard(
    val movie: Movie,
    val matchedCinemas: List<AiMatchedCinemaShowtime> = emptyList(),
    val appliedCriteria: AiExtractedCriteria = AiExtractedCriteria()
)

data class AiMessage(
    val id: String,
    val sender: AiSender,
    val content: String,
    val timestamp: String,
    val intentType: AiIntentType = AiIntentType.SEARCH_SHOWTIME_OR_MOVIE,
    val extractedCriteria: AiExtractedCriteria? = null,
    val suggestedMovies: List<AiSuggestedMovieCard> = emptyList(),
    val matchedCinemasOnly: List<AiMatchedCinemaShowtime> = emptyList(),
    val reviewSummary: AiReviewSummary? = null,
    val faqSources: List<AiFaqSource> = emptyList(),
    val clarificationOptions: List<AiClarificationOption> = emptyList()
)

