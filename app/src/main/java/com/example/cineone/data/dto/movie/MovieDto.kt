package com.example.cineone.data.dto.movie

import kotlinx.serialization.Serializable

@Serializable
data class MovieDto(
    val id: String? = null,
    val title: String? = null,
    val posterUrl: String? = null,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val rating: Double? = null,
    val releaseDate: String? = null
)
