package com.example.cineone.data.remote.seat

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.seat.HeldSeatDto
import com.example.cineone.data.dto.seat.SeatDto
import com.example.cineone.data.dto.seat.SeatHoldRequestDto
import com.example.cineone.data.dto.seat.SeatHoldResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class SeatApi(private val client: HttpClient) {

    suspend fun holdSeats(request: SeatHoldRequestDto): ApiResponseDto<SeatHoldResponseDto> {
        return client.post(ApiConfig.Seat.HOLD) {
            setBody(request)
        }.body()
    }

    suspend fun releaseSeats(showtimeId: Long, seatIds: List<Long>): ApiResponseDto<Unit> {
        return client.post(ApiConfig.Seat.RELEASE) {
            parameter("showtimeId", showtimeId)
            parameter("seatIds", seatIds.joinToString(","))
        }.body()
    }

    suspend fun getHeldSeats(showtimeId: Long): ApiResponseDto<List<HeldSeatDto>> {
        return client.get(ApiConfig.Seat.HELD) {
            parameter("showtimeId", showtimeId)
        }.body()
    }

    suspend fun getShowtimeSeats(showtimeId: Long): ApiResponseDto<List<SeatDto>> {
        return client.get(ApiConfig.Showtime.seats(showtimeId)).body()
    }
}