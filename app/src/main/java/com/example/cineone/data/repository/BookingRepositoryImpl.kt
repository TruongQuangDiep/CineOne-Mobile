package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.booking.CreateBookingRequestDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.booking.BookingApi
import com.example.cineone.domain.model.Booking
import com.example.cineone.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookingRepositoryImpl(private val bookingApi: BookingApi) : BookingRepository {

    override fun createBooking(
        showtimeId: Long,
        seatIds: List<Long>,
        subtotal: Long,
        discountAmount: Long,
        totalAmount: Long,
        voucherCode: String?
    ): Flow<Resource<Booking>> = flow {
        emit(Resource.Loading)
        try {
            val request = CreateBookingRequestDto(
                showtimeId = showtimeId,
                seatIds = seatIds,
                subtotal = subtotal,
                discountAmount = discountAmount,
                totalAmount = totalAmount,
                voucherCode = voucherCode
            )
            val response = bookingApi.createBooking(request)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Đặt vé thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi hệ thống đặt vé"))
        }
    }
}