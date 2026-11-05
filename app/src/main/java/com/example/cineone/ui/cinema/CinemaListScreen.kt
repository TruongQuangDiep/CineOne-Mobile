package com.example.cineone.ui.cinema

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.movie.CinemaBrandBadgeBox
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaListScreen(
    navController: NavController? = null,
    isEmbeddedInHome: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: CinemaViewModel = viewModel(factory = CinemaViewModel.Factory)
) {
    val uiState by viewModel.listUiState.collectAsState()
    val context = LocalContext.current
    var showLocationMenu by remember { mutableStateOf(false) }
    val cinemaListState = rememberLazyListState()

    LaunchedEffect(uiState.selectedBrand, uiState.locationMode, uiState.selectedCity) {
        cinemaListState.scrollToItem(0)
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

    LaunchedEffect(Unit) {
        viewModel.loadCinemasIfNeeded()
    }

    val locationButtonLabel = when (uiState.locationMode) {
        LocationFilterMode.NONE -> "Vị trí"
        LocationFilterMode.CURRENT_LOCATION -> "Vị trí gần tôi"
        LocationFilterMode.HOME_LOCATION -> "Vị trí gần nhà"
        LocationFilterMode.PROVINCE -> uiState.selectedCity.ifBlank { "Tỉnh thành" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CineBackgroundDark)
    ) {
        // Header: "Chọn theo rạp" + nút tìm kiếm nhanh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isEmbeddedInHome && navController != null) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = CineTextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Chọn theo rạp",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }

            IconButton(
                onClick = {
                    navController?.navigate(
                        Screen.MovieSearch.createRoute(query = "", tab = "CINEMA")
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm rạp",
                    tint = CineTextWhite
                )
            }
        }

        // Hàng chọn thương hiệu rạp: Đề xuất | CGV | Beta | Lotte | Galaxy | Cinestar
        if (uiState.availableBrands.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.availableBrands, key = { it }) { brand ->
                    val selected = uiState.selectedBrand == brand
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectBrand(brand) }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) CineRedPrimary.copy(alpha = 0.2f)
                                    else CineInputBackground
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) CineRedPrimary else CineBorder,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Theaters,
                                contentDescription = brand,
                                tint = if (selected) CineRedPrimary else CineTextGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = brand,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) CineTextWhite else CineTextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = CineDivider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Hàng tiêu đề thương hiệu + nút dropdown "[ Vị trí v ]"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.selectedBrand} (${uiState.filteredCinemas.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )

            Box {
                Row(
                    modifier = Modifier
                        .height(34.dp)
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationButtonLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineTextWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = CineTextGray,
                        modifier = Modifier.size(16.dp)
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
                            val isCitySelected =
                                uiState.locationMode == LocationFilterMode.PROVINCE &&
                                    uiState.selectedCity.equals(city, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Tỉnh/TP: $city",
                                        color = if (isCitySelected) CineRedPrimary else CineTextWhite,
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

        if (!uiState.locationStatusMessage.isNullOrBlank()) {
            Text(
                text = uiState.locationStatusMessage!!,
                fontSize = 11.sp,
                color = CineTextGray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            uiState.isLoading && uiState.allCinemas.isEmpty() -> {
                LoadingView(
                    modifier = Modifier.fillMaxSize(),
                    message = "Đang tải danh sách rạp..."
                )
            }

            uiState.errorMessage != null && uiState.allCinemas.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Không tải được danh sách rạp",
                    onRetry = viewModel::loadAllCinemas,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.filteredCinemas.isEmpty() -> {
                EmptyView(
                    message = "Không tìm thấy rạp chiếu phù hợp",
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    state = cinemaListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.filteredCinemas,
                        key = { "${uiState.selectedBrand}_${uiState.locationMode}_${uiState.selectedCity}_${it.cinema.cinemaId}" }
                    ) { item ->
                        val cinema = item.cinema
                        val distStr = LocationUtils.formatDistanceKm(item.distanceKm)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                            border = BorderStroke(1.dp, CineBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController?.navigate(
                                        Screen.CinemaDetail.createRoute(
                                            cinemaId = cinema.cinemaId,
                                            cinemaName = cinema.name
                                        )
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CinemaBrandBadgeBox(
                                    brandName = cinema.brandName,
                                    cinemaName = cinema.name
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cinema.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineTextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (distStr.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = distStr,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CineWarning
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cinema.address.ifBlank { cinema.city },
                                        fontSize = 12.sp,
                                        color = CineTextGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        navController?.navigate(
                                            Screen.CinemaDetail.createRoute(
                                                cinemaId = cinema.cinemaId,
                                                cinemaName = cinema.name
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CineBorderMuted),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CineInputBackground,
                                        contentColor = CineTextWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Xem",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
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
