package com.example.cineone.data.remote.cinema

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.cinema.CinemaDto
import com.example.cineone.data.dto.food.FnbDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class CinemaApi(private val client: HttpClient) {

    suspend fun getCinemas(
        q: String? = null,
        city: String? = null,
        status: String? = null,
        page: Int = 0
    ): ApiResponseDto<List<CinemaDto>> {
        return client.get(ApiConfig.Cinema.LIST) {
            if (!q.isNullOrBlank()) parameter("q", q)
            if (!city.isNullOrBlank()) parameter("city", city)
            if (!status.isNullOrBlank()) parameter("status", status)
            parameter("page", page)
        }.body()
    }

    suspend fun getCinemaDetail(cinemaId: Long): ApiResponseDto<CinemaDto> {
        return client.get("${ApiConfig.Cinema.DETAIL}/$cinemaId").body()
    }

    suspend fun getCinemaFnbs(cinemaId: Long): ApiResponseDto<List<FnbDto>> {
        return client.get(ApiConfig.Fnb.byCinema(cinemaId)).body()
    }
}

