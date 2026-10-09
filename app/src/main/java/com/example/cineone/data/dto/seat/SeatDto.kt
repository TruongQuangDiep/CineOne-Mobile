package com.example.cineone.data.dto.seat

import kotlinx.serialization.Serializable

@Serializable
data class SeatDto(
    val id: String? = null,
    val rowNumber: String? = null,
    val seatNumber: Int? = null,
    val type: String? = null,
    val isAvailable: Boolean? = null
)
