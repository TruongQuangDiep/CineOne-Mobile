package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.showtime.ShowtimeApi
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.ShowtimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ShowtimeRepositoryImpl(private val showtimeApi: ShowtimeApi) : ShowtimeRepository {

    override fun getShowtimes(
        movieId: Long?,
        cinemaId: Long?,
        date: String?,
        page: Int
    ): Flow<Resource<List<Showtime>>> = flow {
        emit(Resource.Loading)
        try {
            val response = showtimeApi.getShowtimes(
                movieId = movieId,
                cinemaId = cinemaId,
                date = date,
                page = page
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách suất chiếu"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}

