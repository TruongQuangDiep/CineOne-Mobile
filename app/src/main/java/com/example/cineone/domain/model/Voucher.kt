package com.example.cineone.domain.model

data class Voucher(
    val voucherId: Long,
    val code: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val discountType: String = "FIXED",
    val discountAmount: Long = 0L,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double? = null,
    val startAt: String? = null,
    val expiresAt: String? = null,
    val status: String = "ACTIVE",
    val discountDisplay: String = "",
    val minOrderDisplay: String = "",
    val maxDiscountDisplay: String? = null,
    val expiryDisplay: String = "",
    val periodDisplay: String = "",
    val remainingUsage: Int = 999,
    val tab: String = "AVAILABLE"
)