package com.example.cineone.data.mapper

import com.example.cineone.data.dto.review.MovieReviewItemDto
import com.example.cineone.data.dto.review.MovieReviewsResponseDto
import com.example.cineone.data.dto.review.MyReviewItemDto
import com.example.cineone.data.dto.review.ReviewEligibilityDto
import com.example.cineone.domain.model.MovieReviewsData
import com.example.cineone.domain.model.Review
import com.example.cineone.domain.model.ReviewEligibility

fun MovieReviewItemDto.toDomain(): Review = Review(
    reviewId = reviewId,
    userId = userId,
    userName = userName?.ifBlank { "Khán giả CineOne" } ?: "Khán giả CineOne",
    userEmail = userEmail,
    rating = rating,
    content = content ?: "",
    createdAt = createdAt ?: "",
    status = "VISIBLE"
)

fun MyReviewItemDto.toDomain(): Review = Review(
    reviewId = reviewId,
    userId = null,
    userName = "Tôi",
    rating = rating,
    content = content ?: "",
    createdAt = createdAt ?: "",
    movieId = movieId,
    movieTitle = movieTitle ?: "Phim không xác định",
    posterUrl = posterUrl,
    status = status ?: "VISIBLE"
)

fun MovieReviewsResponseDto.toDomain(): MovieReviewsData = MovieReviewsData(
    movieId = movieId,
    movieTitle = movieTitle ?: "",
    averageRating = averageRating,
    reviewCount = reviewCount,
    reviews = reviews.map { it.toDomain() }
)

fun ReviewEligibilityDto.toDomain(): ReviewEligibility = ReviewEligibility(
    eligible = eligible,
    alreadyReviewed = alreadyReviewed,
    existingRating = existingRating,
    existingContent = existingContent,
    message = message ?: ""
)

