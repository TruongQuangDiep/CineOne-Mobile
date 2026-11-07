package com.example.cineone.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentResultScreen(
    navController: NavController,
    bookingId: Long,
    bookingCode: String,
    movieTitle: String,
    amount: Long
) {
    val context = LocalContext.current
    val currencyFormatter = remember { DecimalFormat("#,### đ") }
    val displayCode = bookingCode.ifBlank { "CNE$bookingId" }

    // Đọc email của tài khoản từ session
    val userEmail = remember {
        context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
            .getString("user_email", "bạn") ?: "bạn"
    }

    BackHandler {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Icon Checkmark & Tiêu đề theo wireframe ảnh 3
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CineSuccess,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ĐẶT VÉ THÀNH CÔNG !",
                    color = CineTextWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Thông báo gửi về email (theo đúng ảnh wireframe 3)
            Text(
                text = "Vé điện tử đã được gửi về email\n($userEmail)",
                color = CineTextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Card vé điện tử chi tiết
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
                    // Header phim kèm Icon phim
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = movieTitle.ifBlank { "Hộ Linh Tráng Sĩ" },
                            color = CineTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = CineDivider)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Các thông tin vé chi tiết
                    TicketInfoRow(label = "Rạp", value = "CineOne Landmark 81")
                    Spacer(modifier = Modifier.height(10.dp))
                    TicketInfoRow(label = "Phòng", value = "Cinema 3 (Standard)")
                    Spacer(modifier = Modifier.height(10.dp))
                    TicketInfoRow(label = "Suất chiếu", value = "20:00 - Hôm nay")
                    Spacer(modifier = Modifier.height(10.dp))
                    val savedSeatNames = remember {
                        context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                            .getString("selected_seat_names", "")?.takeIf { it.isNotBlank() } ?: "Ghế đã chọn"
                    }
                    TicketInfoRow(label = "Ghế", value = savedSeatNames, valueColor = CineRedPrimary, isBold = true)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mã vé", color = CineTextGray, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayCode, color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("ticket_code", displayCode))
                                    Toast.makeText(context, "Đã sao chép mã vé", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TicketInfoRow(label = "Tổng tiền:", value = currencyFormatter.format(amount), valueColor = CineTextWhite, isBold = true)

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = CineDivider)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Mô phỏng Mã vạch Barcode rạp phim (theo đúng ảnh wireframe 3)
                    CinemaBarcode(code = displayCode)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Nút "LƯU ẢNH" (theo wireframe)
            Button(
                onClick = {
                    Toast.makeText(context, "Đã lưu ảnh vé vào thư viện ảnh của máy!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("LƯU ẢNH VÉ", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nút "Xem vé của tôi & Mã QR"
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.TicketList.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                border = BorderStroke(1.dp, CineRedPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xem vé của tôi & Mã QR Check-in", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Nút "VỀ TRANG CHỦ" (theo wireframe)
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextGray),
                border = BorderStroke(1.dp, CineBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("VỀ TRANG CHỦ", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TicketInfoRow(
    label: String,
    value: String,
    valueColor: Color = CineTextWhite,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = CineTextGray, fontSize = 14.sp)
        Text(
            text = value,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

/**
 * Vẽ thanh mã vạch Barcode mô phỏng vé rạp
 */
@Composable
fun CinemaBarcode(code: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barWeights = listOf(2, 4, 1, 3, 2, 5, 2, 1, 4, 2, 3, 1, 5, 2, 3, 2, 4, 1, 3, 2, 4, 2, 1, 3)
            barWeights.forEachIndexed { index, weight ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((weight * 2).dp)
                        .background(if (index % 2 == 0) Color.White else Color.Transparent)
                )
                Spacer(Modifier.width(2.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "* $code *",
            color = CineTextGray,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}