package com.example.cineone.data.dto.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentDto(
    val id: String? = null,
    val bookingId: String? = null,
    val paymentMethod: String? = null,
    val amount: Double? = null,
    val status: String? = null
)
