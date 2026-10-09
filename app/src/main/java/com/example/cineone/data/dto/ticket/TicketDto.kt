package com.example.cineone.data.dto.ticket

import kotlinx.serialization.Serializable

@Serializable
data class TicketDto(
    val id: String? = null,
    val bookingId: String? = null,
    val qrCode: String? = null,
    val seatName: String? = null,
    val movieTitle: String? = null
)
