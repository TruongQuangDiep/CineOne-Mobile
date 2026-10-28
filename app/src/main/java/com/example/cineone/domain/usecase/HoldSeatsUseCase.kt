package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.seat.SeatHoldResponseDto
import com.example.cineone.domain.repository.SeatRepository
import kotlinx.coroutines.flow.Flow

class HoldSeatsUseCase(private val repository: SeatRepository) {
    operator fun invoke(showtimeId: Long, seatIds: List<Long>): Flow<Resource<SeatHoldResponseDto>> {
        return repository.holdSeats(showtimeId, seatIds)
    }
}