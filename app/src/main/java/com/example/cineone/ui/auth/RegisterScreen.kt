package com.example.cineone.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val hasLength = password.length in 6..20
    val hasUpperAndLowerAndNumber = password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() }
    val hasSpecialChar = password.any { "!@#$^*()_".contains(it) }

    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Khi backend phản hồi đăng ký thành công -> Chuyển sang màn hình nhập OTP
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetState()
            navController.navigate(Screen.Otp.createRoute(email.trim(), "REGISTER"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quay lại", fontSize = 16.sp, color = CineTextWhite) },
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
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ĐĂNG KÝ",
                color = CineTextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Điền thông tin để tạo tài khoản",
                color = CineTextGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 1. Họ và tên
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; localError = null },
                placeholder = { Text("Họ và tên", color = CineTextGray) },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Email (*)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Số điện thoại
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; localError = null },
                placeholder = { Text("Số điện thoại (Không bắt buộc)", color = CineTextGray) },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Mật khẩu
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localError = null },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Xác nhận mật khẩu
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; localError = null },
                placeholder = { Text("Xác nhận mật khẩu", color = CineTextGray) },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineSurfaceCard, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text("Yêu cầu mật khẩu:", color = CineTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PasswordRequirementItem(label = "Phải từ 6 đến 20 ký tự", isSatisfied = hasLength)
                PasswordRequirementItem(label = "Bao gồm số, chữ viết hoa, chữ viết thường", isSatisfied = hasUpperAndLowerAndNumber)
                PasswordRequirementItem(label = "Bao gồm ít nhất một ký tự đặc biệt !@#$^*()_", isSatisfied = hasSpecialChar)
            }

            val displayError = localError ?: uiState.errorMessage
            if (displayError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = displayError, color = CineError, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nút Tiếp tục
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        fullName.isBlank() || email.isBlank() || password.isBlank() -> {
                            localError = "Vui lòng điền các trường bắt buộc (*)."
                        }
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                            localError = "Email không đúng định dạng."
                        }
                        !hasLength || !hasUpperAndLowerAndNumber || !hasSpecialChar -> {
                            localError = "Mật khẩu chưa đáp ứng đủ các tiêu chuẩn bảo mật."
                        }
                        password != confirmPassword -> {
                            localError = "Mật khẩu xác nhận không khớp."
                        }
                        else -> {
                            viewModel.register(
                                email = email.trim(),
                                pass = password,
                                name = fullName.trim(),
                                phone = phone.trim().ifBlank { null }
                            )
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
                    Text("Tiếp tục", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CineTextWhite)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
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