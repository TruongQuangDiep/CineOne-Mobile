package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.seat.HeldSeatDto
import com.example.cineone.data.dto.seat.SeatHoldResponseDto
import kotlinx.coroutines.flow.Flow

interface SeatRepository {
    fun holdSeats(showtimeId: Long, seatIds: List<Long>): Flow<Resource<SeatHoldResponseDto>>
    fun releaseSeats(showtimeId: Long, seatIds: List<Long>): Flow<Resource<Unit>>
    fun getHeldSeats(showtimeId: Long): Flow<Resource<List<HeldSeatDto>>>
}