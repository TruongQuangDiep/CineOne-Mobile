package com.example.cineone.ui.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.theme.*

@Composable
fun ReviewFormScreen(
    navController: NavController,
    movieId: Long,
    movieTitle: String = "",
    posterUrl: String = "",
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(movieId) {
        viewModel.checkEligibility(movieId) { elig ->
            if (elig.alreadyReviewed) {
                viewModel.prefillForm(elig.existingRating, elig.existingContent)
            }
        }
    }

    val ratingLabels = listOf(
        "Rất tệ",
        "Tệ",
        "Bình thường",
        "Hài lòng",
        "Tuyệt vời"
    )

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(CineBackgroundDark)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Đánh giá",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (posterUrl.isNotBlank()) {
                        MoviePosterBox(
                            posterUrl = posterUrl,
                            title = movieTitle,
                            modifier = Modifier
                                .width(60.dp)
                                .height(86.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movieTitle.ifBlank { "Bộ phim" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Chia sẻ cảm xúc và nhận xét của bạn sau khi xem phim",
                            fontSize = 12.sp,
                            color = CineTextGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bạn đánh giá bộ phim này thế nào?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CineTextWhite
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { starIndex ->
                    val isSelected = uiState.formRating >= starIndex
                    IconButton(
                        onClick = { viewModel.setFormRating(starIndex) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "$starIndex sao",
                            tint = if (isSelected) CineWarning else CineBorder,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val labelIndex = (uiState.formRating - 1).coerceIn(0, 4)
            Text(
                text = "${uiState.formRating} sao - ${ratingLabels[labelIndex]}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CineWarning
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = uiState.formContent,
                        onValueChange = viewModel::setFormContent,
                        placeholder = {
                            Text(
                                text = "Viết cảm nhận của bạn về diễn xuất, kịch bản, âm thanh, hình ảnh...",
                                color = CineTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = CineTextWhite,
                            unfocusedTextColor = CineTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 200.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${uiState.formContent.length}/500",
                        fontSize = 12.sp,
                        color = if (uiState.formContent.length >= 500) CineError else CineTextMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.submitErrorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.submitErrorMessage ?: "",
                    color = CineError,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    viewModel.submitReview(movieId) {
                        Toast.makeText(context, "Gửi đánh giá thành công! Cảm ơn bạn.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                enabled = !uiState.isSubmittingReview,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isSubmittingReview) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Đánh giá",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
