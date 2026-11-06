package com.example.cineone.data.remote.payment

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.payment.PaymentLinkResponseDto
import com.example.cineone.data.dto.payment.PaymentStatusDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class PaymentApi(private val client: HttpClient) {

    suspend fun createPaymentLink(bookingId: Long): ApiResponseDto<PaymentLinkResponseDto> {
        return client.post("${ApiConfig.Payment.CREATE_LINK}/$bookingId").body()
    }

    suspend fun getPaymentStatus(bookingId: Long): ApiResponseDto<PaymentStatusDto> {
        return client.get("${ApiConfig.Payment.STATUS}/$bookingId").body()
    }
}