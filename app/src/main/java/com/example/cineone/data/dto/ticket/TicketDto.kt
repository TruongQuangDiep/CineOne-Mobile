package com.example.cineone.data.dto.ticket

import kotlinx.serialization.Serializable

@Serializable
data class TicketDto(
    val ticketId: Long = 0L,
    val ticketCode: String = "",
    val movieTitle: String = "",
    val cinemaName: String = "",
    val roomName: String = "",
    val showDate: String = "",
    val showTime: String = "",
    val seatCodes: String = "",
    val totalAmount: Double = 0.0,
)

@Serializable
data class TicketDetailDto(
    val ticketId: Long = 0L,
    val ticketCode: String = "",
    val movieTitle: String = "",
    val cinemaName: String = "",
    val roomName: String = "",
    val showDateTime: String = "",
    val seatCodes: String = "",
    val totalAmount: Double = 0.0,
    val qrCheckInCode: String = "",
)
