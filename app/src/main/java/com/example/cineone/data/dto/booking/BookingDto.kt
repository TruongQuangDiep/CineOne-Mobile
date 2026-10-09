package com.example.cineone.data.dto.booking

import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    val id: String? = null,
    val userId: String? = null,
    val showtimeId: String? = null,
    val totalPrice: Double? = null,
    val status: String? = null,
    val createdAt: String? = null
)
