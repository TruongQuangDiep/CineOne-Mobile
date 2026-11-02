package com.example.cineone.data.dto.showtime

import kotlinx.serialization.Serializable

@Serializable
data class ShowtimeDto(
    val showtimeId: Long = 0L,
    val movieId: Long = 0L,
    val movieTitle: String? = null,
    val moviePosterUrl: String? = null,
    val cinemaId: Long = 0L,
    val cinemaName: String? = null,
    val roomId: Long = 0L,
    val roomName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val price: Double = 0.0,
    val basePrice: Double = 0.0,
    val status: String = "OPEN",
)
