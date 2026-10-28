package com.example.cineone.data.dto.voucher

import kotlinx.serialization.Serializable

@Serializable
data class VoucherDto(
    val voucherId: Long,
    val code: String,
    val title: String,
    val discountAmount: Long,
    val minOrderAmount: Long = 0L,
    val expiresAt: String? = null,
    val status: String = "AVAILABLE"
)

@Serializable
data class ApplyVoucherRequestDto(
    val code: String,
    val subtotal: Long
)

@Serializable
data class ApplyVoucherResponseDto(
    val code: String,
    val discountAmount: Long,
    val finalAmount: Long,
    val message: String? = null
)