package com.example.cineone.data.dto.voucher

import kotlinx.serialization.Serializable

@Serializable
data class VoucherDto(
    val id: String? = null,
    val code: String? = null,
    val discountPercent: Double? = null,
    val maxDiscount: Double? = null,
    val expiryDate: String? = null
)
