package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class GetMovieDetailUseCase(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(movieId: Long): Flow<Resource<Movie>> {
        return movieRepository.getMovieDetail(movieId)
    }
}

