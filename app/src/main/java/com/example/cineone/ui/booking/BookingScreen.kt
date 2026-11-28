package com.example.cineone.ui.booking

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import java.text.DecimalFormat
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.CheckCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    showtimeId: Long,
    cinemaId: Long,
    movieTitle: String,
    cinemaName: String,
    startTime: String,
    seatNames: String,
    seatPrice: Long,
    foodPrice: Long,
    foodSummary: String,
    viewModel: BookingViewModel = viewModel(factory = BookingViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { DecimalFormat("#,### đ") }
    var showVoucherBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sessionManager = remember { com.example.cineone.core.auth.SessionManager(context) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var verifyPhoneNumber by remember { mutableStateOf(sessionManager.fetchCachedUser()?.phone.orEmpty()) }
    var verifyOtpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPhone by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val authRepo = remember { (context.applicationContext as com.example.cineone.CineOneApp).repositoryModule.authRepository }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    LaunchedEffect(showtimeId) {
        viewModel.initData(
            showtimeId = showtimeId,
            movieTitle = movieTitle,
            cinemaName = cinemaName,
            startTime = startTime,
            seatNames = seatNames,
            seatPrice = seatPrice,
            foodPrice = foodPrice,
            foodSummary = foodSummary
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán đơn hàng", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackgroundDark)
            )
        },
        bottomBar = {
            Surface(
                color = CineSurfaceCard,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, CineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tổng thanh toán", color = CineTextGray, fontSize = 12.sp)
                        Text(
                            text = currencyFormatter.format(uiState.finalAmount),
                            color = CineRedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Button(
                        onClick = {
                            val isPhoneVerified = sessionManager.isPhoneVerified()
                            if (!isPhoneVerified) {
                                showPhoneVerificationDialog = true
                            } else {
                                viewModel.confirmBooking { booking ->
                                    navController.navigate(
                                        Screen.Payment.createRoute(
                                            bookingId = booking.bookingId,
                                            bookingCode = booking.bookingCode,
                                            amount = uiState.finalAmount,
                                            movieTitle = uiState.movieTitle
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = CineTextWhite, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Tiếp tục thanh toán", color = CineTextWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // 1. Card thông tin vé xem phim
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(uiState.movieTitle.ifBlank { "Tên phim" }, color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = CineDivider)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rạp chiếu:", color = CineTextGray, fontSize = 14.sp)
                            Text(uiState.cinemaName.ifBlank { "CineOne Cinema" }, color = CineTextWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Suất chiếu:", color = CineTextGray, fontSize = 14.sp)
                            Text(uiState.startTime.ifBlank { "Hôm nay" }, color = CineTextWhite, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ghế đã chọn:", color = CineTextGray, fontSize = 14.sp)
                            Text(uiState.seatNames.ifBlank { "Chưa chọn ghế" }, color = CineRedPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. Card combo bắp nước (nếu có)
            if (uiState.foodPrice > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Combo Bắp Nước", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.foodSummary, color = CineTextGray, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tiền bắp nước:", color = CineTextGray, fontSize = 14.sp)
                                Text(currencyFormatter.format(uiState.foodPrice), color = CineTextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 3. Card Chọn & Áp dụng Voucher (SCRUM-100)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Voucher / Mã giảm giá", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))

                        val isVoucherApplied = uiState.discountAmount > 0
                        // 1. Hàng nhập mã Voucher & Nút áp dụng
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.voucherCodeInput,
                                onValueChange = { viewModel.onVoucherCodeChanged(it) },
                                placeholder = { Text("Nhập mã voucher...", color = CineTextMuted, fontSize = 14.sp) },
                                singleLine = true,
                                readOnly = isVoucherApplied, // Khóa ô nhập khi đã áp mã thành công
                                colors = OutlinedTextFieldDefaults.colors(
                                    // Khi đã áp mã thành công: viền xanh lá nhẹ, không bị đỏ rực
                                    focusedBorderColor = if (isVoucherApplied) CineSuccess else CineRedPrimary,
                                    unfocusedBorderColor = if (isVoucherApplied) CineSuccess.copy(alpha = 0.6f) else CineBorder,
                                    focusedTextColor = CineTextWhite,
                                    unfocusedTextColor = CineTextWhite,
                                    focusedContainerColor = CineInputBackground,
                                    unfocusedContainerColor = CineInputBackground
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.applyVoucher(uiState.voucherCodeInput) },
                                enabled = !isVoucherApplied && uiState.voucherCodeInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CineRedPrimary,
                                    disabledContainerColor = CineBorderMuted
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Áp dụng", fontSize = 13.sp)
                            }
                        }
                        // 2. Nút chọn từ kho Voucher
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showVoucherBottomSheet = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineRedPrimary),
                            border = BorderStroke(1.dp, CineRedPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocalActivity, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chọn từ kho Voucher của bạn", fontSize = 13.sp)
                        }
                        // 3. THÔNG BÁO LỖI (Inline Alert màu đỏ - Chuẩn theo mockup Ảnh 3)
                        if (uiState.discountAmount == 0L && !uiState.voucherMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF331111), RoundedCornerShape(8.dp))
                                    .border(1.dp, CineRedPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = uiState.voucherMessage ?: "",
                                    color = CineRedPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        // 4. THÔNG BÁO THÀNH CÔNG (Inline Alert màu xanh - Chuẩn theo mockup Ảnh 3)
                        if (isVoucherApplied) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CineSuccess.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .border(1.dp, CineSuccess.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CineSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = uiState.voucherMessage ?: "Đã giảm ${currencyFormatter.format(uiState.discountAmount)}",
                                        color = CineSuccess,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Gỡ voucher",
                                    tint = CineTextGray,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.removeVoucher() }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Card Phương thức thanh toán
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phương thức thanh toán", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CineInputBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, CineRedPrimary, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = true,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CineRedPrimary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = CineRedPrimary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Chuyển khoản VietQR", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tự động xác nhận giao dịch qua payOS", color = CineTextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 5. Bảng kê chi tiết thanh toán
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chi tiết thanh toán", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng tiền vé:", color = CineTextGray, fontSize = 14.sp)
                            Text(currencyFormatter.format(uiState.seatPrice), color = CineTextWhite, fontSize = 14.sp)
                        }
                        if (uiState.foodPrice > 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tổng tiền bắp nước:", color = CineTextGray, fontSize = 14.sp)
                                Text(currencyFormatter.format(uiState.foodPrice), color = CineTextWhite, fontSize = 14.sp)
                            }
                        }
                        if (uiState.discountAmount > 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Giảm giá Voucher:", color = CineSuccess, fontSize = 14.sp)
                                Text("-${currencyFormatter.format(uiState.discountAmount)}", color = CineSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = CineDivider)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Số tiền cần trả:", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(currencyFormatter.format(uiState.finalAmount), color = CineRedPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // BottomSheet chọn Voucher từ kho
    if (showVoucherBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoucherBottomSheet = false },
            containerColor = CineSurfaceCard
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kho Voucher khả dụng", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                if (uiState.availableVouchers.isEmpty()) {
                    Text("Hiện chưa có mã giảm giá nào phù hợp.", color = CineTextGray, fontSize = 14.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.availableVouchers) { voucher ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CineInputBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applyVoucher(voucher.code)
                                        showVoucherBottomSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(voucher.code, color = CineRedPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(voucher.title ?: "Ưu đãi đặt vé", color = CineTextWhite, fontSize = 13.sp)
                                        if (voucher.applicableTierNames.isNotEmpty()) {
                                            Text(
                                                text = "Dành riêng cho: ${voucher.applicableTierNames.joinToString(", ")}",
                                                color = Color(0xFFFFB300),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.applyVoucher(voucher.code)
                                            showVoucherBottomSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                                    ) {
                                        Text("Dùng ngay", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // DIALOG XÁC THỰC SỐ ĐIỆN THOẠI TRƯỚC KHI TẠO VIETQR
    if (showPhoneVerificationDialog) {
        AlertDialog(
            onDismissRequest = { if (!isVerifyingPhone) showPhoneVerificationDialog = false },
            containerColor = CineSurfaceCard,
            title = {
                Text(
                    text = if (!isOtpSent) "Xác thực số điện thoại" else "Nhập mã OTP SMS",
                    color = CineTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (!isOtpSent)
                            "Để bảo vệ quyền lợi đặt vé và nhận vé qua SMS, vui lòng xác thực số điện thoại trước khi thanh toán."
                        else
                            "Mã OTP đã được gửi đến số ${verifyPhoneNumber}. Vui lòng nhập mã để hoàn tất xác thực.",
                        color = CineTextGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (!isOtpSent) {
                        OutlinedTextField(
                            value = verifyPhoneNumber,
                            onValueChange = { verifyPhoneNumber = it; phoneError = null },
                            placeholder = { Text("Nhập số điện thoại (VD: 0912345678)", color = CineTextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CineInputBackground,
                                unfocusedContainerColor = CineInputBackground,
                                focusedBorderColor = CineRedPrimary,
                                unfocusedBorderColor = CineBorder,
                                focusedTextColor = CineTextWhite,
                                unfocusedTextColor = CineTextWhite
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = verifyOtpCode,
                            onValueChange = { if (it.length <= 6) verifyOtpCode = it; phoneError = null },
                            placeholder = { Text("Nhập 6 số OTP...", color = CineTextMuted, fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CineInputBackground,
                                unfocusedContainerColor = CineInputBackground,
                                focusedBorderColor = CineRedPrimary,
                                unfocusedBorderColor = CineBorder,
                                focusedTextColor = CineTextWhite,
                                unfocusedTextColor = CineTextWhite
                            )
                        )
                    }
                    if (phoneError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = phoneError!!, color = CineRedPrimary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isOtpSent) {
                            if (verifyPhoneNumber.trim().length < 9) {
                                phoneError = "Số điện thoại không hợp lệ"
                                return@Button
                            }
                            isVerifyingPhone = true
                            phoneError = null
                            coroutineScope.launch {
                                authRepo.sendPhoneOtp(verifyPhoneNumber.trim()).collect { res ->
                                    isVerifyingPhone = false
                                    when (res) {
                                        is com.example.cineone.core.common.Resource.Success -> {
                                            isOtpSent = true
                                        }
                                        is com.example.cineone.core.common.Resource.Error -> {
                                            phoneError = res.message
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        } else {
                            if (verifyOtpCode.trim().length < 4) {
                                phoneError = "Vui lòng nhập mã OTP"
                                return@Button
                            }
                            isVerifyingPhone = true
                            phoneError = null
                            val currentEmail = sessionManager.fetchUserEmail().orEmpty()
                            coroutineScope.launch {
                                authRepo.verifyPhoneOtp(verifyPhoneNumber.trim(), verifyOtpCode.trim(), currentEmail).collect { res ->
                                    isVerifyingPhone = false
                                    when (res) {
                                        is com.example.cineone.core.common.Resource.Success -> {
                                            showPhoneVerificationDialog = false
                                            Toast.makeText(context, "Xác thực SĐT thành công!", Toast.LENGTH_SHORT).show()
                                            // Tự động tiếp tục tạo Booking và chuyển sang màn hình VietQR
                                            viewModel.confirmBooking { booking ->
                                                navController.navigate(
                                                    Screen.Payment.createRoute(
                                                        bookingId = booking.bookingId,
                                                        bookingCode = booking.bookingCode,
                                                        amount = uiState.finalAmount,
                                                        movieTitle = uiState.movieTitle
                                                    )
                                                )
                                            }
                                        }
                                        is com.example.cineone.core.common.Resource.Error -> {
                                            phoneError = res.message
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isVerifyingPhone
                ) {
                    Text(if (!isOtpSent) "Gửi mã OTP" else "Xác nhận & Thanh toán", fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneVerificationDialog = false }) {
                    Text("Hủy", color = CineTextGray, fontSize = 13.sp)
                }
            }
        )
    }
}
