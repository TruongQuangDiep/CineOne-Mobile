package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.cinema.CinemaApi
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Fnb
import com.example.cineone.domain.repository.CinemaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CinemaRepositoryImpl(private val cinemaApi: CinemaApi) : CinemaRepository {

    override fun getCinemas(
        q: String?,
        city: String?,
        status: String?,
        page: Int
    ): Flow<Resource<List<Cinema>>> = flow {
        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemas(q = q, city = city, status = status, page = page)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách rạp"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getCinemaDetail(cinemaId: Long): Flow<Resource<Cinema>> = flow {
        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemaDetail(cinemaId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không tìm thấy chi tiết rạp"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getCinemaFnbs(cinemaId: Long): Flow<Resource<List<Fnb>>> = flow {
        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemaFnbs(cinemaId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách bắp nước"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}

