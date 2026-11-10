package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.MovieReviewsData
import com.example.cineone.domain.model.Review
import com.example.cineone.domain.model.ReviewEligibility
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getMovieReviews(movieId: Long): Flow<Resource<MovieReviewsData>>
    fun getMyReviews(): Flow<Resource<List<Review>>>
    fun checkEligibility(movieId: Long): Flow<Resource<ReviewEligibility>>
    fun createReview(movieId: Long, rating: Int, content: String): Flow<Resource<Review>>
}

