package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.movie.MovieApi
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MovieRepositoryImpl(private val movieApi: MovieApi) : MovieRepository {

    override fun getMovies(
        q: String?,
        status: String?,
        genreId: Long?,
        page: Int,
        size: Int
    ): Flow<Resource<List<Movie>>> = flow {
        emit(Resource.Loading)
        try {
            val response = movieApi.getMovies(q = q, status = status, genreId = genreId, page = page, size = size)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách phim"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getMovieDetail(movieId: Long): Flow<Resource<Movie>> = flow {
        emit(Resource.Loading)
        try {
            val response = movieApi.getMovieDetail(movieId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không tìm thấy chi tiết phim"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}

