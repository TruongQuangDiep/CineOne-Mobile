package com.example.cineone.data.dto.support

import kotlinx.serialization.Serializable

@Serializable
data class SupportResponseDto(
    val requestId: Long = 0L,
    val type: String? = null,
    val category: String? = null,
    val content: String? = null,
    val status: String? = null,
    val response: String? = null,
    val bookingId: Long? = null,
    val bookingCode: String? = null,
    val respondedAt: String? = null,
    val createdAt: String? = null
)

