package com.example.cineone.data.dto.voucher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoucherDto(
    val voucherId: Long = 0L,
    val code: String = "",
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val discountType: String? = null,
    val discountValue: Double = 0.0,
    val minOrder: Double = 0.0,
    val maxDiscount: Double? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val usageLimit: Int? = null,
    val status: String = "ACTIVE",
    val discountDisplay: String? = null,
    val minOrderDisplay: String? = null,
    val maxDiscountDisplay: String? = null,
    val expiryDisplay: String? = null,
    val periodDisplay: String? = null,
    val remainingUsage: Int? = null,
    val tab: String? = null,
    val applicableTierNames: List<String> = emptyList()
)

@Serializable
data class ApplyVoucherRequestDto(
    val code: String = "",
    val subtotal: Long = 0L,
)

@Serializable
data class ApplyVoucherResponseDto(
    val success: Boolean = false,
    val code: String = "",
    val discountAmount: Long = 0L,
    val finalAmount: Long = 0L,
    val message: String? = null,
)