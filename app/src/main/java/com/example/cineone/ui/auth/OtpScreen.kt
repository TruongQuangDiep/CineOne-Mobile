package com.example.cineone.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    navController: NavController,
    email: String = "nguyenana@gmail.com",
    flowType: String = "REGISTER",
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    var otpCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(45) }
    var retryRemaining by remember { mutableIntStateOf(2) }
    var localError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    val maskedEmail = remember(email) {
        val parts = email.split("@")
        if (parts.size == 2 && parts[0].length > 3) {
            val visible = parts[0].take(4)
            "$visible*****@${parts[1]}"
        } else email
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    // Khi xác thực mã OTP thành công -> Điều hướng
    LaunchedEffect(uiState.isOtpSuccess) {
        if (uiState.isOtpSuccess) {
            viewModel.resetState()
            if (flowType == "REGISTER") {
                // Kích hoạt xong: Đưa về màn hình Đăng nhập
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.ResetPassword.createRoute(email))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quay lại", fontSize = 16.sp, color = CineTextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = CineTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackgroundDark)
            )
        },
        containerColor = CineBackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Xác thực OTP", color = CineTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vui lòng nhập mã OTP vừa gửi tới email\n$maskedEmail",
                color = CineTextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 6 ô số nhập OTP
            BasicTextField(
                value = otpCode,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpCode = it
                        localError = null
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(6) { index ->
                            val char = otpCode.getOrNull(index)?.toString() ?: ""
                            val isFocused = otpCode.length == index
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(CineInputBackground, RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFocused) CineRedPrimary else CineBorderMuted,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    color = CineTextWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (countdown > 0) "(Tự) Gửi lại mã (${countdown}s)\n(Còn $retryRemaining/3 lần gửi lại)" else "Bấm để gửi lại mã\n(Còn $retryRemaining/3 lần gửi lại)",
                    color = CineTextGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Mã sẽ hết hạn trong 5 phút",
                    color = CineTextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End
                )
            }

            val displayError = localError ?: uiState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = displayError, color = CineError, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(48.dp))

            // 2 Nút: Hủy bỏ & Xác nhận
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CineBorderMuted)
                    )
                ) {
                    Text("Hủy bỏ", fontSize = 15.sp, color = CineTextWhite)
                }

                Button(
                    onClick = {
                        if (otpCode.length < 6) {
                            localError = "Vui lòng nhập đủ 6 số OTP."
                            return@Button
                        }
                        val apiType = if (flowType == "REGISTER") "EMAIL_VERIFY" else "RESET_PASS"
                        viewModel.verifyOtp(email = email.trim(), otp = otpCode.trim(), type = apiType)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = CineTextWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Xác nhận", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CineTextWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}