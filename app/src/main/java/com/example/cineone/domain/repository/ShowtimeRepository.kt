package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Showtime
import kotlinx.coroutines.flow.Flow

interface ShowtimeRepository {
    fun getShowtimes(
        movieId: Long? = null,
        cinemaId: Long? = null,
        date: String? = null,
        page: Int = 0
    ): Flow<Resource<List<Showtime>>>
}

