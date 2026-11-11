package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.support.CreateSupportRequestDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.support.SupportApi
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.model.SupportRequest
import com.example.cineone.domain.repository.SupportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupportRepositoryImpl(private val supportApi: SupportApi) : SupportRepository {

    override fun createSupportRequest(
        type: String,
        category: String,
        content: String,
        bookingId: Long?,
        bookingCode: String?
    ): Flow<Resource<SupportRequest>> = flow {
        emit(Resource.Loading)
        try {
            val request = CreateSupportRequestDto(
                type = type,
                category = category,
                content = content,
                bookingId = bookingId,
                bookingCode = bookingCode?.takeIf { it.isNotBlank() }
            )
            val response = supportApi.createSupportRequest(request)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể gửi yêu cầu hỗ trợ / khiếu nại."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi gửi yêu cầu hỗ trợ."))
        }
    }

    override fun getMySupportRequests(): Flow<Resource<List<SupportRequest>>> = flow {
        emit(Resource.Loading)
        try {
            val response = supportApi.getMySupportRequests()
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải danh sách yêu cầu hỗ trợ."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải danh sách yêu cầu hỗ trợ."))
        }
    }

    override fun getSupportDetail(id: Long): Flow<Resource<SupportRequest>> = flow {
        emit(Resource.Loading)
        try {
            val response = supportApi.getSupportDetail(id)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải chi tiết yêu cầu hỗ trợ."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải chi tiết yêu cầu."))
        }
    }

    override fun updateSupportRequest(
        id: Long,
        type: String,
        category: String,
        content: String,
        bookingId: Long?,
        bookingCode: String?
    ): Flow<Resource<SupportRequest>> = flow {
        emit(Resource.Loading)
        try {
            val request = CreateSupportRequestDto(
                type = type,
                category = category,
                content = content,
                bookingId = bookingId,
                bookingCode = bookingCode?.takeIf { it.isNotBlank() }
            )
            val response = supportApi.updateSupportRequest(id, request)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể cập nhật yêu cầu hỗ trợ / khiếu nại."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi cập nhật yêu cầu hỗ trợ."))
        }
    }

    override fun deleteSupportRequest(id: Long): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val response = supportApi.deleteSupportRequest(id)
            if (response.isSuccess) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(response.message ?: "Không thể xóa yêu cầu hỗ trợ / khiếu nại."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi xóa yêu cầu hỗ trợ."))
        }
    }

    override fun getFaqList(category: String?, q: String?): Flow<Resource<List<FaqItem>>> = flow {
        emit(Resource.Loading)
        try {
            val response = supportApi.getFaqList(category = category, q = q)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải danh sách câu hỏi thường gặp."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải câu hỏi thường gặp."))
        }
    }
}

