package com.example.cineone.ui.showtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Showtime
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowtimeScreen(
    navController: NavController,
    movieId: Long,
    movieTitle: String = "Chọn suất chiếu",
    viewModel: ShowtimeViewModel = viewModel(factory = ShowtimeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự động load dữ liệu khi vào màn hình
    LaunchedEffect(movieId) {
        if (uiState.selectedDate.isNotBlank()) {
            viewModel.loadShowtimes(movieId = movieId, date = uiState.selectedDate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = movieTitle.ifBlank { "Chọn suất chiếu" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lịch chiếu theo rạp",
                            fontSize = 12.sp,
                            color = CineTextGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = CineTextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CineBackgroundDark
                )
            )
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. THANH CHỌN NGÀY CUỘN NGANG (Date Selector)
            DateSelector(
                dates = uiState.dates,
                onDateSelected = { selectedDate ->
                    viewModel.onDateSelected(movieId, selectedDate)
                }
            )

            HorizontalDivider(
                color = CineDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            color = CineRedPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.errorMessage != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "Đã xảy ra lỗi",
                                color = CineError,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadShowtimes(movieId) },
                                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                            ) {
                                Text("Thử lại", color = CineTextWhite)
                            }
                        }
                    }

                    uiState.showtimesByCinema.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Không có suất chiếu nào trong ngày này",
                                color = CineTextGray,
                                fontSize = 15.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(uiState.showtimesByCinema.keys.toList()) { cinemaName ->
                                val showtimes = uiState.showtimesByCinema[cinemaName] ?: emptyList()
                                CinemaShowtimeCard(
                                    cinemaName = cinemaName,
                                    showtimes = showtimes,
                                    onShowtimeClick = { showtime ->
                                        // Chuyển sang màn hình chọn ghế (SCRUM-93)
                                        navController.navigate(
                                            Screen.SeatSelection.createRoute(
                                                showtimeId = showtime.showtimeId,
                                                movieTitle = movieTitle,
                                                cinemaName = cinemaName,
                                                startTime = showtime.startTime,
                                                basePrice = showtime.price.toString()
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    dates: List<DateItem>,
    onDateSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates) { item ->
            val backgroundColor = if (item.isSelected) CineRedPrimary else CineInputBackground
            val borderColor = if (item.isSelected) CineRedPrimary else CineBorder
            val textColor = if (item.isSelected) CineTextWhite else CineTextGray

            Column(
                modifier = Modifier
                    .width(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable { onDateSelected(item.dateString) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.dayOfWeek,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.dayOfMonth,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isSelected) CineTextWhite else CineTextWhite
                )
            }
        }
    }
}

@Composable
fun CinemaShowtimeCard(
    cinemaName: String,
    showtimes: List<Showtime>,
    onShowtimeClick: (Showtime) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Tên Rạp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = cinemaName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowShowtimeGrid(
                showtimes = showtimes,
                onShowtimeClick = onShowtimeClick
            )
        }
    }
}

@Composable
fun FlowShowtimeGrid(
    showtimes: List<Showtime>,
    onShowtimeClick: (Showtime) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(showtimes) { st ->
            val displayTime = formatTime(st.startTime)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    .clickable { onShowtimeClick(st) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                    Text(
                        text = st.roomName.ifBlank { "2D" },
                        fontSize = 11.sp,
                        color = CineTextGray
                    )
                }
            }
        }
    }
}

fun formatTime(isoDateTime: String): String {
    return try {
        val dt = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        if (isoDateTime.length >= 16 && isoDateTime.contains("T")) {
            isoDateTime.substring(11, 16)
        } else {
            isoDateTime
        }
    }
}