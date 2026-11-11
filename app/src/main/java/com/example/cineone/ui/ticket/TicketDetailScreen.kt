package com.example.cineone.ui.ticket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.ui.payment.CinemaBarcode
import com.example.cineone.ui.payment.TicketInfoRow
import com.example.cineone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    navController: NavController,
    ticketId: Long,
    viewModel: TicketViewModel = viewModel(factory = TicketViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currencyFormatter = remember { DecimalFormat("#,### đ") }

    val userEmail = remember {
        context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
            .getString("user_email", "user@cineone.vn") ?: "user@cineone.vn"
    }

    LaunchedEffect(ticketId) {
        viewModel.getTicketDetail(ticketId)
    }

    val ticket = uiState.selectedTicketDetail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết vé điện tử", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading || ticket == null) {
                CircularProgressIndicator(color = CineRedPrimary)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Banner thông báo gửi Email
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CineSuccess.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, CineSuccess.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, tint = CineSuccess, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Vé điện tử & biên lai đã gửi về: $userEmail",
                                color = CineSuccess,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Card Vé Cinema Boarding Pass
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tên phim
                            Text(
                                text = ticket.movieTitle.ifBlank { "CineOne Cinema" },
                                color = CineTextWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(10.dp))

                            Surface(
                                color = CineSuccess.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CineSuccess)
                            ) {
                                Text(
                                    text = "VÉ ĐÃ ĐƯỢC XÁC NHẬN",
                                    color = CineSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = CineDivider)
                            Spacer(Modifier.height(16.dp))

                            TicketInfoRow(label = "Rạp chiếu:", value = ticket.cinemaName)
                            Spacer(Modifier.height(8.dp))
                            TicketInfoRow(label = "Phòng chiếu:", value = ticket.roomName)
                            Spacer(Modifier.height(8.dp))
                            TicketInfoRow(label = "Suất chiếu:", value = ticket.showDateTime)
                            Spacer(Modifier.height(8.dp))
                            TicketInfoRow(
                                label = "Ghế đã đặt:",
                                value = ticket.seatCodes.ifBlank { "Chưa cập nhật" }, // <-- Hiển thị đúng ghế từ server trả về
                                valueColor = CineRedPrimary,
                                isBold = true
                            )
                            Spacer(Modifier.height(8.dp))
                            TicketInfoRow(label = "Phương thức:", value = "VietQR (Đã thanh toán)")
                            Spacer(Modifier.height(8.dp))
                            TicketInfoRow(label = "Tổng thanh toán:", value = currencyFormatter.format(ticket.totalAmount), isBold = true)

                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = CineDivider)
                            Spacer(Modifier.height(18.dp))

                            // KHUNG HIỂN THỊ MÃ QR CHECK-IN
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(210.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    val qrData = ticket.qrCheckInCode.ifBlank { ticket.ticketCode }
                                    TicketQrImage(qrData = qrData, modifier = Modifier.size(190.dp))
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Đưa mã QR này cho nhân viên soát vé tại rạp để vào xem phim",
                                color = CineTextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(12.dp))

                            // Mã vé kèm icon copy
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineInputBackground)
                                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Mã vé: ${ticket.ticketCode}",
                                    color = CineTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("ticket_code", ticket.ticketCode))
                                        Toast.makeText(context, "Đã sao chép mã vé", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Mã vạch Barcode
                            CinemaBarcode(code = ticket.ticketCode)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 3. Các nút thao tác
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Đã gửi lại vé điện tử về email: $userEmail", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                            border = BorderStroke(1.dp, CineBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.MailOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gửi lại Email", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Đã lưu ảnh vé vào thư viện máy!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu ảnh vé", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            navController.navigate(
                                com.example.cineone.navigation.Screen.SupportForm.createRoute(
                                    bookingCode = ticket.ticketCode
                                )
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineRedPrimary),
                        border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gửi yêu cầu hỗ trợ / khiếu nại cho vé này", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextGray),
                        border = BorderStroke(1.dp, CineBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Quay lại danh sách vé", fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun TicketQrImage(qrData: String, modifier: Modifier = Modifier) {
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
                } catch (_: Exception) {
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (imageBitmap != null) {
        Image(bitmap = imageBitmap!!, contentDescription = "Ticket QR Code", modifier = modifier)
    } else if (isLoading) {
        CircularProgressIndicator(color = CineRedPrimary, modifier = Modifier.size(36.dp))
    }
}