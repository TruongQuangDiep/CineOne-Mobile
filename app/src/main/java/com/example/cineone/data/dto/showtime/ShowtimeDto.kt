package com.example.cineone.data.dto.showtime

import kotlinx.serialization.Serializable

@Serializable
data class ShowtimeDto(
    val id: String? = null,
    val movieId: String? = null,
    val cinemaId: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val price: Double? = null
)
