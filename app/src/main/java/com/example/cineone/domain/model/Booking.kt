package com.example.cineone.domain.model

data class Booking(
    val bookingId: Long,
    val bookingCode: String,
    val totalAmount: Long,
    val discountAmount: Long = 0L,
    val finalAmount: Long,
    val status: String = "CONFIRMED",
    val seatCodes: List<String> = emptyList()
)