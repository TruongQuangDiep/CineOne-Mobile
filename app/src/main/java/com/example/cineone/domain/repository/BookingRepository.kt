package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Booking
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun createBooking(showtimeId: Long, seatIds: List<Long>, voucherCode: String?): Flow<Resource<Booking>>
}