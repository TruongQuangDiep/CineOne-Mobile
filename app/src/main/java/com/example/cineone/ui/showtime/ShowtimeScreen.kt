package com.example.cineone.ui.showtime

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.domain.model.Showtime
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.movie.CinemaBrandBadgeBox
import com.example.cineone.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowtimeScreen(
    navController: NavController,
    movieId: Long,
    movieTitle: String = "Chọn suất chiếu",
    initialDate: String = "",
    initialTimeSlot: String = "ALL",
    initialLocationMode: String = "NONE",
    initialCity: String = "",
    viewModel: ShowtimeViewModel = viewModel(factory = ShowtimeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLocationMenu by remember { mutableStateOf(false) }
    val showtimeListState = rememberLazyListState()

    LaunchedEffect(
        uiState.selectedDate,
        uiState.selectedTimeSlot,
        uiState.selectedBrand,
        uiState.locationMode,
        uiState.selectedCity
    ) {
        showtimeListState.scrollToItem(0)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.activateCurrentLocation(context)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(movieId, initialDate, initialTimeSlot, initialLocationMode, initialCity) {
        viewModel.initWithPreservedCriteria(
            movieId = movieId,
            initialDate = initialDate,
            initialTimeSlot = initialTimeSlot,
            initialLocationMode = initialLocationMode,
            initialCity = initialCity,
            context = context
        )
    }

    val locationButtonLabel = when (uiState.locationMode) {
        LocationFilterMode.NONE -> "Vị trí"
        LocationFilterMode.CURRENT_LOCATION -> "Gần tôi"
        LocationFilterMode.HOME_LOCATION -> "Gần nhà"
        LocationFilterMode.PROVINCE -> uiState.selectedCity.ifBlank { "Tỉnh thành" }
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
            // 1. THANH CHỌN NGÀY XEM
            Text(
                text = "Chọn ngày xem:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            DateSelector(
                dates = uiState.dates,
                onDateSelected = { selectedDate ->
                    viewModel.onDateSelected(movieId, selectedDate)
                }
            )

            // 2. THANH CHỌN KHUNG GIỜ (8:00 - 12:00 | 12:00 - 17:00 | 17:00 - 21:00 | 21:00 - 24:00)
            Text(
                text = "Chọn khung giờ:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            .clickable { viewModel.onTimeSlotSelected(slot) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
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

            HorizontalDivider(
                color = CineDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 3. DANH SÁCH RẠP: Lọc theo Thương hiệu & Khoảng cách Vị trí (Đúng thiết kế CineOne_giaodien.pdf)
            Text(
                text = "DANH SÁCH RẠP",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CineTextWhite,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.availableBrands.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableBrands, key = { it }) { brand ->
                        val selected = uiState.selectedBrand == brand
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) CineRedPrimary.copy(alpha = 0.2f)
                                    else CineInputBackground
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) CineRedPrimary else CineBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.onBrandSelected(brand) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Theaters,
                                contentDescription = null,
                                tint = if (selected) CineRedPrimary else CineTextGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = brand,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) CineTextWhite else CineTextGray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.selectedBrand} (${uiState.cinemaGroups.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )

                Box {
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (uiState.locationMode != LocationFilterMode.NONE)
                                    CineRedPrimary.copy(alpha = 0.2f)
                                else CineInputBackground
                            )
                            .border(
                                width = 1.dp,
                                color = if (uiState.locationMode != LocationFilterMode.NONE)
                                    CineRedPrimary
                                else CineBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showLocationMenu = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationButtonLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CineTextWhite
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = CineTextGray,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLocationMenu,
                        onDismissRequest = { showLocationMenu = false },
                        modifier = Modifier
                            .background(CineSurfaceCard)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Tất cả vị trí",
                                    color = if (uiState.locationMode == LocationFilterMode.NONE)
                                        CineRedPrimary else CineTextWhite,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showLocationMenu = false
                                viewModel.clearLocationFilter()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Vị trí gần tôi (GPS)",
                                    color = if (uiState.locationMode == LocationFilterMode.CURRENT_LOCATION)
                                        CineRedPrimary else CineTextWhite,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showLocationMenu = false
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    viewModel.activateCurrentLocation(context)
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Vị trí gần nhà",
                                    color = if (uiState.locationMode == LocationFilterMode.HOME_LOCATION)
                                        CineRedPrimary else CineTextWhite,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showLocationMenu = false
                                viewModel.activateHomeLocation(context)
                            }
                        )
                        if (uiState.availableCities.isNotEmpty()) {
                            HorizontalDivider(color = CineDivider)
                            uiState.availableCities.forEach { city ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Tỉnh/TP: $city",
                                            color = if (uiState.selectedCity.equals(city, ignoreCase = true))
                                                CineRedPrimary else CineTextWhite,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        showLocationMenu = false
                                        viewModel.selectCityFilter(city)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

                    uiState.errorMessage != null && uiState.cinemaGroups.isEmpty() -> {
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

                    uiState.cinemaGroups.isEmpty() -> {
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
                                text = "Không có suất chiếu phù hợp với bộ lọc hiện tại",
                                color = CineTextGray,
                                fontSize = 15.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = showtimeListState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = uiState.cinemaGroups,
                                key = {
                                    "${uiState.selectedDate}_${uiState.selectedTimeSlot}_${uiState.selectedBrand}_${uiState.locationMode}_${uiState.selectedCity}_${it.cinemaId}"
                                }
                            ) { group ->
                                CinemaShowtimeCard(
                                    cinemaName = group.cinemaName,
                                    brandName = group.brandName,
                                    address = group.address,
                                    distanceKm = group.distanceKm,
                                    showtimes = group.showtimes,
                                    onShowtimeClick = { showtime ->
                                        navController.navigate(
                                            Screen.SeatSelection.createRoute(
                                                showtimeId = showtime.showtimeId,
                                                cinemaId = showtime.cinemaId,
                                                movieTitle = movieTitle,
                                                cinemaName = group.cinemaName,
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
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.dayOfWeek,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.dayOfMonth,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }
        }
    }
}

@Composable
fun CinemaShowtimeCard(
    cinemaName: String,
    showtimes: List<Showtime>,
    onShowtimeClick: (Showtime) -> Unit,
    brandName: String = "",
    address: String = "",
    distanceKm: Double? = null
) {
    val distStr = LocationUtils.formatDistanceKm(distanceKm)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CinemaBrandBadgeBox(
                    brandName = brandName,
                    cinemaName = cinemaName
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cinemaName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (distStr.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = distStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineWarning
                            )
                        }
                    }
                    if (address.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = address,
                            fontSize = 11.sp,
                            color = CineTextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
