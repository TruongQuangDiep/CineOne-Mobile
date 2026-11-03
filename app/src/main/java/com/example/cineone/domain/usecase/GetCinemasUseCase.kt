package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.repository.CinemaRepository
import kotlinx.coroutines.flow.Flow

class GetCinemasUseCase(
    private val cinemaRepository: CinemaRepository
) {
    operator fun invoke(
        q: String? = null,
        city: String? = null,
        status: String? = null,
        page: Int = 0
    ): Flow<Resource<List<Cinema>>> {
        return cinemaRepository.getCinemas(
            q = q,
            city = city,
            status = status,
            page = page
        )
    }
}

