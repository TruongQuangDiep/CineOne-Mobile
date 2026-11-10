package com.example.cineone.domain.model

data class SupportRequest(
    val requestId: Long = 0L,
    val type: String = "HỖ TRỢ",
    val category: String = "KHÁC",
    val content: String = "",
    val status: String = "PENDING",
    val response: String? = null,
    val bookingId: Long? = null,
    val bookingCode: String? = null,
    val respondedAt: String? = null,
    val createdAt: String = ""
)
