package com.example.cineone.domain.model

data class Seat(
    val seatId: Long,
    val roomId: Long,
    val seatRow: String,
    val seatNumber: Int,
    val seatType: String = "NORMAL", // "NORMAL", "VIP", "SWEETBOX"
    val status: String = "ACTIVE"
)