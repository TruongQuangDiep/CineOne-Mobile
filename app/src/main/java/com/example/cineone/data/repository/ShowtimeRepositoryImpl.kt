package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.showtime.ShowtimeApi
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.ShowtimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

class ShowtimeRepositoryImpl(private val showtimeApi: ShowtimeApi) : ShowtimeRepository {

    override fun getShowtimes(
        movieId: Long?,
        cinemaId: Long?,
        date: String?,
        page: Int
    ): Flow<Resource<List<Showtime>>> = flow {
        val cacheKey = "${movieId ?: -1L}|${cinemaId ?: -1L}|${date.orEmpty().trim()}|$page"
        val now = System.currentTimeMillis()
        val cached = showtimeMemoryCache[cacheKey]
        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {
            emit(Resource.Success(cached.second))
            return@flow
        }

        emit(Resource.Loading)
        try {
            val response = showtimeApi.getShowtimes(
                movieId = movieId,
                cinemaId = cinemaId,
                date = date,
                page = page
            )
            if (response.success && response.data != null) {
                val mapped = response.data.map { it.toDomain() }
                showtimeMemoryCache[cacheKey] = System.currentTimeMillis() to mapped
                emit(Resource.Success(mapped))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách suất chiếu"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 3 * 60 * 1000L // 3 phút
        private val showtimeMemoryCache = ConcurrentHashMap<String, Pair<Long, List<Showtime>>>()
    }
}
