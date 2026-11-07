package com.example.cineone.data.dto.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentLinkResponseDto(
    val orderCode: Long = 0L,
    val checkoutUrl: String = "",
    val qrCode: String = "",
    val status: String = ""
)

@Serializable
data class PaymentStatusDto(
    val bookingId: Long = 0L,
    val bookingCode: String = "",
    val bookingStatus: String = "",
    val isPaid: Boolean = false,
    val status: String = ""
)