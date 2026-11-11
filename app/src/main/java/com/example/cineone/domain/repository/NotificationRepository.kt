package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getMyNotifications(filter: String? = null): Flow<Resource<List<Notification>>>
    fun markAsRead(notificationId: Long): Flow<Resource<Notification>>
    fun markAllAsRead(): Flow<Resource<Boolean>>

    fun getNotificationDetail(notificationId: Long): Flow<Resource<Notification>>
}