package com.example.cineone.data.mapper

import com.example.cineone.data.dto.booking.BookingDto
import com.example.cineone.domain.model.Booking

fun BookingDto.toDomain(): Booking = Booking(
    bookingId = bookingId,
    bookingCode = bookingCode,
    totalAmount = totalAmount,
    discountAmount = discountAmount,
    finalAmount = finalAmount,
    status = status,
    seatCodes = seatCodes
)