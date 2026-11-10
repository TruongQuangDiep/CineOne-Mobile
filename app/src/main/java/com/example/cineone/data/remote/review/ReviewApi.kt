package com.example.cineone.data.remote.review

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.review.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ReviewApi(private val client: HttpClient) {

    suspend fun getMovieReviews(movieId: Long): ApiResponseDto<MovieReviewsResponseDto> {
        return client.get(ApiConfig.Review.movieReviews(movieId)).body()
    }

    suspend fun getMyReviews(): ApiResponseDto<List<MyReviewItemDto>> {
        return client.get(ApiConfig.Review.MY_REVIEWS).body()
    }

    suspend fun checkEligibility(movieId: Long): ApiResponseDto<ReviewEligibilityDto> {
        return client.get(ApiConfig.Review.eligibility(movieId)).body()
    }

    suspend fun createReview(request: CreateReviewRequest): ApiResponseDto<MovieReviewItemDto> {
        return client.post(ApiConfig.Review.CREATE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

