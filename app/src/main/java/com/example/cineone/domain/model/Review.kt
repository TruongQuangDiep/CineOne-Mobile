package com.example.cineone.domain.model

data class Review(
    val reviewId: Long = 0L,
    val userId: Long? = null,
    val userName: String = "Khán giả CineOne",
    val userEmail: String? = null,
    val rating: Int = 5,
    val content: String = "",
    val createdAt: String = "",
    val movieId: Long? = null,
    val movieTitle: String = "",
    val posterUrl: String? = null,
    val status: String = "VISIBLE"
)

data class MovieReviewsData(
    val movieId: Long = 0L,
    val movieTitle: String = "",
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val reviews: List<Review> = emptyList()
)

data class ReviewEligibility(
    val eligible: Boolean = false,
    val alreadyReviewed: Boolean = false,
    val existingRating: Int? = null,
    val existingContent: String? = null,
    val message: String = ""
)

