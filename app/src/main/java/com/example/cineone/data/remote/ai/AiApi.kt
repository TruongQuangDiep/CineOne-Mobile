package com.example.cineone.data.remote.ai

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.ai.AiSearchCriteriaRequestDto
import com.example.cineone.data.dto.ai.AiSearchCriteriaResponseDto
import com.example.cineone.data.dto.ai.FaqAnswerResponseDto
import com.example.cineone.data.dto.ai.FaqQuestionRequestDto
import com.example.cineone.data.dto.ai.ReviewSummaryResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AiApi(private val httpClient: HttpClient) {

    suspend fun askFaq(question: String): ApiResponseDto<FaqAnswerResponseDto> {
        return httpClient.post(ApiConfig.Ai.FAQ) {
            contentType(ContentType.Application.Json)
            setBody(FaqQuestionRequestDto(question = question))
        }.body()
    }

    suspend fun getMovieReviewSummary(movieId: Long): ApiResponseDto<ReviewSummaryResponseDto> {
        return try {
            httpClient.get(ApiConfig.Ai.movieReviewSummary(movieId)).body()
        } catch (_: Exception) {
            httpClient.get(ApiConfig.Ai.aiMovieReviewSummary(movieId)).body()
        }
    }

    suspend fun extractSearchCriteria(query: String): ApiResponseDto<AiSearchCriteriaResponseDto> {
        return httpClient.post(ApiConfig.Ai.SEARCH_CRITERIA) {
            contentType(ContentType.Application.Json)
            setBody(AiSearchCriteriaRequestDto(query = query))
        }.body()
    }
}
