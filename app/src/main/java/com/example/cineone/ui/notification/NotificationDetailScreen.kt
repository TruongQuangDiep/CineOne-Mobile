package com.example.cineone.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.theme.*

@Composable
fun NotificationDetailScreen(
    navController: NavController,
    notificationId: Long,
    viewModel: NotificationViewModel = viewModel(factory = NotificationViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(notificationId) {
        viewModel.loadNotificationDetail(notificationId)
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }
                Text(
                    text = "Chi tiết thông báo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isDetailLoading -> {
                    LoadingView()
                }

                uiState.detailError != null -> {
                    ErrorView(
                        message = uiState.detailError!!,
                        onRetry = { viewModel.loadNotificationDetail(notificationId) }
                    )
                }

                uiState.detailNotification != null -> {
                    val item = uiState.detailNotification!!
                    val meta = remember(item.type) { notificationMeta(item.type) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card tiêu đề + phân loại + thời gian
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CineSurfaceCard)
                                .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(meta.tint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = meta.icon,
                                        contentDescription = meta.label,
                                        tint = meta.tint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(meta.tint.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = meta.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = meta.tint
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite,
                                lineHeight = 26.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = CineTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatNotificationTime(item.createdAt),
                                    fontSize = 13.sp,
                                    color = CineTextGray
                                )
                            }
                        }

                        // Card nội dung đầy đủ
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CineSurfaceCard)
                                .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nội dung chi tiết",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineTextMuted
                            )
                            HorizontalDivider(color = CineDivider)
                            Text(
                                text = item.message,
                                fontSize = 15.sp,
                                color = CineTextWhite,
                                lineHeight = 23.sp
                            )
                        }

                        // Nút điều hướng nhanh theo loại thông báo
                        when (item.type.uppercase()) {
                            "BOOKING", "PAYMENT", "TICKET" -> {
                                Button(
                                    onClick = { navController.navigate(Screen.TicketList.route) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                                ) {
                                    Text("Xem vé của tôi", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            "PROMOTION" -> {
                                Button(
                                    onClick = { navController.navigate(Screen.VoucherList.route) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                                ) {
                                    Text("Xem ví Voucher", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Quay lại", color = CineTextWhite, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}