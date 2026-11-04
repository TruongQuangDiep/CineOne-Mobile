package com.example.cineone.ui.movie

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Movie
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.home.HomeMovieTab
import com.example.cineone.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    navController: NavController? = null,
    initialStatus: String = "NOW_SHOWING",
    viewModel: MovieViewModel = viewModel(factory = MovieViewModel.Factory)
) {
    val uiState by viewModel.listUiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val tabs = remember { HomeMovieTab.entries }

    LaunchedEffect(initialStatus) {
        viewModel.loadMovies(query = "", status = initialStatus)
    }

    val selectedTabIndex = remember(uiState.selectedStatus) {
        tabs.indexOfFirst { it.statusCode == uiState.selectedStatus }.coerceAtLeast(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Danh sách phim",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
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
        ) {
            // Ô tìm kiếm tên phim
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { q ->
                    viewModel.loadMovies(query = q, status = uiState.selectedStatus)
                },
                placeholder = {
                    Text("Tìm tên phim...", color = CineTextGray, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CineTextGray
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.loadMovies(query = "", status = uiState.selectedStatus)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa",
                                tint = CineTextGray
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = CineBorder,
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab trạng thái phim: Đang chiếu | Đặc biệt | Sắp chiếu
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CineBackgroundDark,
                contentColor = CineTextWhite,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                        color = CineRedPrimary,
                        height = 3.dp
                    )
                },
                divider = { HorizontalDivider(color = CineDivider) }
            ) {
                tabs.forEach { tab ->
                    val selected = tab.statusCode == uiState.selectedStatus
                    Tab(
                        selected = selected,
                        onClick = {
                            viewModel.loadMovies(
                                query = uiState.searchQuery,
                                status = tab.statusCode
                            )
                        },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) CineRedPrimary else CineTextGray
                            )
                        }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    LoadingView(modifier = Modifier.fillMaxSize())
                }

                uiState.errorMessage != null && uiState.movies.isEmpty() -> {
                    ErrorView(
                        message = uiState.errorMessage ?: "Không thể tải danh sách phim",
                        onRetry = {
                            viewModel.loadMovies(
                                query = uiState.searchQuery,
                                status = uiState.selectedStatus
                            )
                        }
                    )
                }

                uiState.movies.isEmpty() -> {
                    EmptyView(message = "Không tìm thấy phim phù hợp")
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.movies, key = { it.movieId }) { movie ->
                            MovieGridCard(
                                movie = movie,
                                onClick = {
                                    navController?.navigate(
                                        Screen.MovieDetail.createRoute(movie.movieId)
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

@Composable
private fun MovieGridCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        MoviePosterBox(
            posterUrl = movie.posterUrl,
            title = movie.title,
            ageRating = normalizeAgeBadge(movie.ageRating),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = CineWarning,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatMovieRatingBadge(movie.rating),
                fontSize = 11.sp,
                color = CineTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = movie.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = movie.genres.ifBlank { "Đang cập nhật" },
            fontSize = 12.sp,
            color = CineTextGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatMovieRatingBadge(rating: Double): String {
    if (rating <= 0.0) return "Chưa có đánh giá"
    val score10 = if (rating <= 5.0) rating * 2.0 else rating.coerceAtMost(10.0)
    return String.format(Locale.US, "%.1f/10", score10).replace(".0/10", "/10")
}
