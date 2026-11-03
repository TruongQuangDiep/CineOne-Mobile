package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Fnb
import com.example.cineone.domain.repository.CinemaRepository
import kotlinx.coroutines.flow.Flow

class GetCinemaFnbsUseCase(
    private val cinemaRepository: CinemaRepository
) {
    operator fun invoke(cinemaId: Long): Flow<Resource<List<Fnb>>> {
        return cinemaRepository.getCinemaFnbs(cinemaId)
    }
}

