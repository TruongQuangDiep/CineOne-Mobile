package com.example.cineone.ui.food

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Fnb
import com.example.cineone.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSelectionScreen(
    navController: NavController,
    cinemaId: Long,
    showtimeId: Long,
    movieTitle: String = "",
    cinemaName: String = "",
    startTime: String = "",
    seatNames: String = "",
    seatPriceStr: String = "0",
    viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val seatPrice = seatPriceStr.toDoubleOrNull() ?: 0.0

    LaunchedEffect(cinemaId) {
        viewModel.loadCinemaFnbs(cinemaId = cinemaId, seatPrice = seatPrice)
    }

    val currencyFormatter = remember { DecimalFormat("#,### đ") }
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Combo Bắp & Nước",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                        Text(
                            text = cinemaName.ifBlank { "Rạp chiếu" },
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
            // THANH TỔNG TẠM TÍNH Ở ĐÁY MÀN HÌNH
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { showBottomSheet = true }
                            .padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Xem chi tiết đơn hàng",
                                fontSize = 12.sp,
                                color = CineTextGray
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = CineTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = currencyFormatter.format(uiState.grandTotalPrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineRedPrimary
                        )
                    }

                    Button(
                        onClick = {
                            // Chuyển sang màn hình Thanh toán (SCRUM-31)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Thanh toán", color = CineTextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = CineRedPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorMessage != null && uiState.fnbs.isEmpty() -> {
                    Text(
                        text = uiState.errorMessage ?: "Đã xảy ra lỗi",
                        color = CineError,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.fnbs.isEmpty() -> {
                    Text(
                        text = "Rạp này hiện chưa cập nhật menu bắp nước",
                        color = CineTextGray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // SCRUM-94: DANH SÁCH COMBO BẮP NƯỚC
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.fnbs) { item ->
                            val qty = uiState.cartItems[item.fnbId] ?: 0
                            FnbItemCard(
                                item = item,
                                quantity = qty,
                                onIncrease = { viewModel.increaseQuantity(item.fnbId) },
                                onDecrease = { viewModel.decreaseQuantity(item.fnbId) },
                                currencyFormatter = currencyFormatter
                            )
                        }
                    }
                }
            }

            // SCRUM-95: BOTTOM SHEET TÓM TẮT ĐƠN HÀNG & TẠM TÍNH
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    containerColor = CineSurfaceCard,
                    scrimColor = Color.Black.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "Tóm tắt đơn hàng",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Thông tin phim & rạp
                        Text(text = movieTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CineRedPrimary)
                        Text(text = "$cinemaName • Suất chiếu: $startTime", fontSize = 13.sp, color = CineTextGray)
                        Spacer(modifier = Modifier.height(10.dp))

                        HorizontalDivider(color = CineDivider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Vé xem phim
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ghế ($seatNames)", fontSize = 14.sp, color = CineTextWhite)
                            Text(text = currencyFormatter.format(uiState.seatPrice), fontSize = 14.sp, color = CineTextWhite)
                        }

                        // Danh sách bắp nước đã chọn
                        uiState.cartItems.forEach { (id, qty) ->
                            val item = uiState.fnbs.find { it.fnbId == id }
                            if (item != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.name} (x$qty)",
                                        fontSize = 14.sp,
                                        color = CineTextGray
                                    )
                                    Text(
                                        text = currencyFormatter.format(item.price * qty),
                                        fontSize = 14.sp,
                                        color = CineTextWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CineDivider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Tổng cộng tạm tính
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TỔNG TẠM TÍNH",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite
                            )
                            Text(
                                text = currencyFormatter.format(uiState.grandTotalPrice),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineRedPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// Card món bắp nước kèm nút tăng/giảm số lượng
@Composable
fun FnbItemCard(
    item: Fnb,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    currencyFormatter: DecimalFormat
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CineBorder)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Ảnh minh họa món ăn
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CineInputBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fastfood,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Tên, mô tả, giá
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        color = CineTextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormatter.format(item.price),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineRedPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bộ nút đếm số lượng (-) [Số lượng] (+) đã fix gọn gàng, không bị lấn số
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (quantity > 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, CircleShape)
                            .clickable { onDecrease() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Giảm",
                            tint = CineTextWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = quantity.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.widthIn(min = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CineRedPrimary)
                        .clickable { onIncrease() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tăng",
                        tint = CineTextWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}