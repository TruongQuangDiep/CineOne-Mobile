package com.example.cineone.domain.model

data class Showtime(
    val showtimeId: Long,
    val movieId: Long,
    val cinemaId: Long,
    val roomId: Long,
    val roomName: String,
    val startTime: String,
    val endTime: String,
    val price: Double,
    val movieTitle: String = "",
    val moviePosterUrl: String = "",
    val cinemaName: String = "",
    val status: String = "OPEN",
)

