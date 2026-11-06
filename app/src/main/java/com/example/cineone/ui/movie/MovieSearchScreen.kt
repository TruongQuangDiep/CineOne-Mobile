package com.example.cineone.ui.movie

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.Showtime
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.showtime.DateItem
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieSearchScreen(
    navController: NavController? = null,
    initialQuery: String = "",
    initialTab: String = "CINEMA",
    initialLocationMode: String = "NONE",
    initialCity: String = "",
    initialGenre: String = "",
    initialDate: String = "",
    initialTimeSlot: String = "ALL",
    viewModel: MovieViewModel = viewModel(factory = MovieViewModel.Factory)
) {
    val uiState by viewModel.searchUiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showProvinceMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(
        uiState.selectedTab,
        uiState.locationMode,
        uiState.selectedCity,
        uiState.selectedDate,
        uiState.selectedTimeSlot
    ) {
        scrollState.scrollTo(0)
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

    LaunchedEffect(initialQuery, initialTab, initialLocationMode, initialCity, initialGenre, initialDate, initialTimeSlot) {
        viewModel.initSearchScreen(
            initialQuery = initialQuery,
            initialTabCode = initialTab,
            initialLocationCode = initialLocationMode,
            initialCity = initialCity,
            initialGenre = initialGenre,
            initialDate = initialDate,
            initialTimeSlot = initialTimeSlot,
            context = context
        )
    }

    Scaffold(
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. THANH TÌM KIẾM ĐA TIÊU CHÍ + NÚT "Hủy" (Đúng thiết kế CineOne_giaodien.pdf)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(CineInputBackground)
                        .border(1.dp, CineBorder, RoundedCornerShape(21.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = CineTextGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm phim, rạp ...",
                                    color = CineTextGray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = CineTextWhite,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(CineRedPrimary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa từ khóa",
                                tint = CineTextGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.onSearchQueryChange("") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Hủy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { navController?.popBackStack() }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. BỘ LỌC VỊ TRÍ (CINE-21.2: Tỉnh thành | Vị trí hiện tại | Vị trí nhà tôi)
            Text(
                text = "Vị trí (chọn 1 trong 3 ô dưới):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ô 1: Tỉnh thành v
                Box(modifier = Modifier.weight(1f)) {
                    val isProvinceSelected =
                        uiState.locationMode == LocationFilterMode.PROVINCE && uiState.selectedCity.isNotBlank()
                    LocationFilterChip(
                        label = if (isProvinceSelected) uiState.selectedCity else "Tỉnh thành",
                        isSelected = isProvinceSelected,
                        showDropdownArrow = true,
                        onClick = { showProvinceMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = showProvinceMenu,
                        onDismissRequest = { showProvinceMenu = false },
                        modifier = Modifier
                            .background(CineSurfaceCard)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Tất cả tỉnh thành",
                                    color = if (uiState.selectedCity.isBlank()) CineRedPrimary else CineTextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (uiState.selectedCity.isBlank()) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                showProvinceMenu = false
                                viewModel.selectProvince("")
                            }
                        )
                        uiState.availableCities.forEach { city ->
                            val selected = uiState.selectedCity.equals(city, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = city,
                                        color = if (selected) CineRedPrimary else CineTextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    showProvinceMenu = false
                                    viewModel.selectProvince(city)
                                }
                            )
                        }
                    }
                }

                // Ô 2: Vị trí hiện tại (GPS)
                LocationFilterChip(
                    label = "Vị trí hiện tại",
                    isSelected = uiState.locationMode == LocationFilterMode.CURRENT_LOCATION,
                    showDropdownArrow = false,
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f)
                )

                // Ô 3: Vị trí nhà tôi (Profile homeAddress)
                LocationFilterChip(
                    label = "Vị trí nhà tôi",
                    isSelected = uiState.locationMode == LocationFilterMode.HOME_LOCATION,
                    showDropdownArrow = false,
                    onClick = {
                        viewModel.activateHomeLocation(context)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!uiState.locationStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.locationStatusMessage!!,
                            fontSize = 11.sp,
                            color = CineTextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (uiState.locationMode != LocationFilterMode.NONE) {
                        Text(
                            text = "Bỏ lọc",
                            fontSize = 11.sp,
                            color = CineRedPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.selectProvince("") }
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CineDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3. 4 TAB TIÊU CHÍ TÌM KIẾM (CINE-21.1: Rạp | Phim | Thể loại | Ngày chiếu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchCategoryTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CineRedPrimary else CineSurfaceCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CineRedPrimary else CineBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.onSearchTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CineTextWhite else CineTextGray,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. NỘI DUNG KẾT QUẢ THEO TỪNG TAB TIÊU CHÍ
            when {
                uiState.isLoading && uiState.allMovies.isEmpty() && uiState.allCinemas.isEmpty() -> {
                    LoadingView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        message = "Đang tải dữ liệu tìm kiếm..."
                    )
                }

                uiState.errorMessage != null && uiState.allMovies.isEmpty() && uiState.allCinemas.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Không thể tải dữ liệu",
                        onRetry = viewModel::refreshSearchCatalog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }

                else -> {
                    when (uiState.selectedTab) {
                        SearchCategoryTab.CINEMA -> {
                            CinemaTabSection(
                                cinemas = uiState.filteredCinemas,
                                visibleCount = uiState.visibleCinemaCount,
                                onCinemaViewClick = { item ->
                                    navController?.navigate(
                                        Screen.CinemaDetail.createRoute(
                                            cinemaId = item.cinema.cinemaId,
                                            cinemaName = item.cinema.name,
                                            date = uiState.selectedDate,
                                            timeSlot = uiState.selectedTimeSlot.name
                                        )
                                    )
                                },
                                onSeeMoreClick = viewModel::loadMoreCinemas
                            )
                        }

                        SearchCategoryTab.MOVIE -> {
                            MovieTabSection(
                                movies = uiState.filteredMovies,
                                visibleCount = uiState.visibleMovieCount,
                                onMovieDetailClick = { movie ->
                                    navController?.navigate(Screen.MovieDetail.createRoute(movie.movieId))
                                },
                                onBookMovieClick = { movie ->
                                    navController?.navigate(
                                        Screen.Showtime.createRoute(
                                            movieId = movie.movieId,
                                            movieTitle = movie.title,
                                            date = uiState.selectedDate,
                                            timeSlot = uiState.selectedTimeSlot.name,
                                            locationMode = uiState.locationMode.name,
                                            city = uiState.selectedCity
                                        )
                                    )
                                },
                                onSeeMoreClick = viewModel::loadMoreMovies
                            )
                        }

                        SearchCategoryTab.GENRE -> {
                            GenreTabSection(
                                availableGenres = uiState.availableGenres,
                                selectedGenres = uiState.selectedGenres,
                                movies = uiState.genreFilteredMovies,
                                visibleCount = uiState.visibleGenreMovieCount,
                                onSelectAllGenres = viewModel::clearGenres,
                                onToggleGenre = viewModel::toggleGenre,
                                onMovieDetailClick = { movie ->
                                    navController?.navigate(Screen.MovieDetail.createRoute(movie.movieId))
                                },
                                onBookMovieClick = { movie ->
                                    navController?.navigate(
                                        Screen.Showtime.createRoute(
                                            movieId = movie.movieId,
                                            movieTitle = movie.title,
                                            date = uiState.selectedDate,
                                            timeSlot = uiState.selectedTimeSlot.name,
                                            locationMode = uiState.locationMode.name,
                                            city = uiState.selectedCity
                                        )
                                    )
                                },
                                onSeeMoreClick = viewModel::loadMoreGenreMovies
                            )
                        }

                        SearchCategoryTab.SHOW_DATE -> {
                            ShowDateTabSection(
                                dates = uiState.dates,
                                selectedTimeSlot = uiState.selectedTimeSlot,
                                isShowtimesLoading = uiState.isShowtimesLoading,
                                dateMovieGroups = uiState.dateMovieGroups,
                                visibleCount = uiState.visibleDateMovieCount,
                                onDateSelected = viewModel::selectSearchDate,
                                onTimeSlotSelected = viewModel::selectSearchTimeSlot,
                                onMovieClick = { movieId ->
                                    navController?.navigate(Screen.MovieDetail.createRoute(movieId))
                                },
                                onShowtimeClick = { movieTitle, cinemaName, showtime ->
                                    navController?.navigate(
                                        Screen.SeatSelection.createRoute(
                                            showtimeId = showtime.showtimeId,
                                            cinemaId = showtime.cinemaId,
                                            movieTitle = movieTitle,
                                            cinemaName = cinemaName,
                                            startTime = showtime.startTime,
                                            basePrice = showtime.price.toString()
                                        )
                                    )
                                },
                                onSeeMoreClick = viewModel::loadMoreDateMovies
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LocationFilterChip(
    label: String,
    isSelected: Boolean,
    showDropdownArrow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CineRedPrimary.copy(alpha = 0.2f) else CineInputBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) CineRedPrimary else CineBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) CineTextWhite else CineTextGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showDropdownArrow) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isSelected) CineRedPrimary else CineTextGray,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun CinemaTabSection(
    cinemas: List<CinemaWithDistance>,
    visibleCount: Int,
    onCinemaViewClick: (CinemaWithDistance) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    if (cinemas.isEmpty()) {
        EmptyResultCard(message = "Không tìm thấy rạp chiếu phù hợp với bộ lọc đã chọn")
        return
    }

    val displayed = cinemas.take(visibleCount)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            displayed.forEachIndexed { index, item ->
                val cinema = item.cinema
                val distText = LocationUtils.formatDistanceKm(item.distanceKm)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCinemaViewClick(item) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biểu tượng rạp
                    CinemaBrandBadgeBox(
                        brandName = cinema.brandName,
                        cinemaName = cinema.name
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cinema.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = cinema.address.ifBlank { cinema.city },
                            fontSize = 12.sp,
                            color = CineTextGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (distText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Cách vị trí đã chọn: $distText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { onCinemaViewClick(item) },
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

                if (index < displayed.lastIndex) {
                    HorizontalDivider(
                        color = CineDivider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (cinemas.size > visibleCount) {
                SeeMoreFooterButton(onClick = onSeeMoreClick)
            }
        }
    }
}

@Composable
private fun MovieTabSection(
    movies: List<Movie>,
    visibleCount: Int,
    onMovieDetailClick: (Movie) -> Unit,
    onBookMovieClick: (Movie) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    if (movies.isEmpty()) {
        EmptyResultCard(message = "Không tìm thấy phim phù hợp với từ khóa")
        return
    }

    val displayed = movies.take(visibleCount)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            displayed.forEachIndexed { index, movie ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMovieDetailClick(movie) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoviePosterBox(
                        posterUrl = movie.posterUrl,
                        title = movie.title,
                        ageRating = normalizeAgeBadge(movie.ageRating),
                        modifier = Modifier
                            .width(50.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MovieStatusBadge(status = movie.status)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    ActionBookButton(
                        text = "Đặt vé",
                        onClick = { onBookMovieClick(movie) }
                    )
                }

                if (index < displayed.lastIndex) {
                    HorizontalDivider(
                        color = CineDivider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (movies.size > visibleCount) {
                SeeMoreFooterButton(onClick = onSeeMoreClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreTabSection(
    availableGenres: List<String>,
    selectedGenres: Set<String>,
    movies: List<Movie>,
    visibleCount: Int,
    onSelectAllGenres: () -> Unit,
    onToggleGenre: (String) -> Unit,
    onMovieDetailClick: (Movie) -> Unit,
    onBookMovieClick: (Movie) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    Text(
        text = "Chọn thể loại phim (có thể chọn nhiều):",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = CineTextGray
    )

    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isAllSelected = selectedGenres.isEmpty()
        GenreChipItem(
            label = "Tất cả",
            isSelected = isAllSelected,
            onClick = onSelectAllGenres
        )
        availableGenres.forEach { genre ->
            val selected = selectedGenres.any { it.equals(genre, ignoreCase = true) }
            GenreChipItem(
                label = genre,
                isSelected = selected,
                onClick = { onToggleGenre(genre) }
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (movies.isEmpty()) {
        EmptyResultCard(message = "Không tìm thấy phim thuộc thể loại đã chọn")
        return
    }

    val displayed = movies.take(visibleCount)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            displayed.forEachIndexed { index, movie ->
                val movieGenreList = remember(movie.genres) {
                    MovieViewModel.splitGenres(movie.genres)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMovieDetailClick(movie) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoviePosterBox(
                        posterUrl = movie.posterUrl,
                        title = movie.title,
                        ageRating = normalizeAgeBadge(movie.ageRating),
                        modifier = Modifier
                            .width(50.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = movie.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (movieGenreList.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                movieGenreList.forEach { g ->
                                    val isHighlighted = selectedGenres.any {
                                        LocationUtils.normalizeSearchText(it) ==
                                            LocationUtils.normalizeSearchText(g)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isHighlighted) CineRedPrimary.copy(alpha = 0.22f)
                                                else CineInputBackground
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isHighlighted) CineRedPrimary else CineBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = g,
                                            fontSize = 11.sp,
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isHighlighted) CineTextWhite else CineTextGray
                                        )
                                    }
                                }
                            }
                        } else {
                            MovieStatusBadge(status = movie.status)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    ActionBookButton(
                        text = "Đặt vé",
                        onClick = { onBookMovieClick(movie) }
                    )
                }

                if (index < displayed.lastIndex) {
                    HorizontalDivider(
                        color = CineDivider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            if (movies.size > visibleCount) {
                SeeMoreFooterButton(onClick = onSeeMoreClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowDateTabSection(
    dates: List<DateItem>,
    selectedTimeSlot: TimeSlotFilter,
    isShowtimesLoading: Boolean,
    dateMovieGroups: List<SearchDateMovieGroup>,
    visibleCount: Int,
    onDateSelected: (String) -> Unit,
    onTimeSlotSelected: (TimeSlotFilter) -> Unit,
    onMovieClick: (Long) -> Unit,
    onShowtimeClick: (movieTitle: String, cinemaName: String, showtime: Showtime) -> Unit,
    onSeeMoreClick: () -> Unit
) {
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
        items(dates, key = { it.dateString }) { dateItem ->
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
                    .clickable { onDateSelected(dateItem.dateString) }
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
            val selected = slot == selectedTimeSlot
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) CineRedPrimary else CineInputBackground)
                    .border(
                        width = 1.dp,
                        color = if (selected) CineRedPrimary else CineBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTimeSlotSelected(slot) }
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

    when {
        isShowtimesLoading -> {
            LoadingView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                message = "Đang tải lịch chiếu theo ngày & khung giờ..."
            )
        }

        dateMovieGroups.isEmpty() -> {
            EmptyResultCard(
                message = "Không có suất chiếu phù hợp trong ngày và khung giờ đã chọn"
            )
        }

        else -> {
            val displayedGroups = dateMovieGroups.take(visibleCount)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                border = BorderStroke(1.dp, CineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    displayedGroups.forEachIndexed { groupIdx, group ->
                        val movie = group.movie
                        val ageBadge = normalizeAgeBadge(movie.ageRating).ifBlank { "P" }

                        // Thông tin phim
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMovieClick(movie.movieId) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoviePosterBox(
                                posterUrl = movie.posterUrl,
                                title = movie.title,
                                ageRating = "",
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = movie.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (movie.duration > 0) {
                                            "Thời lượng: ${movie.duration} phút"
                                        } else {
                                            movie.genres.ifBlank { "Đang chiếu" }
                                        },
                                        fontSize = 12.sp,
                                        color = CineTextGray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CineRedPrimary.copy(alpha = 0.2f))
                                            .border(1.dp, CineRedPrimary, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = ageBadge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineTextWhite
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Danh sách Rạp + Suất chiếu của phim này
                        group.cinemaGroups.forEach { cinemaGroup ->
                            val distBadge = LocationUtils.formatDistanceKm(cinemaGroup.distanceKm)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Theaters,
                                        contentDescription = null,
                                        tint = CineRedPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = buildString {
                                            append(cinemaGroup.cinemaName)
                                            if (cinemaGroup.areaLabel.isNotBlank()) {
                                                append(" [${cinemaGroup.areaLabel}]")
                                            }
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CineTextWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (distBadge.isNotBlank()) {
                                        Text(
                                            text = distBadge,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineWarning
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    cinemaGroup.showtimes.forEach { st ->
                                        val timeStr = DateTimeUtils.formatTime(st.startTime)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CineInputBackground)
                                                .border(1.dp, CineBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    onShowtimeClick(
                                                        movie.title,
                                                        cinemaGroup.cinemaName,
                                                        st
                                                    )
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = timeStr,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CineTextWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (groupIdx < displayedGroups.lastIndex) {
                            HorizontalDivider(
                                color = CineBorderMuted,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }

                    if (dateMovieGroups.size > visibleCount) {
                        SeeMoreFooterButton(onClick = onSeeMoreClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CineRedPrimary else CineInputBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) CineRedPrimary else CineBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) CineTextWhite else CineTextGray
        )
    }
}

@Composable
fun CinemaBrandBadgeBox(
    brandName: String,
    cinemaName: String,
    modifier: Modifier = Modifier
) {
    val shortBrand = remember(brandName, cinemaName) {
        val raw = brandName.ifBlank { cinemaName }.trim()
        when {
            raw.contains("CGV", ignoreCase = true) -> "CGV"
            raw.contains("Beta", ignoreCase = true) -> "BETA"
            raw.contains("Lotte", ignoreCase = true) -> "LOTTE"
            raw.contains("Galaxy", ignoreCase = true) -> "GLX"
            raw.contains("Cinestar", ignoreCase = true) -> "CNS"
            raw.contains("Mega", ignoreCase = true) -> "MEGA"
            raw.contains("BHD", ignoreCase = true) -> "BHD"
            else -> raw.take(4).uppercase()
        }
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CineInputBackground)
            .border(1.dp, CineBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Theaters,
                contentDescription = null,
                tint = CineRedPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = shortBrand,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CineTextWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MovieStatusBadge(status: String) {
    val (label, badgeColor) = when (status.uppercase()) {
        "NOW_SHOWING" -> "Đang chiếu" to CineSuccess
        "SNEAK_SHOW" -> "Chiếu sớm" to CineWarning
        "COMING_SOON" -> "Sắp chiếu" to CineRedPrimary
        else -> "Đang chiếu" to CineSuccess
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.16f))
            .border(1.dp, badgeColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor
        )
    }
}

@Composable
private fun ActionBookButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CineRedPrimary,
            contentColor = CineTextWhite
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SeeMoreFooterButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = CineDivider,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Xem thêm",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CineTextGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CineTextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyResultCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CineSurfaceCard)
            .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        EmptyView(message = message)
    }
}
