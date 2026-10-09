package com.example.cineone.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String? = null,
    val title: String? = null,
    val message: String? = null,
    val isRead: Boolean? = null,
    val createdAt: String? = null
)
