package com.example.cineone.data.remote.support

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.support.CreateSupportRequestDto
import com.example.cineone.data.dto.support.FaqDto
import com.example.cineone.data.dto.support.SupportResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class SupportApi(private val client: HttpClient) {

    suspend fun createSupportRequest(request: CreateSupportRequestDto): ApiResponseDto<SupportResponseDto> {
        return client.post(ApiConfig.Support.CREATE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getMySupportRequests(): ApiResponseDto<List<SupportResponseDto>> {
        return client.get(ApiConfig.Support.MY_REQUESTS).body()
    }

    suspend fun getSupportDetail(id: Long): ApiResponseDto<SupportResponseDto> {
        return client.get(ApiConfig.Support.detail(id)).body()
    }

    suspend fun getFaqList(category: String? = null, q: String? = null): ApiResponseDto<List<FaqDto>> {
        return client.get(ApiConfig.Faq.LIST) {
            if (!category.isNullOrBlank()) {
                parameter("category", category)
            }
            if (!q.isNullOrBlank()) {
                parameter("q", q)
            }
        }.body()
    }
}
