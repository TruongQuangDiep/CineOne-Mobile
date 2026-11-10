package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Review
import com.example.cineone.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow

class CreateReviewUseCase(private val reviewRepository: ReviewRepository) {
    operator fun invoke(movieId: Long, rating: Int, content: String): Flow<Resource<Review>> {
        return reviewRepository.createReview(movieId, rating, content)
    }
}

