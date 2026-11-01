package com.example.cineone.data.dto.seat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeatDto(
    val seatId: Long = 0L,
    val roomId: Long = 0L,
    val seatRow: String = "",
    val seatNumber: Int = 0,
    val seatType: String = "NORMAL",
    val status: String = "ACTIVE"
)

@Serializable
data class SeatHoldRequestDto(
    val showtimeId: Long,
    val seatIds: List<Long>
)

@Serializable
data class SeatHoldResponseDto(
    val showtimeId: Long = 0L,
    val heldSeatIds: List<Long> = emptyList(),
    @SerialName("expiresAt")
    val expiresAt: String? = null
)

@Serializable
data class HeldSeatDto(
    val seatId: Long = 0L,
    val userId: Long? = null,
    val status: String? = "HOLDING",
    val holdExpiresAt: String? = null
)