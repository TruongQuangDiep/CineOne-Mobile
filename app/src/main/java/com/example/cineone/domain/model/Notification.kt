package com.example.cineone.domain.model

data class Notification(
    val notificationId: Long,
    val campaignId: Long?,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String
)