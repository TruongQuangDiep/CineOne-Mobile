package com.example.cineone.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.domain.model.Notification
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun NotificationScreen(
    navController: NavController,
    isEmbeddedInHome: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel(factory = NotificationViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn = remember {
        CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
    }

    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            viewModel.loadNotifications()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CineBackgroundDark)
    ) {
        // Header: Thông báo + Đọc tất cả / Làm mới
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isEmbeddedInHome) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "Thông báo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite,
                modifier = Modifier.weight(1f)
            )

            if (isLoggedIn) {
                if (uiState.unreadCount > 0) {
                    TextButton(
                        onClick = viewModel::markAllAsRead,
                        enabled = !uiState.isMarkingAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đọc tất cả",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CineRedPrimary
                        )
                    }
                }
                IconButton(onClick = viewModel::loadNotifications) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới",
                        tint = CineTextGray
                    )
                }
            }
        }

        if (!isLoggedIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = CineTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Đăng nhập để xem hộp thư thông báo cá nhân của bạn.",
                        color = CineTextGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Login.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Đăng nhập ngay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // Segmented Filter: Tất cả | Quan trọng | Đã xem
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CineSurfaceCard)
                .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NotificationTab.entries.forEach { tab ->
                val selected = uiState.selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) CineRedPrimary else Color.Transparent)
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val label = if (tab == NotificationTab.ALL && uiState.unreadCount > 0) {
                        "${tab.label} (${uiState.unreadCount})"
                    } else {
                        tab.label
                    }
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else CineTextGray,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading && uiState.notifications.isEmpty() -> {
                LoadingView()
            }

            uiState.errorMessage != null && uiState.notifications.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage!!,
                    onRetry = viewModel::loadNotifications
                )
            }

            uiState.filteredNotifications.isEmpty() -> {
                val emptyMsg = when (uiState.selectedTab) {
                    NotificationTab.ALL -> "Bạn chưa có thông báo nào."
                    NotificationTab.IMPORTANT -> "Chưa có thông báo quan trọng nào."
                    NotificationTab.READ -> "Chưa có thông báo nào đã xem."
                }
                EmptyView(message = emptyMsg)
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredNotifications,
                        key = { it.notificationId }
                    ) { item ->
                        NotificationItemCard(
                            item = item,
                            onClick = {
                                if (!item.isRead) {
                                    viewModel.markAsRead(item.notificationId)
                                }
                                navController.navigate(Screen.NotificationDetail.createRoute(item.notificationId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: Notification,
    onClick: () -> Unit
) {
    val meta = remember(item.type) { notificationMeta(item.type) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (!item.isRead) CineSurfaceCard else CineSurfaceCard.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = if (!item.isRead) CineRedPrimary.copy(alpha = 0.45f) else CineBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(meta.tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = meta.icon,
                contentDescription = meta.label,
                tint = meta.tint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    color = CineTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!item.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CineRedPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.message,
                fontSize = 13.sp,
                color = CineTextGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatNotificationTime(item.createdAt),
                    fontSize = 11.sp,
                    color = CineTextMuted
                )
                Text(
                    text = "• ${meta.label}",
                    fontSize = 11.sp,
                    color = meta.tint,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

data class NotificationVisualMeta(
    val icon: ImageVector,
    val tint: Color,
    val label: String
)

internal fun notificationMeta(type: String): NotificationVisualMeta {
    return when (type.trim().uppercase()) {
        "BOOKING", "TICKET" -> NotificationVisualMeta(
            icon = Icons.Outlined.ConfirmationNumber,
            tint = Color(0xFF4CAF50),
            label = "Đặt vé"
        )
        "PAYMENT" -> NotificationVisualMeta(
            icon = Icons.Outlined.ReceiptLong,
            tint = Color(0xFF3B82F6),
            label = "Thanh toán"
        )
        "PROMOTION" -> NotificationVisualMeta(
            icon = Icons.Outlined.Campaign,
            tint = Color(0xFFF59E0B),
            label = "Khuyến mãi"
        )
        "REMINDER" -> NotificationVisualMeta(
            icon = Icons.Outlined.EventAvailable,
            tint = Color(0xFFA855F7),
            label = "Nhắc lịch"
        )
        else -> NotificationVisualMeta(
            icon = Icons.Outlined.NotificationsActive,
            tint = Color(0xFFE50914),
            label = "Hệ thống"
        )
    }
}

internal fun formatNotificationTime(raw: String): String {
    if (raw.isBlank()) return "Vừa xong"
    return try {
        val dt = LocalDateTime.parse(raw.trim().replace(" ", "T"))
        val today = LocalDate.now()
        val timeText = dt.format(DateTimeFormatter.ofPattern("HH:mm"))
        when (dt.toLocalDate()) {
            today -> "$timeText • Hôm nay"
            today.minusDays(1) -> "$timeText • Hôm qua"
            else -> dt.format(DateTimeFormatter.ofPattern("HH:mm • dd/MM/yyyy"))
        }
    } catch (_: Exception) {
        raw.take(16).replace("T", " ")
    }
}