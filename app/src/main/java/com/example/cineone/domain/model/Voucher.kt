package com.example.cineone.domain.model

data class Voucher(
    val voucherId: Long,
    val code: String,
    val title: String,
    val discountAmount: Long,
    val minOrderAmount: Double = 0.0,
    val expiresAt: String? = null,
    val status: String = "AVAILABLE",
)
