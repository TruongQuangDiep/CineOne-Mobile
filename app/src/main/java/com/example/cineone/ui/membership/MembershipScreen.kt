package com.example.cineone.ui.membership

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.data.dto.membership.TierDetailDto
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    navController: NavController,
    viewModel: MembershipViewModel = viewModel(factory = MembershipViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val member = uiState.membershipData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thành viên", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            if (uiState.isLoading) {
                CircularProgressIndicator(color = CineRedPrimary)
            } else if (member != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Thẻ thông tin người dùng & Hạng hiện tại (Ảnh 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = member.fullName.ifBlank { "Thành viên CineOne" },
                                    color = CineTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = member.email,
                                    color = CineTextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Hạng hiện tại",
                                color = CineTextGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = member.currentTier,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 2. Khối Tiến độ lên hạng tiếp theo (Ảnh 3)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val nextTierTitle = if (member.nextTier != null) "Tiến độ lên ${member.nextTier}" else "Bạn đã đạt hạng cao nhất"
                        Text(
                            text = nextTierTitle,
                            color = CineTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dòng số tiền chi tiêu / mục tiêu và phần trăm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val spendingTargetText = if (member.nextTierMinSpendingDisplay != null) {
                                "${member.totalSpentDisplay} / ${member.nextTierMinSpendingDisplay}"
                            } else {
                                member.totalSpentDisplay
                            }

                            Text(
                                text = spendingTargetText,
                                color = CineTextGray,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${member.progressPercentage}%",
                                color = CineRedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Thanh Progress Bar
                        LinearProgressIndicator(
                            progress = { (member.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = CineRedPrimary,
                            trackColor = CineBorderMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (member.nextTier != null) {
                            Text(
                                text = "Còn ${member.spendingToNextTierDisplay} để lên hạng ${member.nextTier}",
                                color = CineTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 3. Khối Quyền lợi hạng hiện tại (Ảnh 3)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quyền lợi hạng ${member.currentTier}",
                            color = CineTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        member.currentTierBenefits.forEach { benefit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CineSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = benefit,
                                    color = CineTextWhite,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 4. Khối Chu kỳ thành viên (Ảnh 3)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Chu kỳ thành viên",
                            color = CineTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = member.membershipCycle,
                            color = CineTextGray,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Nút bấm xem danh sách tất cả các hạng (Ảnh 3)
                    OutlinedButton(
                        onClick = { viewModel.toggleTiersBottomSheet(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CineTextWhite),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite)
                    ) {
                        Text(
                            text = "XEM CÁC HẠNG THÀNH VIÊN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 6. ModalBottomSheet hiển thị "Các hạng thành viên" (lấy động từ DB) (Ảnh 3 bên phải)
        if (uiState.showTiersBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleTiersBottomSheet(false) },
                containerColor = CineSurfaceCard,
                contentColor = CineTextWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Các hạng thành viên",
                        color = CineTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Danh sách các hạng do Admin cấu hình trong DB
                    member?.allTiers?.forEach { tier ->
                        TierItemRow(tier = tier)
                        HorizontalDivider(color = CineDivider, modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // Ghi chú chu kỳ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CineTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chu kỳ hạng thành viên:",
                                color = CineTextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chu kỳ 12 tháng được tính từ thời điểm tạo tài khoản hoặc khi người dùng được nâng lên hạng thành viên mới.",
                                color = CineTextGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TierItemRow(tier: TierDetailDto) {
    val dotColor = try {
        Color(android.graphics.Color.parseColor(tier.colorHex))
    } catch (_: Exception) {
        CineRedPrimary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tier.name,
                    color = CineTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (tier.isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = CineSuccess.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, CineSuccess)
                    ) {
                        Text(
                            text = "HẠNG CỦA BẠN",
                            color = CineSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = tier.minSpendingDisplay,
                color = CineTextGray,
                fontSize = 13.sp
            )
        }
    }
}