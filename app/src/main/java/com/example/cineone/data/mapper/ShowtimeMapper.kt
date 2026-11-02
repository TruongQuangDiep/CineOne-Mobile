package com.example.cineone.data.mapper

import com.example.cineone.data.dto.showtime.ShowtimeDto
import com.example.cineone.domain.model.Showtime

fun ShowtimeDto.toDomain(): Showtime = Showtime(
    showtimeId = showtimeId,
    movieId = movieId,
    cinemaId = cinemaId,
    roomId = roomId,
    roomName = roomName,
    startTime = startTime,
    endTime = endTime,
    price = if (price > 0.0) price else basePrice,
    movieTitle = movieTitle.orEmpty(),
    moviePosterUrl = moviePosterUrl.orEmpty(),
    cinemaName = cinemaName.orEmpty(),
    status = status,
)

