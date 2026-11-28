package com.example.cineone.ui.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSurveyScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val sessionManager = remember { com.example.cineone.core.auth.SessionManager(context) }

    var dateOfBirth by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var pinnedCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Lắng nghe kết quả trả về nếu người dùng bấm "Chọn vị trí trên bản đồ"
    val navBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val updatedAddress = savedStateHandle?.getStateFlow<String?>("updated_home_address", null)?.collectAsState()

    LaunchedEffect(updatedAddress?.value) {
        updatedAddress?.value?.let { addr ->
            if (addr.isNotBlank()) {
                homeAddress = addr
                pinnedCoords = sessionManager.fetchUserHomeCoordinates(addr)
            }
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateOfBirth = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR) - 18,
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun navigateToHome() {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navigateToHome() }) {
                    Text("Bỏ qua", color = CineTextGray, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Icon Avatar đỏ chính giữa
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF261014), shape = CircleShape)
                    .border(1.dp, Color(0xFF6B1D24), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Tiêu đề & phụ đề
            Text(
                text = "Hoàn tất hồ sơ của bạn",
                color = CineTextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Bổ sung thông tin để CineOne đề xuất\ntrải nghiệm phù hợp hơn với bạn.",
                color = CineTextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. MỤC NGÀY SINH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ngày sinh", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Không bắt buộc", color = CineTextGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("DD/MM/YYYY", color = CineTextGray) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = CineRedPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = CineBorder,
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. MỤC ĐỊA CHỈ NHÀ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Địa chỉ nhà", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Không bắt buộc", color = CineTextGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                placeholder = {
                    Text("Nhập địa chỉ hoặc khu vực\nđường, phường, tỉnh/thành phố...", color = CineTextGray, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Home, contentDescription = null, tint = CineRedPrimary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = CineBorder,
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. MỤC VỊ TRÍ TRÊN BẢN ĐỒ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vị trí trên bản đồ", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (pinnedCoords != null) {
                    Text("Đã chọn ghim vị trí", color = CineSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Khung vẽ giả lập bản đồ đêm và ghim đỏ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color(0xFF161922), RoundedCornerShape(12.dp))
                    .border(1.dp, CineBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Vẽ các đường giao thông cách điệu
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val roadColor = Color(0xFF2C3242)
                    drawLine(roadColor, Offset(0f, size.height * 0.35f), Offset(size.width, size.height * 0.45f), strokeWidth = 14f)
                    drawLine(roadColor, Offset(0f, size.height * 0.7f), Offset(size.width, size.height * 0.6f), strokeWidth = 12f)
                    drawLine(roadColor, Offset(size.width * 0.28f, 0f), Offset(size.width * 0.22f, size.height), strokeWidth = 10f)
                    drawLine(roadColor, Offset(size.width * 0.52f, 0f), Offset(size.width * 0.52f, size.height), strokeWidth = 16f)
                    drawLine(roadColor, Offset(size.width * 0.75f, 0f), Offset(size.width * 0.8f, size.height), strokeWidth = 12f)
                }

                // Ghim đỏ ở chính giữa bản đồ
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nút "Chọn vị trí trên bản đồ"
            OutlinedButton(
                onClick = {
                    // Mở màn hình chọn địa chỉ theo Tỉnh/Thành & bản đồ OSM đã có sẵn
                    navController.navigate(Screen.HomeLocationSetup.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CineRedPrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineRedPrimary)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chọn vị trí trên bản đồ", color = CineTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Giúp CineOne tìm các rạp gần khu vực của bạn.",
                color = CineTextGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6. NÚT DUY NHẤT Ở ĐÁY: "Lưu thông tin & Bắt đầu"
            Button(
                onClick = {
                    isLoading = true
                    val cachedUser = sessionManager.fetchCachedUser()
                    val finalAddress = homeAddress.trim().ifBlank { null }
                    val finalDob = dateOfBirth.trim().ifBlank { null }

                    if (finalDob != null) {
                        sessionManager.saveUserDateOfBirth(finalDob)
                    }
                    if (finalAddress != null && pinnedCoords != null) {
                        sessionManager.saveUserHomeCoordinates(
                            lat = pinnedCoords!!.first,
                            lon = pinnedCoords!!.second,
                            address = finalAddress
                        )
                    }

                    authViewModel.updateProfile(
                        fullName = cachedUser?.fullName.orEmpty(),
                        phone = cachedUser?.phone,
                        address = finalAddress,
                        dateOfBirth = finalDob
                    )
                    navigateToHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
            ) {
                Text(
                    text = "Lưu thông tin & Bắt đầu",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}