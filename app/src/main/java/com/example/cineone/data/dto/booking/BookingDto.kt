package com.example.cineone.data.dto.booking

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val bookingId: Long = 0L,
    val bookingCode: String = "",
    val totalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val status: String = "HELD",
    val seatCodes: List<String> = emptyList()
)

@Serializable
data class CreateBookingRequestDto(
    val showtimeId: Long,
    val seatIds: List<Long> = emptyList(),
    val subtotal: Long = 0L,
    val discountAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val voucherCode: String? = null
)