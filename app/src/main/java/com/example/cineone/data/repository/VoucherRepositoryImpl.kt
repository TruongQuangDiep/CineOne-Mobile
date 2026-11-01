package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.voucher.ApplyVoucherRequestDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.voucher.VoucherApi
import com.example.cineone.domain.model.Voucher
import com.example.cineone.domain.repository.VoucherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VoucherRepositoryImpl(private val voucherApi: VoucherApi) : VoucherRepository {

    override fun getMyVouchers(tab: String): Flow<Resource<List<Voucher>>> = flow {
        emit(Resource.Loading)
        try {
            val response = voucherApi.getMyVouchers(tab)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không tải được danh sách voucher"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun applyVoucher(code: String, subtotal: Long): Flow<Resource<Long>> = flow {
        emit(Resource.Loading)
        try {
            val response = voucherApi.applyVoucher(ApplyVoucherRequestDto(code, subtotal))
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.discountAmount))
            } else {
                emit(Resource.Error(response.message ?: "Voucher không hợp lệ"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kiểm tra voucher"))
        }
    }
}