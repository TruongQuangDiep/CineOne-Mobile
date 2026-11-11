package com.example.cineone.ui.support

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.model.SupportRequest
import com.example.cineone.domain.model.Ticket
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.theme.*

private enum class SupportStatusTab(val code: String, val label: String) {
    ALL("ALL", "Tất cả"),
    PROCESSING("PROCESSING", "Đang xử lý"),
    DONE("DONE", "Đã xong")
}

@Composable
fun SupportListScreen(
    navController: NavController,
    viewModel: SupportViewModel = viewModel(factory = SupportViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isLoggedIn = remember {
        CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
    }
    var showAllItems by remember { mutableStateOf(false) }
    var requestToDelete by remember { mutableStateOf<SupportRequest?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadMyRequests()
        viewModel.loadUserTicketsForPicker()
    }

    val filteredRequests = remember(uiState.myRequests, uiState.selectedStatusFilter) {
        when (uiState.selectedStatusFilter.uppercase()) {
            "PROCESSING" -> uiState.myRequests.filter {
                val s = it.status.trim().uppercase()
                s == "PENDING" || s == "IN_PROGRESS" || s == "PROCESSING"
            }
            "DONE" -> uiState.myRequests.filter {
                val s = it.status.trim().uppercase()
                s == "RESOLVED" || s == "REJECTED" || s == "CLOSED"
            }
            else -> uiState.myRequests
        }
    }

    val displayedRequests = remember(filteredRequests, showAllItems) {
        if (showAllItems || filteredRequests.size <= 5) filteredRequests
        else filteredRequests.take(5)
    }

    val topRequestId = displayedRequests.firstOrNull()?.requestId
    LaunchedEffect(topRequestId, displayedRequests.size, uiState.selectedStatusFilter) {
        if (displayedRequests.isNotEmpty()) {
            listState.scrollToItem(0)
            kotlinx.coroutines.yield()
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        text = "Hỗ trợ & khiếu nại",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = CineDivider)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Bạn cần hỗ trợ?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chúng tôi luôn sẵn sàng hỗ trợ bạn.",
                    fontSize = 13.sp,
                    color = CineTextGray
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isLoggedIn) {
                                navController.navigate(Screen.SupportForm.createRoute())
                            } else {
                                navController.navigate(Screen.Login.route)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CineRedPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CineRedPrimary.copy(alpha = 0.12f),
                            contentColor = CineTextWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "+ Gửi yêu cầu mới",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                    }
                }
            }

            HorizontalDivider(
                color = CineDivider,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (!isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Vui lòng đăng nhập để gửi yêu cầu hỗ trợ / khiếu nại và theo dõi tiến độ xử lý.",
                            fontSize = 14.sp,
                            color = CineTextGray,
                            textAlign = TextAlign.Center
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "YÊU CẦU CỦA TÔI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineSurfaceCard)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    ) {
                        SupportStatusTab.entries.forEachIndexed { index, tab ->
                            val isSelected = uiState.selectedStatusFilter.equals(tab.code, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) CineRedPrimary.copy(alpha = 0.22f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        showAllItems = false
                                        viewModel.setStatusFilter(tab.code)
                                    }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CineRedPrimary else CineTextWhite,
                                    maxLines = 1
                                )
                            }
                            if (index < SupportStatusTab.entries.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(34.dp)
                                        .background(CineBorder)
                                )
                            }
                        }
                    }
                }

                when {
                    uiState.isRequestsLoading && uiState.myRequests.isEmpty() -> {
                        LoadingView(message = "Đang tải lịch sử yêu cầu...")
                    }
                    uiState.requestsError != null && uiState.myRequests.isEmpty() -> {
                        ErrorView(
                            message = uiState.requestsError ?: "Không thể tải danh sách yêu cầu.",
                            onRetry = { viewModel.loadMyRequests() }
                        )
                    }
                    filteredRequests.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có yêu cầu nào trong mục này.",
                                fontSize = 14.sp,
                                color = CineTextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedRequests, key = { it.requestId }) { req ->
                                val isPending = req.status.trim().equals("PENDING", ignoreCase = true) && req.response.isNullOrBlank()
                                SupportRequestItemCard(
                                    request = req,
                                    onClick = { viewModel.selectRequestDetail(req) },
                                    onEditClick = if (isPending) {
                                        {
                                            navController.navigate(
                                                Screen.SupportForm.createRoute(
                                                    bookingCode = req.bookingCode ?: "",
                                                    requestId = req.requestId
                                                )
                                            )
                                        }
                                    } else null,
                                    onDeleteClick = if (isPending) {
                                        { requestToDelete = req }
                                    } else null
                                )
                            }

                            if (filteredRequests.size > 5) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = { showAllItems = !showAllItems }) {
                                            Text(
                                                text = if (showAllItems) "Thu gọn" else "Xem thêm",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CineTextWhite
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (showAllItems) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardDoubleArrowDown,
                                                contentDescription = null,
                                                tint = CineTextWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedRequestDetail != null) {
        val detail = uiState.selectedRequestDetail!!
        val isPending = detail.status.trim().equals("PENDING", ignoreCase = true) && detail.response.isNullOrBlank()
        val relatedTicket = remember(detail.bookingCode, detail.bookingId, uiState.userTickets) {
            val code = detail.bookingCode?.trim().orEmpty()
            uiState.userTickets.find { t ->
                val tCode = t.ticketCode.ifBlank { "TCK-${t.ticketId}" }
                (code.isNotBlank() && tCode.equals(code, ignoreCase = true)) ||
                    (detail.bookingId != null && t.ticketId == detail.bookingId)
            }
        }
        SupportRequestDetailDialog(
            request = detail,
            relatedTicket = relatedTicket,
            onDismiss = { viewModel.selectRequestDetail(null) },
            onEditClick = if (isPending) {
                {
                    viewModel.selectRequestDetail(null)
                    navController.navigate(
                        Screen.SupportForm.createRoute(
                            bookingCode = detail.bookingCode ?: "",
                            requestId = detail.requestId
                        )
                    )
                }
            } else null,
            onDeleteClick = if (isPending) {
                {
                    viewModel.selectRequestDetail(null)
                    requestToDelete = detail
                }
            } else null
        )
    }

    if (requestToDelete != null) {
        val target = requestToDelete!!
        AlertDialog(
            onDismissRequest = { if (!uiState.isDeleting) requestToDelete = null },
            containerColor = CineSurfaceCard,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text(
                    text = "Xác nhận xóa yêu cầu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa yêu cầu \"${formatDisplayType(target.type)} • ${formatDisplayCategory(target.category)}\" này không?",
                    fontSize = 13.sp,
                    color = CineTextGray,
                    lineHeight = 19.sp
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { requestToDelete = null },
                    enabled = !uiState.isDeleting,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CineBorder)
                ) {
                    Text("Hủy", color = CineTextWhite)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupportRequest(
                            requestId = target.requestId,
                            onSuccess = {
                                requestToDelete = null
                                Toast.makeText(context, "Đã xóa yêu cầu thành công!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                requestToDelete = null
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = !uiState.isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun formatDisplayType(type: String): String {
    val u = type.trim().uppercase()
    return if (u.contains("KHIẾU") || u.contains("KHIEU") || u == "COMPLAINT") "Khiếu nại" else "Hỗ trợ"
}

private fun formatDisplayCategory(category: String): String {
    return when (category.trim().uppercase()) {
        "THANH TOÁN", "PAYMENT" -> "Thanh toán"
        "VÉ", "TICKET", "BOOKING" -> "Vé"
        "ĐÁNH GIÁ", "REVIEW" -> "Đánh giá"
        "TÀI KHOẢN", "ACCOUNT" -> "Tài khoản"
        "KHÁC", "OTHER" -> "Khác"
        else -> category
    }
}

@Composable
private fun statusLabelAndColor(status: String): Pair<String, Color> {
    return when (status.trim().uppercase()) {
        "PENDING" -> "Chờ xử lý" to CineWarning
        "IN_PROGRESS", "PROCESSING" -> "Đang xử lý" to Color(0xFF38BDF8)
        "RESOLVED" -> "Đã giải quyết" to CineSuccess
        "REJECTED", "CLOSED" -> "Từ chối" to CineError
        else -> status to CineTextGray
    }
}

@Composable
private fun SupportRequestItemCard(
    request: SupportRequest,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val (statusText, statusColor) = statusLabelAndColor(request.status)
    val dateOnly = request.createdAt.substringBefore(" ").ifBlank { request.createdAt }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDisplayType(request.type)} • ${formatDisplayCategory(request.category)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa yêu cầu",
                                tint = CineTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Xóa yêu cầu",
                                tint = CineError,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = request.content,
                fontSize = 13.sp,
                color = CineTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateOnly,
                    fontSize = 12.sp,
                    color = CineTextMuted
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Xem chi tiết",
                    tint = CineTextGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SupportStatusBadge(status: String) {
    val (label, color) = when (status.trim().uppercase()) {
        "PENDING" -> "Chờ xử lý" to CineWarning
        "IN_PROGRESS", "PROCESSING" -> "Đang xử lý" to Color(0xFF38BDF8)
        "RESOLVED" -> "Đã giải quyết" to CineSuccess
        "REJECTED", "CLOSED" -> "Từ chối" to CineError
        else -> status to CineTextGray
    }

    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SupportRequestDetailDialog(
    request: SupportRequest,
    relatedTicket: Ticket? = null,
    onDismiss: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CineSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDisplayType(request.type)} • ${formatDisplayCategory(request.category)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SupportStatusBadge(status = request.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!request.bookingCode.isNullOrBlank() || relatedTicket != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (relatedTicket != null && relatedTicket.movieTitle.isNotBlank()) {
                                Text(
                                    text = relatedTicket.movieTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite
                                )
                            }
                            Text(
                                text = "Mã vé liên quan: ${request.bookingCode ?: relatedTicket?.ticketCode.orEmpty()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineWarning
                            )
                            if (relatedTicket != null) {
                                val cinemaRoom = listOfNotNull(
                                    relatedTicket.cinemaName.takeIf { it.isNotBlank() },
                                    relatedTicket.roomName.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                if (cinemaRoom.isNotBlank()) {
                                    Text(
                                        text = "Rạp: $cinemaRoom",
                                        fontSize = 12.sp,
                                        color = CineTextGray
                                    )
                                }
                                val showDateTime = "${relatedTicket.showDate} ${relatedTicket.showTime}".trim()
                                val seatInfo = relatedTicket.seatCodes.takeIf { it.isNotBlank() }?.let { " • Ghế: $it" }.orEmpty()
                                if (showDateTime.isNotBlank() || seatInfo.isNotBlank()) {
                                    Text(
                                        text = "Suất chiếu: $showDateTime$seatInfo",
                                        fontSize = 12.sp,
                                        color = CineTextGray
                                    )
                                }
                            }
                        }
                    }
                }

                if (request.createdAt.isNotBlank()) {
                    Text(
                        text = "Thời gian gửi: ${request.createdAt}",
                        fontSize = 12.sp,
                        color = CineTextGray
                    )
                }

                HorizontalDivider(color = CineDivider)

                Text(
                    text = "Nội dung yêu cầu:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextGray
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CineInputBackground)
                        .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = request.content,
                        fontSize = 13.sp,
                        color = CineTextWhite,
                        lineHeight = 19.sp
                    )
                }

                HorizontalDivider(color = CineDivider)

                Text(
                    text = "Phản hồi từ Admin:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineSuccess
                )

                if (!request.response.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineSuccess.copy(alpha = 0.1f))
                            .border(1.dp, CineSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = request.response,
                                fontSize = 13.sp,
                                color = CineTextWhite,
                                lineHeight = 19.sp
                            )
                            if (!request.respondedAt.isNullOrBlank()) {
                                Text(
                                    text = "Phản hồi lúc: ${request.respondedAt}",
                                    fontSize = 11.sp,
                                    color = CineTextGray
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Yêu cầu của bạn đang được bộ phận Chăm sóc Khách hàng tiếp nhận và xử lý.",
                            fontSize = 12.sp,
                            color = CineTextGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDeleteClick != null) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CineError),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = CineError,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa", color = CineError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (onEditClick != null) {
                    OutlinedButton(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CineBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CineTextWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sửa", color = CineTextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Đóng", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportFormScreen(
    navController: NavController,
    initialBookingCode: String = "",
    editRequestId: Long = 0L,
    viewModel: SupportViewModel = viewModel(factory = SupportViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isEditMode = editRequestId > 0L

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var ticketPickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialBookingCode, editRequestId) {
        viewModel.resetForm(initialBookingCode = initialBookingCode)
        viewModel.loadUserTicketsForPicker()
        if (editRequestId > 0L) {
            viewModel.loadRequestForEdit(editRequestId)
        }
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        text = if (isEditMode) "Chỉnh sửa yêu cầu" else "Gửi yêu cầu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = CineDivider)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Loại yêu cầu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite
                )
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = formatDisplayType(uiState.formType),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CineInputBackground,
                            unfocusedContainerColor = CineInputBackground,
                            focusedBorderColor = CineRedPrimary,
                            unfocusedBorderColor = CineBorder,
                            focusedTextColor = CineTextWhite,
                            unfocusedTextColor = CineTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.background(CineSurfaceCard)
                    ) {
                        SupportViewModel.TYPE_OPTIONS.forEach { typeOption ->
                            val isSelected = uiState.formType.equals(typeOption, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = formatDisplayType(typeOption),
                                        color = if (isSelected) CineRedPrimary else CineTextWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setFormType(typeOption)
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Vấn đề",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite
                )
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = formatDisplayCategory(uiState.formCategory),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CineInputBackground,
                            unfocusedContainerColor = CineInputBackground,
                            focusedBorderColor = CineRedPrimary,
                            unfocusedBorderColor = CineBorder,
                            focusedTextColor = CineTextWhite,
                            unfocusedTextColor = CineTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.background(CineSurfaceCard)
                    ) {
                        SupportViewModel.CATEGORY_OPTIONS.forEach { cat ->
                            val isSelected = cat.equals(uiState.formCategory, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = formatDisplayCategory(cat),
                                        color = if (isSelected) CineRedPrimary else CineTextWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setFormCategory(cat)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            val selectedTicket = remember(uiState.formBookingCode, uiState.userTickets) {
                uiState.userTickets.find {
                    val code = it.ticketCode.ifBlank { "TCK-${it.ticketId}" }
                    code.equals(uiState.formBookingCode, ignoreCase = true)
                }
            }
            val displayTicketValue = when {
                uiState.formBookingCode.isBlank() -> ""
                selectedTicket != null -> "${selectedTicket.movieTitle} • ${uiState.formBookingCode}"
                else -> uiState.formBookingCode
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mã vé liên quan (Tùy chọn)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite
                )

                ExposedDropdownMenuBox(
                    expanded = ticketPickerExpanded,
                    onExpandedChange = { ticketPickerExpanded = !ticketPickerExpanded }
                ) {
                    OutlinedTextField(
                        value = displayTicketValue,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text(
                                text = "Chọn vé từ danh sách vé của tôi...",
                                fontSize = 13.sp,
                                color = CineTextMuted
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.formBookingCode.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setFormBookingCode("", null) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Bỏ chọn mã vé",
                                            tint = CineTextGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = ticketPickerExpanded)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CineInputBackground,
                            unfocusedContainerColor = CineInputBackground,
                            focusedBorderColor = CineRedPrimary,
                            unfocusedBorderColor = CineBorder,
                            focusedTextColor = CineTextWhite,
                            unfocusedTextColor = CineTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = ticketPickerExpanded,
                        onDismissRequest = { ticketPickerExpanded = false },
                        modifier = Modifier
                            .background(CineSurfaceCard)
                            .heightIn(max = 300.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Không chọn mã vé (Tự mô tả trong nội dung)",
                                    fontSize = 13.sp,
                                    color = if (uiState.formBookingCode.isBlank()) CineRedPrimary else CineTextGray,
                                    fontWeight = if (uiState.formBookingCode.isBlank()) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setFormBookingCode("", null)
                                ticketPickerExpanded = false
                            }
                        )

                        if (uiState.userTickets.isEmpty()) {
                            HorizontalDivider(color = CineDivider)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Bạn chưa có vé nào — vui lòng mô tả trong phần nội dung.",
                                        fontSize = 12.sp,
                                        color = CineTextMuted
                                    )
                                },
                                onClick = { ticketPickerExpanded = false },
                                enabled = false
                            )
                        } else {
                            uiState.userTickets.forEach { t ->
                                val code = t.ticketCode.ifBlank { "TCK-${t.ticketId}" }
                                val isSelected = uiState.formBookingCode.equals(code, ignoreCase = true)
                                HorizontalDivider(color = CineDivider)
                                DropdownMenuItem(
                                    text = {
                                        Column(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "${t.movieTitle} • $code",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CineRedPrimary else CineTextWhite
                                            )
                                            Text(
                                                text = "${t.cinemaName} • ${t.showDate} ${t.showTime}".trim(),
                                                fontSize = 11.sp,
                                                color = CineTextGray
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setFormBookingCode(code, null)
                                        ticketPickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nội dung",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite
                )

                OutlinedTextField(
                    value = uiState.formContent,
                    onValueChange = viewModel::setFormContent,
                    placeholder = {
                        Text(
                            text = "Mô tả vấn đề của bạn...",
                            color = CineTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CineInputBackground,
                        unfocusedContainerColor = CineInputBackground,
                        focusedBorderColor = CineRedPrimary,
                        unfocusedBorderColor = CineBorder,
                        focusedTextColor = CineTextWhite,
                        unfocusedTextColor = CineTextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 240.dp)
                )
            }

            if (!uiState.submitErrorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.submitErrorMessage ?: "",
                    color = CineError,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.submitSupportRequest {
                        Toast.makeText(
                            context,
                            if (isEditMode) "Cập nhật yêu cầu thành công!" else "Gửi yêu cầu thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navController.popBackStack()
                    }
                },
                enabled = !uiState.isSubmitting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isEditMode) "Cập nhật yêu cầu" else "Gửi yêu cầu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun FaqScreen(
    navController: NavController,
    viewModel: SupportViewModel = viewModel(factory = SupportViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var expandedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        viewModel.loadFaqList()
    }

    val faqGroupItems = remember(uiState.faqItems) {
        uiState.faqItems.filter { it.category.trim().uppercase() == "FAQ" }
    }
    val guideGroupItems = remember(uiState.faqItems) {
        uiState.faqItems.filter {
            val c = it.category.trim().uppercase()
            c == "HƯỚNG DẪN" || c == "GUIDE"
        }
    }
    val policyGroupItems = remember(uiState.faqItems) {
        uiState.faqItems.filter {
            val c = it.category.trim().uppercase()
            c == "CHÍNH SÁCH" || c == "POLICY"
        }
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
                    text = "Chính sách & Điều khoản",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.faqSearchQuery,
                onValueChange = viewModel::onFaqSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Tìm kiếm câu hỏi hoặc nội dung",
                        fontSize = 13.sp,
                        color = CineTextGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = CineTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.faqSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onFaqSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa",
                                tint = CineTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = CineBorder,
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
            ) {
                FaqFilterChip.entries.forEachIndexed { index, chip ->
                    val isSelected = uiState.selectedFaqChip == chip
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) CineRedPrimary.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.onFaqChipSelected(chip) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chip.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CineRedPrimary else CineTextWhite,
                            maxLines = 1
                        )
                    }
                    if (index < FaqFilterChip.entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(CineBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                uiState.isFaqLoading && uiState.faqItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CineRedPrimary)
                    }
                }
                uiState.faqItems.isEmpty() -> {
                    EmptyView(
                        message = "Không tìm thấy câu hỏi hoặc nội dung phù hợp.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
                else -> {
                    if (faqGroupItems.isNotEmpty()) {
                        FaqCategorySection(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Câu hỏi thường gặp",
                            items = faqGroupItems,
                            expandedIds = expandedIds,
                            onToggle = { id ->
                                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    if (guideGroupItems.isNotEmpty()) {
                        FaqCategorySection(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "Hướng dẫn",
                            items = guideGroupItems,
                            expandedIds = expandedIds,
                            onToggle = { id ->
                                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    if (policyGroupItems.isNotEmpty()) {
                        FaqCategorySection(
                            icon = Icons.Default.Description,
                            title = "Chính sách",
                            items = policyGroupItems,
                            expandedIds = expandedIds,
                            onToggle = { id ->
                                expandedIds = if (id in expandedIds) expandedIds - id else expandedIds + id
                            }
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }

            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Assistant.route) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CineRedPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CineRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trợ lý AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                }

                Button(
                    onClick = { navController.navigate(Screen.SupportList.route) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hỗ trợ & Khiếu nại",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqCategorySection(
    icon: ImageVector,
    title: String,
    items: List<FaqItem>,
    expandedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CineTextWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
        }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = BorderStroke(1.dp, CineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    val isExpanded = item.id in expandedIds
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(item.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                                color = CineTextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (isExpanded) CineRedPrimary else CineTextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = CineDivider)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.content,
                                    fontSize = 13.sp,
                                    color = CineTextGray,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    if (index < items.lastIndex) {
                        HorizontalDivider(color = CineDivider)
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(
    navController: NavController = androidx.navigation.compose.rememberNavController()
) {
    SupportListScreen(navController = navController)
}
