package com.example.cineone.data.mapper

import com.example.cineone.data.dto.booking.BookingDto
import com.example.cineone.domain.model.Booking

fun BookingDto.toDomain(): Booking = Booking(
    bookingId = bookingId,
    bookingCode = bookingCode,
    totalAmount = totalAmount.toLong(),
    discountAmount = discountAmount.toLong(),
    finalAmount = (if (finalAmount > 0) finalAmount else totalAmount).toLong(),
    status = status,
    seatCodes = seatCodes
)