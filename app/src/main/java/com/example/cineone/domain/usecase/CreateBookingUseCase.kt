package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Booking
import com.example.cineone.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow

class CreateBookingUseCase(private val repository: BookingRepository) {
    operator fun invoke(
        showtimeId: Long,
        seatIds: List<Long>,
        subtotal: Long = 0L,
        discountAmount: Long = 0L,
        totalAmount: Long = 0L,
        voucherCode: String? = null
    ): Flow<Resource<Booking>> {
        return repository.createBooking(
            showtimeId = showtimeId,
            seatIds = seatIds,
            subtotal = subtotal,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            voucherCode = voucherCode
        )
    }
}
