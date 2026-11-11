package com.example.cineone.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val notificationId: Long = 0L,
    val campaignId: Long? = null,
    val title: String = "",
    val message: String = "",
    val type: String = "SYSTEM",
    val isRead: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class UnreadCountDto(
    val unreadCount: Long = 0L
)