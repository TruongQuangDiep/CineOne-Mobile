package com.example.cineone.data.dto.review

import kotlinx.serialization.Serializable

@Serializable
data class CreateReviewRequest(
    val movieId: Long,
    val rating: Int,
    val content: String = ""
)

@Serializable
data class MovieReviewItemDto(
    val reviewId: Long = 0L,
    val userId: Long? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val rating: Int = 5,
    val content: String? = null,
    val createdAt: String? = null
)

@Serializable
data class MovieReviewsResponseDto(
    val movieId: Long = 0L,
    val movieTitle: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val reviews: List<MovieReviewItemDto> = emptyList()
)

@Serializable
data class MyReviewItemDto(
    val reviewId: Long = 0L,
    val movieId: Long? = null,
    val movieTitle: String? = null,
    val posterUrl: String? = null,
    val rating: Int = 5,
    val content: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ReviewEligibilityDto(
    val eligible: Boolean = false,
    val alreadyReviewed: Boolean = false,
    val existingRating: Int? = null,
    val existingContent: String? = null,
    val message: String? = null
)

@Serializable
data class ReviewDto(
    val id: String? = null,
    val movieId: String? = null,
    val userId: String? = null,
    val rating: Int? = null,
    val comment: String? = null
)
