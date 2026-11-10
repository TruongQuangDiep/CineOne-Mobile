package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.review.CreateReviewRequest
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.review.ReviewApi
import com.example.cineone.domain.model.MovieReviewsData
import com.example.cineone.domain.model.Review
import com.example.cineone.domain.model.ReviewEligibility
import com.example.cineone.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReviewRepositoryImpl(private val reviewApi: ReviewApi) : ReviewRepository {

    override fun getMovieReviews(movieId: Long): Flow<Resource<MovieReviewsData>> = flow {
        emit(Resource.Loading)
        try {
            val response = reviewApi.getMovieReviews(movieId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải danh sách đánh giá."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải đánh giá."))
        }
    }

    override fun getMyReviews(): Flow<Resource<List<Review>>> = flow {
        emit(Resource.Loading)
        try {
            val response = reviewApi.getMyReviews()
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải danh sách đánh giá của bạn."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải đánh giá của bạn."))
        }
    }

    override fun checkEligibility(movieId: Long): Flow<Resource<ReviewEligibility>> = flow {
        emit(Resource.Loading)
        try {
            val response = reviewApi.checkEligibility(movieId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể kiểm tra điều kiện đánh giá."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi kiểm tra điều kiện đánh giá."))
        }
    }

    override fun createReview(movieId: Long, rating: Int, content: String): Flow<Resource<Review>> = flow {
        emit(Resource.Loading)
        try {
            val request = CreateReviewRequest(movieId = movieId, rating = rating, content = content)
            val response = reviewApi.createReview(request)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể gửi đánh giá phim."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi gửi đánh giá."))
        }
    }
}

