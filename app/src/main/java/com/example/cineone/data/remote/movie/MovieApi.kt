package com.example.cineone.data.remote.movie

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.movie.MovieDetailDto
import com.example.cineone.data.dto.movie.MovieDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class MovieApi(private val client: HttpClient) {

    suspend fun getMovies(
        q: String? = null,
        status: String? = null,
        genreId: Long? = null,
        page: Int = 0,
        size: Int = 10
    ): ApiResponseDto<List<MovieDto>> {
        return client.get(ApiConfig.Movie.LIST) {
            if (!q.isNullOrBlank()) parameter("q", q)
            if (!status.isNullOrBlank()) parameter("status", status)
            if (genreId != null) parameter("genreId", genreId)
            parameter("page", page)
            parameter("size", size)
        }.body()
    }

    suspend fun getMovieDetail(movieId: Long): ApiResponseDto<MovieDetailDto> {
        return client.get("${ApiConfig.Movie.DETAIL}/$movieId").body()
    }
}

