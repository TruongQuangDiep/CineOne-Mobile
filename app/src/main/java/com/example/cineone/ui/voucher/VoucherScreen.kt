package com.example.cineone.ui.voucher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Voucher
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherScreen(
    navController: NavController,
    viewModel: VoucherViewModel = viewModel(factory = VoucherViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tabs = listOf("AVAILABLE" to "Khả dụng", "USED" to "Đã sử dụng", "EXPIRED" to "Hết hạn")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voucher của tôi", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackgroundDark)
            )
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. TabRow 3 tab: Khả dụng | Đã sử dụng | Hết hạn (Ảnh 4)
            TabRow(
                selectedTabIndex = when (uiState.currentTab) {
                    "USED" -> 1
                    "EXPIRED" -> 2
                    else -> 0
                },
                containerColor = CineSurfaceCard,
                contentColor = CineRedPrimary,
                indicator = { tabPositions ->
                    val index = when (uiState.currentTab) {
                        "USED" -> 1
                        "EXPIRED" -> 2
                        else -> 0
                    }
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = CineRedPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, pair ->
                    val selected = when (uiState.currentTab) {
                        "USED" -> 1
                        "EXPIRED" -> 2
                        else -> 0
                    } == index

                    Tab(
                        selected = selected,
                        onClick = { viewModel.switchTab(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                color = if (selected) CineRedPrimary else CineTextGray,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // 2. Danh sách Voucher (Ảnh 4)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = CineRedPrimary)
                } else if (uiState.vouchers.isEmpty()) {
                    Text(
                        text = "Không có voucher nào trong mục này",
                        color = CineTextGray,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.vouchers) { voucher ->
                            VoucherCardItem(
                                voucher = voucher,
                                isAvailable = uiState.currentTab == "AVAILABLE",
                                onUseClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("voucher_code", voucher.code))
                                    context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("selected_voucher_code", voucher.code)
                                        .putString("active_home_tab", "HOME")
                                        .apply()

                                    Toast.makeText(
                                        context,
                                        "Đã lưu mã ${voucher.code}! Hãy chọn phim để áp dụng giảm giá.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                },
                                onItemClick = {
                                    viewModel.selectVoucherForDetail(voucher)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. ModalBottomSheet: Chi tiết voucher (Ảnh 4 bên phải)
        if (uiState.showDetailBottomSheet && uiState.selectedVoucher != null) {
            val v = uiState.selectedVoucher!!
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectVoucherForDetail(null) },
                containerColor = CineSurfaceCard,
                contentColor = CineTextWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Chi tiết voucher",
                        color = CineTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Khung mã voucher + nút Sao chép
                    Surface(
                        color = CineBackgroundDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Mã voucher", color = CineTextGray, fontSize = 11.sp)
                                Text(v.code, color = CineRedPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            TextButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("voucher_code", v.code))
                                    Toast.makeText(context, "Đã sao chép mã ${v.code}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("[ Sao chép ]", color = CineRedPrimary, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Các dòng thông tin chi tiết (Ảnh 4)
                    VoucherDetailRow("Mức giảm:", v.discountDisplay)
                    VoucherDetailRow("Đơn tối thiểu:", v.minOrderDisplay)
                    if (v.maxDiscountDisplay != null) {
                        VoucherDetailRow("Giảm tối đa:", v.maxDiscountDisplay)
                    }
                    if (v.periodDisplay.isNotBlank()) {
                        VoucherDetailRow("Thời gian sử dụng:", v.periodDisplay)
                    }
                    VoucherDetailRow("Số lượt còn lại:", "Còn ${v.remainingUsage} lượt sử dụng")
                    if (!v.description.isNullOrBlank()) {
                        VoucherDetailRow("Mô tả:", v.description)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState.currentTab == "AVAILABLE") {
                        Button(
                            onClick = {
                                viewModel.selectVoucherForDetail(null)

                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("voucher_code", v.code))

                                context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("selected_voucher_code", v.code)
                                    .putString("active_home_tab", "HOME")
                                    .apply()

                                Toast.makeText(
                                    context,
                                    "Đã lưu mã ${v.code}! Hãy chọn phim để áp dụng giảm giá.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Sử dụng ngay", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoucherCardItem(
    voucher: Voucher,
    isAvailable: Boolean,
    onUseClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = if (isAvailable) CineRedPrimary else CineTextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = voucher.title,
                    color = CineTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = voucher.discountDisplay, color = CineRedPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = voucher.minOrderDisplay, color = CineTextGray, fontSize = 12.sp)
            Text(text = "Mã: ${voucher.code}", color = CineTextWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Text(text = voucher.expiryDisplay, color = CineTextMuted, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(12.dp))

            if (isAvailable) {
                OutlinedButton(
                    onClick = onUseClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CineBorderMuted),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite)
                ) {
                    Text("Sử dụng", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun VoucherDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, color = CineTextGray, fontSize = 13.sp, modifier = Modifier.width(130.dp))
        Text(
            text = value,
            color = CineTextWhite,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}