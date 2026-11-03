package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Seat
import com.example.cineone.domain.repository.SeatRepository
import kotlinx.coroutines.flow.Flow

class GetSeatsUseCase(private val repository: SeatRepository) {
    operator fun invoke(showtimeId: Long): Flow<Resource<List<Seat>>> {
        return repository.getShowtimeSeats(showtimeId)
    }
}