package com.example.cineone.domain.model

data class ShowtimeSeatState(
    val showtimeId: String,
    val seatId: String,
    val isBooked: Boolean,
    val isHeld: Boolean
)
