package com.example.cineone.data.dto.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiMessageDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("sender")
    val sender: String = "ASSISTANT",
    @SerialName("content")
    val content: String = "",
    @SerialName("timestamp")
    val timestamp: String = ""
)

@Serializable
data class FaqQuestionRequestDto(
    @SerialName("question")
    val question: String
)

@Serializable
data class FaqAnswerResponseDto(
    @SerialName("answer")
    val answer: String = "",
    @SerialName("sources")
    val sources: List<FaqSourceDto> = emptyList()
)

@Serializable
data class FaqSourceDto(
    @SerialName("title")
    val title: String = "",
    @SerialName("category")
    val category: String = ""
)

@Serializable
data class ReviewSummaryResponseDto(
    @SerialName("movie_id")
    val movieId: Long? = null,
    @SerialName("movie_title")
    val movieTitle: String? = null,
    @SerialName("review_count")
    val reviewCount: Int? = 0,
    @SerialName("average_rating")
    val averageRating: Double? = 0.0,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("highlights")
    val highlights: List<String>? = emptyList()
)

@Serializable
data class AiSearchCriteriaRequestDto(
    @SerialName("query")
    val query: String
)

@Serializable
data class AiSearchCriteriaResponseDto(
    @SerialName("movieTitle")
    val movieTitle: String? = null,
    @SerialName("genre")
    val genre: String? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("timeRange")
    val timeRange: AiTimeRangeDto? = null,
    @SerialName("location")
    val location: AiLocationDto? = null,
    @SerialName("cinema")
    val cinema: String? = null,
    @SerialName("brand")
    val brand: String? = null,
    @SerialName("requiresClarification")
    val requiresClarification: Boolean = false,
    @SerialName("clarificationQuestion")
    val clarificationQuestion: String? = null,
    @SerialName("confidence")
    val confidence: Double? = null
)

@Serializable
data class AiTimeRangeDto(
    @SerialName("from")
    val from: String? = null,
    @SerialName("to")
    val to: String? = null
)

@Serializable
data class AiLocationDto(
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("value")
    val value: String? = null,
    @SerialName("radiusKm")
    val radiusKm: Int? = null
)
