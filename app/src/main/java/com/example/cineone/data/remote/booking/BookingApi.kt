package com.example.cineone.data.remote.booking

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.booking.BookingDto
import com.example.cineone.data.dto.booking.CreateBookingRequestDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class BookingApi(private val client: HttpClient) {

    suspend fun createBooking(request: CreateBookingRequestDto): ApiResponseDto<BookingDto> {
        return client.post(ApiConfig.Booking.CREATE) {
            setBody(request)
        }.body()
    }
}