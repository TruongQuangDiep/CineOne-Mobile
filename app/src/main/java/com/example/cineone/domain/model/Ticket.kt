package com.example.cineone.domain.model

data class Ticket(
    val ticketId: Long,
    val ticketCode: String,
    val movieTitle: String,
    val cinemaName: String,
    val roomName: String,
    val showDate: String,
    val showTime: String,
    val seatCodes: String,
)

data class TicketDetail(
    val ticketId: Long,
    val ticketCode: String,
    val movieTitle: String,
    val cinemaName: String,
    val roomName: String,
    val showDateTime: String,
    val seatCodes: String,
    val totalAmount: Double = 0.0,
    val qrCheckInCode: String,
)
