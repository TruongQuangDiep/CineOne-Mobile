package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.seat.HeldSeatDto
import com.example.cineone.data.dto.seat.SeatHoldRequestDto
import com.example.cineone.data.dto.seat.SeatHoldResponseDto
import com.example.cineone.data.remote.seat.SeatApi
import com.example.cineone.domain.repository.SeatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.domain.model.Seat

class SeatRepositoryImpl(private val seatApi: SeatApi) : SeatRepository {

    override fun holdSeats(showtimeId: Long, seatIds: List<Long>): Flow<Resource<SeatHoldResponseDto>> = flow {
        emit(Resource.Loading)
        try {
            val response = seatApi.holdSeats(SeatHoldRequestDto(showtimeId, seatIds))
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "Không thể giữ ghế"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun releaseSeats(showtimeId: Long, seatIds: List<Long>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = seatApi.releaseSeats(showtimeId, seatIds)
            if (response.isSuccess) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Không thể nhả ghế"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getHeldSeats(showtimeId: Long): Flow<Resource<List<HeldSeatDto>>> = flow {
        emit(Resource.Loading)
        try {
            val response = seatApi.getHeldSeats(showtimeId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách ghế"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getShowtimeSeats(showtimeId: Long): Flow<Resource<List<Seat>>> = flow {
        emit(Resource.Loading)
        try {
            val response = seatApi.getShowtimeSeats(showtimeId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải sơ đồ ghế"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}