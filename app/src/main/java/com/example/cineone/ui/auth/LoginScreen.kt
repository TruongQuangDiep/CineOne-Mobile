package com.example.cineone.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    // 1. Thu thập trạng thái UI từ AuthViewModel (Loading, Error, Success)
    val authState by authViewModel.uiState.collectAsState()

    // 2. Chỉ chuyển sang Home khi Backend xác thực thành công (isSuccess = true)
    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess) {
            authViewModel.resetState() // Reset lại cờ thành công
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineBackgroundDark)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo & Slogan
            Text(
                text = "CineOne",
                color = CineRedPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Đặt vé đa rạp thông minh AI",
                color = CineTextGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Ô nhập Email / SĐT
            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = {
                    usernameOrEmail = it
                    localValidationError = null
                },
                placeholder = { Text("Email hoặc số điện thoại", color = CineTextGray) },
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ô nhập Mật khẩu
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localValidationError = null
                },
                placeholder = { Text("Mật khẩu", color = CineTextGray) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            // Quên mật khẩu?
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = { navController.navigate(Screen.ForgotPassword.route) }) {
                    Text(
                        text = "Quên mật khẩu?",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }

            // Hiển thị lỗi: Ưu tiên lỗi Validate trước, nếu không có thì hiện lỗi trả về từ Backend
            val errorMessage = localValidationError ?: authState.errorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = CineError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nút Đăng nhập
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        usernameOrEmail.isBlank() -> {
                            localValidationError = "Vui lòng nhập email hoặc số điện thoại."
                        }
                        password.isBlank() -> {
                            localValidationError = "Vui lòng nhập mật khẩu."
                        }
                        else -> {
                            // GỌI API BACKEND THÔNG QUA VIEWMODEL
                            authViewModel.login(
                                identifier = usernameOrEmail.trim(),
                                password = password
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                enabled = !authState.isLoading
            ) {
                if (authState.isLoading) {
                    CircularProgressIndicator(
                        color = CineTextWhite,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTextWhite)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Đăng ký
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Chưa có tài khoản? ", color = CineTextGray, fontSize = 14.sp)
                TextButton(
                    onClick = { navController.navigate(Screen.Register.route) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Đăng ký", color = CineRedPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = CineDivider)
                Text(
                    text = "HOẶC",
                    color = CineTextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = CineDivider)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Khám phá với tư cách Khách
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CineBorderMuted)
                )
            ) {
                Text("Khám phá với tư cách Khách", fontSize = 15.sp, color = CineTextWhite)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}