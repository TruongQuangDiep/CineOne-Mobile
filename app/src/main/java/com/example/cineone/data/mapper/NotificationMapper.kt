package com.example.cineone.data.mapper

import com.example.cineone.data.dto.notification.NotificationDto
import com.example.cineone.domain.model.Notification

fun NotificationDto.toDomain(): Notification = Notification(
    notificationId = notificationId,
    campaignId = campaignId,
    title = title.ifBlank { "Thông báo hệ thống" },
    message = message,
    type = type.ifBlank { "SYSTEM" }.uppercase(),
    isRead = isRead,
    createdAt = createdAt ?: ""
)