package com.example.cineone.data.mapper

import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.data.dto.ai.AiMessageDto
import com.example.cineone.data.dto.ai.AiSearchCriteriaResponseDto
import com.example.cineone.data.dto.ai.FaqAnswerResponseDto
import com.example.cineone.data.dto.ai.FaqSourceDto
import com.example.cineone.data.dto.ai.ReviewSummaryResponseDto
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiFaqSource
import com.example.cineone.domain.model.AiMessage
import com.example.cineone.domain.model.AiReviewSummary
import com.example.cineone.domain.model.AiSender
import com.example.cineone.domain.model.Movie

object AiMessageMapper {

    fun toDomain(dto: AiMessageDto): AiMessage {
        val sender = if (dto.sender.equals("USER", ignoreCase = true)) {
            AiSender.USER
        } else {
            AiSender.ASSISTANT
        }
        return AiMessage(
            id = dto.id,
            sender = sender,
            content = dto.content,
            timestamp = dto.timestamp
        )
    }

    fun mapFaqSources(dto: FaqAnswerResponseDto): List<AiFaqSource> {
        return dto.sources.map(::mapFaqSource)
    }

    private fun mapFaqSource(dto: FaqSourceDto): AiFaqSource {
        return AiFaqSource(
            title = dto.title.trim().ifBlank { "Chính sách CineOne" },
            category = dto.category.trim().ifBlank { "FAQ" }
        )
    }

    fun mapReviewSummary(
        dto: ReviewSummaryResponseDto,
        fallbackMovie: Movie? = null
    ): AiReviewSummary {
        val resolvedId = dto.movieId ?: fallbackMovie?.movieId ?: 0L
        val resolvedTitle = dto.movieTitle?.takeIf { it.isNotBlank() }
            ?: fallbackMovie?.title
            ?: "Phim"
        val resolvedRating = dto.averageRating?.takeIf { it > 0.0 }
            ?: fallbackMovie?.rating
            ?: 0.0
        val resolvedSummary = dto.summary?.takeIf { it.isNotBlank() }
            ?: "Hiện chưa có đủ dữ liệu bình luận để tổng hợp nhận xét chi tiết cho phim này."
        return AiReviewSummary(
            movieId = resolvedId,
            movieTitle = resolvedTitle,
            reviewCount = dto.reviewCount ?: 0,
            averageRating = resolvedRating,
            summary = resolvedSummary,
            highlights = dto.highlights.orEmpty().filter { it.isNotBlank() },
            movie = fallbackMovie
        )
    }

    fun mapSearchCriteria(dto: AiSearchCriteriaResponseDto): AiExtractedCriteria {
        val slot = mapTimeRangeToSlot(dto.timeRange?.from, dto.timeRange?.to)
        val locMode = when (dto.location?.mode?.uppercase()) {
            "NEARBY", "CURRENT_LOCATION", "GPS" -> LocationFilterMode.CURRENT_LOCATION
            "HOME", "HOME_LOCATION", "SAVED_ADDRESS" -> LocationFilterMode.HOME_LOCATION
            "AREA", "CITY", "PROVINCE" -> LocationFilterMode.PROVINCE
            else -> LocationFilterMode.NONE
        }
        val city = if (locMode == LocationFilterMode.PROVINCE) {
            dto.location?.value.orEmpty()
        } else {
            ""
        }
        val dateLabel = dto.date?.takeIf { it.isNotBlank() }?.let { DateTimeUtils.formatReleaseDate(it) }
        val timeLabel = if (!dto.timeRange?.from.isNullOrBlank() && !dto.timeRange?.to.isNullOrBlank()) {
            "${dto.timeRange?.from} - ${dto.timeRange?.to}"
        } else if (slot != TimeSlotFilter.ALL) {
            slot.label
        } else {
            null
        }
        return AiExtractedCriteria(
            movieTitle = dto.movieTitle?.trim()?.ifBlank { null },
            genre = dto.genre?.trim()?.ifBlank { null },
            date = dto.date?.trim()?.ifBlank { null },
            dateLabel = dateLabel,
            timeSlot = slot,
            timeRangeLabel = timeLabel,
            locationMode = locMode,
            city = city,
            brand = dto.brand?.trim().orEmpty(),
            cinemaName = dto.cinema?.trim().orEmpty(),
            requiresClarification = dto.requiresClarification,
            clarificationQuestion = dto.clarificationQuestion?.trim()?.ifBlank { null }
        )
    }

    private fun mapTimeRangeToSlot(from: String?, to: String?): TimeSlotFilter {
        if (from.isNullOrBlank()) return TimeSlotFilter.ALL
        val hour = from.substringBefore(":").toIntOrNull() ?: return TimeSlotFilter.ALL
        return when {
            hour in 6..11 -> TimeSlotFilter.SLOT_8_12
            hour in 12..16 -> TimeSlotFilter.SLOT_12_17
            hour in 17..20 -> TimeSlotFilter.SLOT_17_21
            hour >= 21 || hour < 6 -> TimeSlotFilter.SLOT_21_24
            else -> TimeSlotFilter.ALL
        }
    }
}
