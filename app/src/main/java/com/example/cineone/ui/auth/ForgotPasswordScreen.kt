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
import androidx.compose.ui.graphics.Color
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
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }
    var isCodeSent by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    // Đếm ngược 59s
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    // Khi backend gửi OTP thành công -> Bật cờ và đếm ngược
    LaunchedEffect(uiState.isForgotPasswordSuccess) {
        if (uiState.isForgotPasswordSuccess) {
            isCodeSent = true
            countdown = 59
            viewModel.resetState()
        }
    }

    // Khi xác thực OTP thành công -> Chuyển sang màn Thiết lập mật khẩu mới
    LaunchedEffect(uiState.isOtpSuccess) {
        if (uiState.isOtpSuccess) {
            viewModel.resetState()
            navController.navigate(Screen.ResetPassword.createRoute(email.trim()))
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

            Text("QUÊN MẬT KHẨU", color = CineTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Vui lòng nhập Email đã đăng ký tài khoản.",
                color = CineTextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Ô nhập Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localError = null },
                placeholder = { Text("Email (*)", color = CineTextGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cụm 6 ô OTP + Nút Lấy mã OTP / 59s
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 6 ô số OTP
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(6) { index ->
                                val char = otpCode.getOrNull(index)?.toString() ?: ""
                                val isFocused = otpCode.length == index
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(CineInputBackground, RoundedCornerShape(8.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isFocused) CineRedPrimary else CineBorderMuted,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = char, color = CineTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Nút "Lấy mã OTP" hoặc "59s"
                if (countdown > 0) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .padding(horizontal = 14.dp)
                            .background(CineBorderMuted, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "${countdown}s", color = CineTextGray, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                localError = "Vui lòng nhập email hợp lệ."
                                return@Button
                            }
                            // GỌI API BACKEND GỬI OTP THẬT
                            viewModel.forgotPassword(email.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Text(text = if (isCodeSent) "Gửi lại" else "Lấy mã OTP", fontSize = 13.sp, color = CineTextWhite)
                    }
                }
            }

            val displayError = localError ?: uiState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = displayError, color = CineError, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(40.dp))

            // Nút Xác nhận ở dưới cùng
            Button(
                onClick = {
                    if (email.isBlank()) {
                        localError = "Vui lòng nhập email."
                        return@Button
                    }
                    if (otpCode.length < 6) {
                        localError = "Vui lòng nhập đủ 6 chữ số OTP."
                        return@Button
                    }
                    // GỌI API XÁC THỰC MÃ OTP VỚI BACKEND
                    viewModel.verifyOtp(email = email.trim(), otp = otpCode.trim(), type = "RESET_PASS")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = CineTextWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Xác nhận", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTextWhite)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}