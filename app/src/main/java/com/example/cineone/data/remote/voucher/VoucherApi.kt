package com.example.cineone.data.remote.voucher

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.voucher.ApplyVoucherRequestDto
import com.example.cineone.data.dto.voucher.ApplyVoucherResponseDto
import com.example.cineone.data.dto.voucher.VoucherDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class VoucherApi(private val client: HttpClient) {

    suspend fun getMyVouchers(tab: String = "AVAILABLE"): ApiResponseDto<List<VoucherDto>> {
        return client.get(ApiConfig.Voucher.MY_VOUCHERS) {
            parameter("tab", tab)
        }.body()
    }

    suspend fun applyVoucher(request: ApplyVoucherRequestDto): ApiResponseDto<ApplyVoucherResponseDto> {
        return client.post(ApiConfig.Voucher.APPLY) {
            setBody(request)
        }.body()
    }
}