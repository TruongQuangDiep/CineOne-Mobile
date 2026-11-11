package com.example.cineone.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.core.notification.NotificationEventBus
import com.example.cineone.domain.model.Notification
import com.example.cineone.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NotificationTab(val code: String, val label: String) {
    ALL("ALL", "Tất cả"),
    IMPORTANT("IMPORTANT", "Quan trọng"),
    READ("READ", "Đã xem")
}

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val selectedTab: NotificationTab = NotificationTab.ALL,
    val isMarkingAll: Boolean = false,
    val errorMessage: String? = null,
    val detailNotification: Notification? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }

    val filteredNotifications: List<Notification>
        get() = when (selectedTab) {
            NotificationTab.ALL -> notifications
            NotificationTab.IMPORTANT -> notifications.filter { it.type != "PROMOTION" }
            NotificationTab.READ -> notifications.filter { it.isRead }
        }
}

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        viewModelScope.launch {
            NotificationEventBus.refreshEvents.collect {
                loadNotifications()
            }
        }
    }

    fun loadNotifications() {
        val isLoggedIn = CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        if (!isLoggedIn) {
            NotificationEventBus.updateUnreadCount(0)
            _uiState.update {
                it.copy(isLoading = false, notifications = emptyList(), errorMessage = null)
            }
            return
        }

        viewModelScope.launch {
            notificationRepository.getMyNotifications().collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    is Resource.Success -> {
                        NotificationEventBus.updateUnreadCount(res.data.count { !it.isRead })
                        _uiState.update {
                            it.copy(isLoading = false, notifications = res.data, errorMessage = null)
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = res.message)
                    }
                }
            }
        }
    }

    fun selectTab(tab: NotificationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun markAsRead(notificationId: Long) {
        val updatedList = _uiState.value.notifications.map {
            if (it.notificationId == notificationId) it.copy(isRead = true) else it
        }
        _uiState.update { it.copy(notifications = updatedList) }
        if (updatedList.isNotEmpty()) {
            NotificationEventBus.updateUnreadCount(updatedList.count { !it.isRead })
        }
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId).collect { res ->
                if (res is Resource.Success) {
                    NotificationEventBus.notifyRefresh()
                }
            }
        }
    }

    fun loadNotificationDetail(notificationId: Long) {
        val cached = _uiState.value.notifications.firstOrNull { it.notificationId == notificationId }
        if (cached != null) {
            _uiState.update {
                it.copy(
                    detailNotification = cached.copy(isRead = true),
                    isDetailLoading = false,
                    detailError = null
                )
            }
            if (!cached.isRead) {
                markAsRead(notificationId)
            }
            return
        }

        viewModelScope.launch {
            notificationRepository.getNotificationDetail(notificationId).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isDetailLoading = true, detailError = null, detailNotification = null)
                    }
                    is Resource.Success -> {
                        val item = res.data
                        _uiState.update {
                            it.copy(
                                isDetailLoading = false,
                                detailNotification = item.copy(isRead = true),
                                detailError = null
                            )
                        }
                        if (!item.isRead) {
                            markAsRead(notificationId)
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isDetailLoading = false, detailError = res.message)
                    }
                }
            }
        }
    }

    fun markAllAsRead() {
        if (_uiState.value.unreadCount == 0 || _uiState.value.isMarkingAll) return
        viewModelScope.launch {
            notificationRepository.markAllAsRead().collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.update { it.copy(isMarkingAll = true) }
                    is Resource.Success -> {
                        NotificationEventBus.updateUnreadCount(0)
                        _uiState.update { state ->
                            state.copy(
                                isMarkingAll = false,
                                notifications = state.notifications.map { it.copy(isRead = true) }
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isMarkingAll = false, errorMessage = res.message)
                    }
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NotificationViewModel(
                    notificationRepository = CineOneApp.instance.repositoryModule.notificationRepository
                )
            }
        }
    }
}