package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getMovies(
        q: String? = null,
        status: String? = null,
        genreId: Long? = null,
        page: Int = 0,
        size: Int = 10
    ): Flow<Resource<List<Movie>>>

    fun getMovieDetail(movieId: Long): Flow<Resource<Movie>>
}

