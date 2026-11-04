package com.example.cineone.data.mapper

import com.example.cineone.data.dto.seat.SeatDto
import com.example.cineone.domain.model.Seat

fun SeatDto.toDomain(): Seat {
    return Seat(
        seatId = seatId,
        roomId = roomId,
        seatRow = seatRow,
        seatNumber = seatNumber,
        seatType = seatType,
        status = status
    )
}