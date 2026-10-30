package com.example.cineone.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoucherDto(
    val voucherId: Long = 0L,
    val code: String = "",
    @SerialName("description")
    val title: String = "",
    @SerialName("discountValue")
    val discountAmount: Double = 0.0,
    @SerialName("minOrder")
    val minOrderAmount: Double = 0.0,
    @SerialName("endAt")
    val expiresAt: String? = null,
    val status: String = "AVAILABLE",
)

@Serializable
data class ApplyVoucherRequestDto(
    val code: String = "",
    val subtotal: Long = 0L,
)

@Serializable
data class ApplyVoucherResponseDto(
    val code: String = "",
    val discountAmount: Long = 0L,
    val finalAmount: Long = 0L,
    val message: String? = null,
)
