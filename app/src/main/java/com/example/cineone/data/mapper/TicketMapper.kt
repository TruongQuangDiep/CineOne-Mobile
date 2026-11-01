package com.example.cineone.data.mapper

import com.example.cineone.data.dto.ticket.TicketDetailDto
import com.example.cineone.data.dto.ticket.TicketDto
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.model.TicketDetail

fun TicketDto.toDomain(): Ticket = Ticket(
    ticketId = ticketId,
    ticketCode = ticketCode,
    movieTitle = movieTitle,
    cinemaName = cinemaName,
    roomName = roomName,
    showDate = showDate,
    showTime = showTime,
    seatCodes = seatCodes
)

fun TicketDetailDto.toDomain(): TicketDetail = TicketDetail(
    ticketId = ticketId,
    ticketCode = ticketCode,
    movieTitle = movieTitle,
    cinemaName = cinemaName,
    roomName = roomName,
    showDateTime = showDateTime,
    seatCodes = seatCodes,
    totalAmount = totalAmount,
    qrCheckInCode = qrCheckInCode
)