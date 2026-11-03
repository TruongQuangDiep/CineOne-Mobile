package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.ShowtimeRepository
import kotlinx.coroutines.flow.Flow

class GetShowtimesUseCase(
    private val showtimeRepository: ShowtimeRepository
) {
    operator fun invoke(
        movieId: Long? = null,
        cinemaId: Long? = null,
        date: String? = null,
        page: Int = 0
    ): Flow<Resource<List<Showtime>>> {
        return showtimeRepository.getShowtimes(
            movieId = movieId,
            cinemaId = cinemaId,
            date = date,
            page = page
        )
    }
}

