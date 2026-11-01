package com.example.cineone.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    navController: NavController,
    email: String = "",
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val maskedEmail = remember(email) {
        val parts = email.split("@")
        if (parts.size == 2 && parts[0].length > 3) {
            val visible = parts[0].take(4)
            "$visible*****@${parts[1]}"
        } else email
    }

    // Điều kiện mật khẩu
    val hasLength = newPassword.length in 6..20
    val hasUpperAndLowerAndNumber = newPassword.any { it.isUpperCase() } &&
            newPassword.any { it.isLowerCase() } &&
            newPassword.any { it.isDigit() }
    val hasSpecialChar = newPassword.any { "!@#$^*()_".contains(it) }

    // Khi backend đổi mật khẩu thành công -> Về màn hình Đăng nhập
    LaunchedEffect(uiState.isResetPasswordSuccess) {
        if (uiState.isResetPasswordSuccess) {
            viewModel.resetState()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hủy bỏ", fontSize = 16.sp, color = CineTextWhite) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }) {
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

            Text("THIẾT LẬP MẬT KHẨU MỚI", color = CineTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tạo mật khẩu mới cho tài khoản:\n$maskedEmail",
                color = CineTextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Mật khẩu mới (*)
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; localError = null },
                placeholder = { Text("Mật khẩu mới (*)", color = CineTextGray) },
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(imageVector = icon, contentDescription = null, tint = CineTextGray)
                    }
                },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Xác nhận mật khẩu mới (*)
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; localError = null },
                placeholder = { Text("Xác nhận mật khẩu mới (*)", color = CineTextGray) },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = icon, contentDescription = null, tint = CineTextGray)
                    }
                },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Khối Checklist yêu cầu mật khẩu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurfaceCard, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("Yêu cầu mật khẩu:", color = CineTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PasswordRequirementItem(label = "Phải từ 6 đến 20 ký tự", isSatisfied = hasLength)
                PasswordRequirementItem(label = "Bao gồm số, chữ viết hoa, chữ viết thường", isSatisfied = hasUpperAndLowerAndNumber)
                PasswordRequirementItem(label = "Bao gồm ít nhất một ký tự đặc biệt !@#$^*()_", isSatisfied = hasSpecialChar)
            }

            val displayError = localError ?: uiState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = displayError, color = CineError, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    when {
                        !hasLength || !hasUpperAndLowerAndNumber || !hasSpecialChar -> {
                            localError = "Mật khẩu chưa đáp ứng đủ các tiêu chuẩn bảo mật."
                        }
                        newPassword != confirmPassword -> {
                            localError = "Mật khẩu xác nhận không khớp."
                        }
                        else -> {
                            // GỌI API BACKEND ĐỔI MẬT KHẨU THẬT
                            viewModel.resetPassword(email = email.trim(), newPassword = newPassword.trim())
                        }
                    }
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
                    Text("Cập nhật mật khẩu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTextWhite)
                }
            }
        }
    }
}

@Composable
private fun PasswordRequirementItem(label: String, isSatisfied: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Text(
            text = if (isSatisfied) "[✓]" else "[ ]",
            color = if (isSatisfied) CineSuccess else CineTextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isSatisfied) CineTextWhite else CineTextGray,
            fontSize = 12.sp
        )
    }
}
