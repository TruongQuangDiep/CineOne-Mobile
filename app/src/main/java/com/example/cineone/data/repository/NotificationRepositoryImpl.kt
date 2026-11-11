package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.notification.NotificationApi
import com.example.cineone.domain.model.Notification
import com.example.cineone.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApi
) : NotificationRepository {

    override fun getMyNotifications(filter: String?): Flow<Resource<List<Notification>>> = flow {
        emit(Resource.Loading)
        try {
            val response = notificationApi.getMyNotifications(filter)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải danh sách thông báo."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải thông báo."))
        }
    }

    override fun markAsRead(notificationId: Long): Flow<Resource<Notification>> = flow {
        emit(Resource.Loading)
        try {
            val response = notificationApi.markAsRead(notificationId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể đánh dấu đã đọc."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi cập nhật thông báo."))
        }
    }

    override fun markAllAsRead(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val response = notificationApi.markAllAsRead()
            if (response.isSuccess) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(response.message ?: "Không thể đánh dấu tất cả đã đọc."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi cập nhật thông báo."))
        }
    }

    override fun getNotificationDetail(notificationId: Long): Flow<Resource<Notification>> = flow {
        emit(Resource.Loading)
        try {
            val response = notificationApi.getNotificationDetail(notificationId)
            if (response.isSuccess && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không thể tải chi tiết thông báo."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối khi tải chi tiết thông báo."))
        }
    }
}