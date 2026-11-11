package com.example.cineone.data.mapper

import com.example.cineone.data.dto.support.FaqDto
import com.example.cineone.data.dto.support.SupportResponseDto
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.model.SupportRequest

fun SupportResponseDto.toDomain(): SupportRequest = SupportRequest(
    requestId = requestId,
    type = type?.ifBlank { "HỖ TRỢ" } ?: "HỖ TRỢ",
    category = category?.ifBlank { "KHÁC" } ?: "KHÁC",
    content = content ?: "",
    status = status?.ifBlank { "PENDING" } ?: "PENDING",
    response = response?.takeIf { it.isNotBlank() },
    bookingId = bookingId,
    bookingCode = bookingCode?.takeIf { it.isNotBlank() },
    respondedAt = respondedAt?.takeIf { it.isNotBlank() },
    createdAt = createdAt ?: ""
)

fun FaqDto.toDomain(): FaqItem = FaqItem(
    id = id,
    title = title ?: "",
    content = content ?: "",
    category = category?.ifBlank { "FAQ" } ?: "FAQ"
)

