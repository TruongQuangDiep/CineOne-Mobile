package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Booking
import com.example.cineone.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow

class CreateBookingUseCase(private val repository: BookingRepository) {
    operator fun invoke(showtimeId: Long, seatIds: List<Long>, voucherCode: String? = null): Flow<Resource<Booking>> {
        return repository.createBooking(showtimeId, seatIds, voucherCode)
    }
}