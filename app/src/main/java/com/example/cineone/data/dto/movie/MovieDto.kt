package com.example.cineone.data.dto.movie

import kotlinx.serialization.Serializable

@Serializable
data class MovieDto(
    val movieId: Long = 0L,
    val title: String = "",
    val poster: String? = null,
    val posterUrl: String? = null,
    val trailer: String? = null,
    val trailerUrl: String? = null,
    val duration: Int = 0,
    val releaseDate: String? = null,
    val genres: String = "",
    val rating: Double = 0.0,
    val ageRating: String? = null,
    val country: String? = null,
    val director: String? = null,
    val castInfo: String? = null,
    val description: String? = null,
    val status: String = "COMING_SOON",
)

@Serializable
data class MovieDetailDto(
    val movieId: Long = 0L,
    val title: String = "",
    val poster: String? = null,
    val posterUrl: String? = null,
    val trailer: String? = null,
    val trailerUrl: String? = null,
    val duration: Int = 0,
    val releaseDate: String? = null,
    val genres: String = "",
    val rating: Double = 0.0,
    val ageRating: String? = null,
    val country: String? = null,
    val director: String? = null,
    val castInfo: String? = null,
    val description: String? = null,
    val status: String = "COMING_SOON",
)
