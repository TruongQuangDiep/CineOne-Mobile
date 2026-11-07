package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.payment.PaymentLinkResponseDto
import com.example.cineone.data.dto.payment.PaymentStatusDto
import com.example.cineone.data.remote.payment.PaymentApi
import com.example.cineone.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PaymentRepositoryImpl(private val paymentApi: PaymentApi) : PaymentRepository {

    override fun createPaymentLink(bookingId: Long): Flow<Resource<PaymentLinkResponseDto>> = flow {
        emit(Resource.Loading)
        try {
            val response = paymentApi.createPaymentLink(bookingId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tạo mã VietQR thanh toán"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối tạo mã VietQR"))
        }
    }

    override fun getPaymentStatus(bookingId: Long): Flow<Resource<PaymentStatusDto>> = flow {
        try {
            val response = paymentApi.getPaymentStatus(bookingId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được trạng thái thanh toán"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kiểm tra trạng thái thanh toán"))
        }
    }
}