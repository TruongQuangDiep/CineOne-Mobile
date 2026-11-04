package com.example.cineone.domain.model

data class Voucher(
    val voucherId: Long,
    val code: String,
    val title: String,
    val imageUrl: String? = null,
    val discountType: String = "FIXED",
    val discountAmount: Long,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double? = null,
    val startAt: String? = null,
    val expiresAt: String? = null,
    val status: String = "ACTIVE",
)
