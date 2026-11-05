package com.example.cineone.ui.cinema

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.movie.normalizeAgeBadge
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CinemaDetailScreen(
    navController: NavController? = null,
    cinemaId: Long = 0L,
    cinemaName: String = "",
    initialDate: String = "",
    initialTimeSlot: String = "ALL",
    viewModel: CinemaViewModel = viewModel(factory = CinemaViewModel.Factory)
) {
    val uiState by viewModel.detailUiState.collectAsState()
    val movieListState = rememberLazyListState()

    LaunchedEffect(uiState.selectedDate, uiState.selectedTimeSlot) {
        movieListState.scrollToItem(0)
    }

    LaunchedEffect(cinemaId, initialDate, initialTimeSlot) {
        viewModel.initCinemaDetail(
            cinemaId = cinemaId,
            initialDate = initialDate,
            initialTimeSlotCode = initialTimeSlot
        )
    }

    val displayCinemaName = uiState.cinema?.name?.ifBlank { null }
        ?: cinemaName.ifBlank { "Chi tiết rạp" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = displayCinemaName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val addr = uiState.cinema?.address.orEmpty()
                        if (addr.isNotBlank()) {
                            Text(
                                text = addr,
                                fontSize = 11.sp,
                                color = CineTextGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
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
                .padding(horizontal = 16.dp)
        ) {
            // 1. Chọn ngày xem
            Text(
                text = "Chọn ngày xem:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.dates, key = { it.dateString }) { dateItem ->
                    val selected = dateItem.isSelected
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CineRedPrimary else CineInputBackground)
                            .border(
                                width = 1.dp,
                                color = if (selected) CineRedPrimary else CineBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.selectDetailDate(cinemaId, dateItem.dateString)
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dateItem.dayOfWeek,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) CineTextWhite else CineTextGray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateItem.dayOfMonth,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Chọn khung giờ
            Text(
                text = "Chọn khung giờ:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TimeSlotFilter.entries) { slot ->
                    val selected = slot == uiState.selectedTimeSlot
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) CineRedPrimary else CineInputBackground)
                            .border(
                                width = 1.dp,
                                color = if (selected) CineRedPrimary else CineBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectDetailTimeSlot(slot) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = slot.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) CineTextWhite else CineTextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CineDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3. DANH SÁCH PHIM
            Text(
                text = "DANH SÁCH PHIM",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CineTextWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            when {
                uiState.isLoading -> {
                    LoadingView(
                        modifier = Modifier.fillMaxSize(),
                        message = "Đang tải lịch chiếu tại rạp..."
                    )
                }

                uiState.errorMessage != null && uiState.movieShowtimes.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Không thể tải lịch chiếu",
                        onRetry = { viewModel.loadCinemaDetailData(cinemaId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.movieShowtimes.isEmpty() -> {
                    EmptyView(
                        message = "Không có suất chiếu nào tại rạp trong ngày và khung giờ này",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        state = movieListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.movieShowtimes,
                            key = { "${uiState.selectedDate}_${uiState.selectedTimeSlot}_${it.movie.movieId}" }
                        ) { item ->
                            val movie = item.movie
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                                border = BorderStroke(1.dp, CineBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Cột trái: Poster + nút "Chi tiết" (Đúng thiết kế CineOne_giaodien.pdf)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(84.dp)
                                    ) {
                                        MoviePosterBox(
                                            posterUrl = movie.posterUrl,
                                            title = movie.title,
                                            ageRating = normalizeAgeBadge(movie.ageRating),
                                            modifier = Modifier
                                                .width(84.dp)
                                                .height(114.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    navController?.navigate(
                                                        Screen.MovieDetail.createRoute(movie.movieId)
                                                    )
                                                }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                navController?.navigate(
                                                    Screen.MovieDetail.createRoute(movie.movieId)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, CineBorderMuted),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = CineInputBackground,
                                                contentColor = CineTextWhite
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                        ) {
                                            Text(
                                                text = "Chi tiết",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Cột phải: Tên phim, Thời lượng, Các suất chiếu
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = movie.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineTextWhite,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (movie.duration > 0) {
                                                "Thời lượng: ${movie.duration} phút"
                                            } else {
                                                movie.genres.ifBlank { "Phim đang chiếu" }
                                            },
                                            fontSize = 12.sp,
                                            color = CineTextGray
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item.showtimes.forEach { st ->
                                                val displayTime = DateTimeUtils.formatTime(st.startTime)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CineInputBackground)
                                                        .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            navController?.navigate(
                                                                Screen.SeatSelection.createRoute(
                                                                    showtimeId = st.showtimeId,
                                                                    cinemaId = st.cinemaId,
                                                                    movieTitle = movie.title,
                                                                    cinemaName = displayCinemaName,
                                                                    startTime = st.startTime,
                                                                    basePrice = st.price.toString()
                                                                )
                                                            )
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = displayTime,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CineTextWhite
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
