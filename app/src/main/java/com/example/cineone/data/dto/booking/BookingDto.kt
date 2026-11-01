package com.example.cineone.data.dto.booking

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val bookingId: Long = 0L,
    val bookingCode: String = "",
    val totalAmount: Long = 0L,
    val discountAmount: Long = 0L,
    val finalAmount: Long = 0L,
    val status: String = "CONFIRMED",
    val seatCodes: List<String> = emptyList()
)

@Serializable
data class CreateBookingRequestDto(
    val showtimeId: Long,
    val seatIds: List<Long>,
    val voucherCode: String? = null
)