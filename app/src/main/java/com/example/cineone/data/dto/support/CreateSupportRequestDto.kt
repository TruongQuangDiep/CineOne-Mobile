package com.example.cineone.data.dto.support

import kotlinx.serialization.Serializable

@Serializable
data class CreateSupportRequestDto(
    val type: String,
    val category: String,
    val content: String,
    val bookingId: Long? = null,
    val bookingCode: String? = null
)

