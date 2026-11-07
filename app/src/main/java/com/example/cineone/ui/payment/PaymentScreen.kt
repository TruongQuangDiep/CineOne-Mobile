package com.example.cineone.ui.payment

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    bookingId: Long,
    bookingCode: String,
    amount: Long,
    movieTitle: String,
    viewModel: PaymentViewModel = viewModel(factory = PaymentViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val currencyFormatter = remember { DecimalFormat("#,### đ") }
    var copiedText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookingId) {
        viewModel.initPayment(
            bookingId = bookingId,
            bookingCode = bookingCode,
            amount = amount,
            movieTitle = movieTitle
        )
    }

    // Tự động thông báo / chuyển trang khi webhook báo thành công
    var showSuccessDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isPaid) {
        if (uiState.isPaid) {
            showSuccessDialog = true
        }
    }

    // Format mm:ss cho đồng hồ đếm ngược
    val minutes = uiState.remainingSeconds / 60
    val seconds = uiState.remainingSeconds % 60
    val timerStr = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quét mã VietQR", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Đồng hồ đếm ngược giữ ghế 10 phút (SCRUM-101)
            Card(
                colors = CardDefaults.cardColors(containerColor = if (uiState.remainingSeconds < 120) CineError.copy(alpha = 0.2f) else CineWarning.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, if (uiState.remainingSeconds < 120) CineError else CineWarning),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = if (uiState.remainingSeconds < 120) CineError else CineWarning)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isExpired) "Đã hết thời gian giữ ghế!" else "Thời gian giữ vé: $timerStr",
                        color = if (uiState.remainingSeconds < 120) CineError else CineWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // 2. Khung hiển thị mã QR VietQR
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("VIETQR / NAPAS 247", color = Color(0xFF003B7A), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    if (uiState.isLoading) {
                        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CineRedPrimary)
                        }
                    } else {
                        // Render ảnh QR từ qrCode string
                        val qrData = uiState.qrCode.ifBlank { uiState.checkoutUrl }
                        QrCodeImage(qrData = qrData, modifier = Modifier.size(220.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Quét mã bằng bất kỳ App Ngân hàng hoặc Ví MoMo/ZaloPay",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Thông tin chuyển khoản chi tiết kèm nút Copy
            Card(
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông tin chuyển khoản", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Số tiền:", color = CineTextGray, fontSize = 14.sp)
                        Text(currencyFormatter.format(uiState.amount), color = CineRedPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Nội dung CK:", color = CineTextGray, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.bookingCode.ifBlank { "CNE$bookingId" }, color = CineTextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.bookingCode.ifBlank { "CNE$bookingId" }))
                                    copiedText = "Đã sao chép nội dung!"
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Nút kiểm tra trạng thái thanh toán thủ công
            OutlinedButton(
                onClick = {
                    viewModel.checkStatusManually { isSuccess, msg ->
                        if (!isSuccess) {
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                border = BorderStroke(1.dp, CineBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = CineRedPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đang kiểm tra...", fontSize = 14.sp)
                } else {
                    Text("Tôi đã chuyển khoản - Kiểm tra ngay", fontSize = 14.sp)
                }
            }
        }
    }

    // Dialog thông báo thanh toán thành công (Webhook báo về)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CineSurfaceCard,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(48.dp)) },
            title = { Text("Thanh toán thành công!", color = CineTextWhite, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Text(
                    "Đơn đặt vé của bạn đã được xác nhận. Vé xem phim đã được lưu vào mục 'Vé của tôi'.",
                    color = CineTextGray,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                ) {
                    Text("Về Trang chủ")
                }
            }
        )
    }

    // Dialog khi hết hạn 10 phút giữ ghế
    if (uiState.isExpired) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = CineSurfaceCard,
            title = { Text("Hết thời gian giữ ghế", color = CineError, fontWeight = FontWeight.Bold) },
            text = { Text("Suất chiếu và ghế đã được giải phóng. Vui lòng quay lại để chọn ghế mới.", color = CineTextWhite) },
            confirmButton = {
                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                ) {
                    Text("Chọn lại ghế")
                }
            }
        )
    }
}

@Composable
fun QrCodeImage(qrData: String, modifier: Modifier = Modifier) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(qrData) {
        if (qrData.isNotBlank()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(qrData, "UTF-8")
                    val url = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$encoded"
                    val stream = URL(url).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    imageBitmap = bitmap.asImageBitmap()
                } catch (_: Exception) { }
                finally {
                    isLoading = false
                }
            }
        }
    }

    if (imageBitmap != null) {
        Image(bitmap = imageBitmap!!, contentDescription = "VietQR Code", modifier = modifier)
    } else if (isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CineRedPrimary)
        }
    }
}
