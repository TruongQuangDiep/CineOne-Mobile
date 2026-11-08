package com.example.cineone.ui.seat

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Seat
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    navController: NavController,
    showtimeId: Long,
    cinemaId: Long = 1L,
    movieTitle: String = "",
    cinemaName: String = "",
    startTime: String = "",
    basePriceStr: String = "85000",
    viewModel: SeatViewModel = viewModel(factory = SeatViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val basePrice = basePriceStr.toDoubleOrNull() ?: 85000.0

    LaunchedEffect(showtimeId) {
        viewModel.loadSeats(showtimeId = showtimeId, basePrice = basePrice)
    }

    val currencyFormatter = remember { DecimalFormat("#,### đ") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = movieTitle.ifBlank { "Chọn ghế" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$cinemaName • ${formatDisplayTime(startTime)}",
                            fontSize = 12.sp,
                            color = CineTextGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = CineTextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackgroundDark)
            )
        },
        bottomBar = {
            // THANH TỔNG TIỀN VÀ NÚT TIẾP TỤC DƯỚI CÙNG
            Surface(
                color = CineSurfaceCard,
                tonalElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CineBorder)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val selectedSeatNames = uiState.selectedSeatIds.mapNotNull { id ->
                            uiState.seatsByRow.values.flatten().find { it.seatId == id }?.let { "${it.seatRow}${it.seatNumber}" }
                        }.sorted().joinToString(", ")

                        Text(
                            text = if (selectedSeatNames.isBlank()) "Chưa chọn ghế" else "Ghế: $selectedSeatNames",
                            fontSize = 13.sp,
                            color = CineTextGray
                        )
                        Text(
                            text = currencyFormatter.format(uiState.totalPrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineRedPrimary
                        )
                    }

                    Button(
                        onClick = {
                            val selectedSeatNames = uiState.selectedSeatIds.mapNotNull { id ->
                                uiState.seatsByRow.values.flatten().find { it.seatId == id }?.let { "${it.seatRow}${it.seatNumber}" }
                            }.sorted().joinToString(", ")

                            // Lưu seatIds vào session tạm để BookingViewModel đọc được
                            val idsString = uiState.selectedSeatIds.joinToString(",")
                            context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                                .edit()
                                .putString("selected_seat_ids", idsString)
                                .putString("selected_seat_names", selectedSeatNames)
                                .apply()

                            navController.navigate(
                                Screen.FoodSelection.createRoute(
                                    cinemaId = cinemaId,
                                    showtimeId = showtimeId,
                                    movieTitle = movieTitle,
                                    cinemaName = cinemaName,
                                    startTime = startTime,
                                    seatNames = selectedSeatNames,
                                    seatPrice = uiState.totalPrice.toLong().toString()
                                )
                            )
                        },
                        enabled = uiState.selectedSeatIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CineRedPrimary,
                            disabledContainerColor = CineBorderMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Tiếp tục",
                            color = if (uiState.selectedSeatIds.isNotEmpty()) CineTextWhite else CineTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. MÀN HÌNH VÒM CONG (Screen Curved Canvas)
            ScreenArcHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // 2. KHU VỰC VẼ LƯỚI GHẾ COMPOSE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(color = CineRedPrimary)
                    }
                    uiState.errorMessage != null && uiState.seatsByRow.isEmpty() -> {
                        Text(text = uiState.errorMessage ?: "Lỗi tải sơ đồ ghế", color = CineError)
                    }
                    uiState.seatsByRow.isEmpty() -> {
                        Text(text = "Rạp chưa cập nhật sơ đồ ghế", color = CineTextGray)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(uiState.seatsByRow.keys.sorted().toList()) { rowLetter ->
                                val rowSeats = uiState.seatsByRow[rowLetter]?.sortedBy { it.seatNumber } ?: emptyList()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Tên Hàng (A, B, C...)
                                    Text(
                                        text = rowLetter,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CineTextGray,
                                        modifier = Modifier.width(24.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Lưới các ghế trong hàng
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        userScrollEnabled = false
                                    ) {
                                        items(rowSeats) { seat ->
                                            SeatItemView(
                                                seat = seat,
                                                isSelected = uiState.selectedSeatIds.contains(seat.seatId),
                                                isHeldByOther = uiState.heldByOtherSeatIds.contains(seat.seatId),
                                                isBooked = uiState.bookedSeatIds.contains(seat.seatId),
                                                onClick = { viewModel.onSeatClick(seat) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CHÚ THÍCH MÀU GHẾ (Legend)
            SeatLegendBar()
        }
    }
}

// Composable vẽ từng ô ghế
@Composable
fun SeatItemView(
    seat: Seat,
    isSelected: Boolean,
    isHeldByOther: Boolean,
    isBooked: Boolean,
    onClick: () -> Unit
) {
    val isVip = seat.seatType.equals("VIP", ignoreCase = true)

    val (bg, border, textCol) = when {
        isBooked -> Triple(Color(0xFF2B2D33), Color(0xFF3A3D47), Color(0xFF6B7280)) // Đã đặt: Xám tối
        isHeldByOther -> Triple(Color(0xFF7A1C1C), Color(0xFFE50914), CineTextWhite) // Người khác giữ: Đỏ viền sáng
        isSelected -> Triple(CineRedPrimary, CineRedPrimary, CineTextWhite)           // Tôi đang chọn: Đỏ chuẩn
        isVip -> Triple(Color(0xFF382E1E), Color(0xFFF59E0B), Color(0xFFF59E0B))     // VIP trống: Vàng/Cam
        else -> Triple(CineInputBackground, CineBorder, CineTextWhite)                // Thường trống: Đen viền
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable(enabled = !isBooked && !isHeldByOther) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seat.seatNumber.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

// Composable: Màn hình chiếu cong vòm
@Composable
fun ScreenArcHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(28.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height)
                quadraticTo(size.width / 2f, 0f, size.width, size.height)
            }
            drawPath(
                path = path,
                color = CineRedPrimary,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "MÀN HÌNH",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = CineTextMuted,
            letterSpacing = 3.sp
        )
    }
}

// Composable: Chú thích loại ghế dưới đáy
@Composable
fun SeatLegendBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = CineInputBackground, border = CineBorder, label = "Thường")
        LegendItem(color = Color(0xFF382E1E), border = Color(0xFFF59E0B), label = "VIP")
        LegendItem(color = CineRedPrimary, border = CineRedPrimary, label = "Đang chọn")
        LegendItem(color = Color(0xFF7A1C1C), border = Color(0xFFE50914), label = "Đang giữ")
    }
}

@Composable
fun LegendItem(color: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(1.dp, border, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, color = CineTextGray)
    }
}

fun formatDisplayTime(isoDateTime: String): String {
    return if (isoDateTime.length >= 16 && isoDateTime.contains("T")) {
        isoDateTime.substring(11, 16)
    } else {
        isoDateTime
    }
}
