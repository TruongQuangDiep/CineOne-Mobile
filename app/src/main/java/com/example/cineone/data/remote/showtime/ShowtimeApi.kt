package com.example.cineone.data.remote.showtime

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.showtime.ShowtimeDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ShowtimeApi(private val client: HttpClient) {

    suspend fun getShowtimes(
        movieId: Long? = null,
        cinemaId: Long? = null,
        date: String? = null,
        page: Int = 0
    ): ApiResponseDto<List<ShowtimeDto>> {
        return client.get(ApiConfig.Showtime.LIST) {
            if (movieId != null) parameter("movieId", movieId)
            if (cinemaId != null) parameter("cinemaId", cinemaId)
            if (!date.isNullOrBlank()) parameter("date", date)
            parameter("page", page)
        }.body()
    }
}

