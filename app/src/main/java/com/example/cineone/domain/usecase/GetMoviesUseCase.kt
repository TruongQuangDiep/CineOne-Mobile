package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class GetMoviesUseCase(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(
        q: String? = null,
        status: String? = null,
        genreId: Long? = null,
        page: Int = 0,
        size: Int = 10
    ): Flow<Resource<List<Movie>>> {
        return movieRepository.getMovies(
            q = q,
            status = status,
            genreId = genreId,
            page = page,
            size = size
        )
    }
}

