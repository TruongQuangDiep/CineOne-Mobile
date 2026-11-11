package com.example.cineone.data.remote.notification

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.notification.NotificationDto
import com.example.cineone.data.dto.notification.UnreadCountDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class NotificationApi(private val client: HttpClient) {

    suspend fun getMyNotifications(filter: String? = null): ApiResponseDto<List<NotificationDto>> {
        return client.get(ApiConfig.Notification.LIST) {
            if (!filter.isNullOrBlank()) {
                parameter("filter", filter)
            }
        }.body()
    }

    suspend fun getUnreadCount(): ApiResponseDto<UnreadCountDto> {
        return client.get(ApiConfig.Notification.UNREAD_COUNT).body()
    }

    suspend fun markAsRead(id: Long): ApiResponseDto<NotificationDto> {
        return client.put(ApiConfig.Notification.markRead(id)).body()
    }

    suspend fun markAllAsRead(): ApiResponseDto<Map<String, Int>> {
        return client.put(ApiConfig.Notification.MARK_ALL_READ).body()
    }

    suspend fun getNotificationDetail(id: Long): ApiResponseDto<NotificationDto> {
        return client.get(ApiConfig.Notification.detail(id)).body()
    }
}