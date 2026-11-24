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
            if (response.isSuccess && response.data != null) {
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
            val discount = response.data?.discountAmount ?: 0L
            val isSuccess = response.isSuccess && discount > 0L

            if (isSuccess) {
                emit(Resource.Success(discount))
            } else {
                // Lấy câu thông báo lỗi chi tiết từ Server (Ví dụ: "Đơn hàng tối thiểu phải từ 100,000 đ")
                val errorMsg = response.message
                    ?: response.data?.message
                    ?: "Đơn hàng chưa đủ điều kiện áp dụng mã này"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kiểm tra voucher"))
        }
    }
}