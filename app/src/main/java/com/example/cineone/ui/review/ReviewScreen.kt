package com.example.cineone.ui.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.MovieReviewsData
import com.example.cineone.domain.model.Review
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.theme.*
import java.util.Locale

@Composable
fun ReviewScreen(
    navController: NavController,
    initialMovieId: Long = 0L,
    initialTab: String = "COMMUNITY",
    isEmbeddedInHome: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var showIneligibilityDialog by remember { mutableStateOf(false) }
    var ineligibilityReason by remember { mutableStateOf("") }
    var selectedMovieForReviews by remember { mutableStateOf<Movie?>(null) }
    var displayedMovieLimit by remember { mutableIntStateOf(10) }
    var displayedMyReviewsLimit by remember { mutableIntStateOf(10) }
    var displayedCommunityReviewsLimit by remember { mutableIntStateOf(10) }

    val isLoggedIn = remember {
        CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
    }

    LaunchedEffect(initialMovieId, initialTab) {
        if (initialTab == "MY_REVIEWS") {
            viewModel.setTab(ReviewTab.MY_REVIEWS)
        } else {
            viewModel.setTab(ReviewTab.COMMUNITY)
        }
        if (initialMovieId > 0L) {
            viewModel.selectMovieById(initialMovieId)
        }
    }

    LaunchedEffect(uiState.selectedMovie) {
        if (uiState.selectedMovie != null) {
            selectedMovieForReviews = uiState.selectedMovie
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CineBackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEmbeddedInHome) {
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
                    }

                    Text(
                        text = "Đánh giá",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLoggedIn) {
                        IconButton(
                            onClick = {
                                viewModel.setTab(
                                    if (uiState.activeTab == ReviewTab.COMMUNITY) ReviewTab.MY_REVIEWS else ReviewTab.COMMUNITY
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.activeTab == ReviewTab.COMMUNITY) Icons.Outlined.RateReview else Icons.Filled.Star,
                                contentDescription = "Chuyển tab",
                                tint = CineRedPrimary
                            )
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = uiState.activeTab.ordinal,
                    containerColor = CineBackgroundDark,
                    contentColor = CineRedPrimary,
                    divider = { HorizontalDivider(color = CineDivider, thickness = 1.dp) }
                ) {
                    ReviewTab.values().forEach { tab ->
                        Tab(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontSize = 14.sp,
                                    fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.activeTab == tab) CineRedPrimary else CineTextGray
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                ReviewTab.COMMUNITY -> {
                    CommunityReviewsTab(
                        movies = uiState.movies,
                        movieReviewStats = uiState.movieReviewStats,
                        isLoading = uiState.isMoviesLoading,
                        errorMessage = uiState.moviesError,
                        searchQuery = uiState.searchQuery,
                        displayedLimit = displayedMovieLimit,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onMovieClick = { movie ->
                            selectedMovieForReviews = movie
                            viewModel.selectMovie(movie)
                        },
                        onReviewActionClick = { movie ->
                            if (!isLoggedIn) {
                                Toast.makeText(context, "Vui lòng đăng nhập để viết đánh giá.", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.Login.route)
                                return@CommunityReviewsTab
                            }
                            viewModel.checkEligibility(movie.movieId) { elig ->
                                if (elig.eligible) {
                                    navController.navigate(
                                        Screen.ReviewForm.createRoute(
                                            movieId = movie.movieId,
                                            movieTitle = movie.title,
                                            posterUrl = movie.posterUrl
                                        )
                                    )
                                } else {
                                    ineligibilityReason = elig.message.ifBlank {
                                        "Bạn chỉ có thể đánh giá sau khi đã mua vé và suất chiếu đã kết thúc."
                                    }
                                    showIneligibilityDialog = true
                                }
                            }
                        },
                        onLoadMore = { displayedMovieLimit += 10 },
                        onRetry = { viewModel.loadMovies(uiState.searchQuery) }
                    )
                }

                ReviewTab.MY_REVIEWS -> {
                    MyReviewsTab(
                        reviews = uiState.myReviews,
                        isLoading = uiState.isMyReviewsLoading,
                        errorMessage = uiState.myReviewsError,
                        isLoggedIn = isLoggedIn,
                        displayedLimit = displayedMyReviewsLimit,
                        onLoginClick = { navController.navigate(Screen.Login.route) },
                        onEditReviewClick = { review ->
                            if (review.movieId != null && review.movieId > 0L) {
                                viewModel.prefillForm(review.rating, review.content)
                                navController.navigate(
                                    Screen.ReviewForm.createRoute(
                                        movieId = review.movieId,
                                        movieTitle = review.movieTitle,
                                        posterUrl = review.posterUrl ?: ""
                                    )
                                )
                            }
                        },
                        onLoadMore = { displayedMyReviewsLimit += 10 },
                        onRetry = viewModel::loadMyReviews
                    )
                }
            }
        }
    }

    if (selectedMovieForReviews != null) {
        val movie = selectedMovieForReviews!!
        MovieReviewsDetailDialog(
            movie = movie,
            reviewsData = uiState.movieReviewsData,
            isLoading = uiState.isMovieReviewsLoading,
            aiSummary = uiState.aiSummary,
            aiSummaryError = uiState.aiSummaryError,
            isAiLoading = uiState.isAiSummaryLoading,
            isCheckingEligibility = uiState.isCheckingEligibility,
            displayedLimit = displayedCommunityReviewsLimit,
            onDismiss = {
                selectedMovieForReviews = null
                viewModel.clearSelectedMovie()
            },
            onRefreshAiSummary = {
                viewModel.refreshAiSummary(movie.movieId)
            },
            onWriteReviewClick = {
                if (!isLoggedIn) {
                    Toast.makeText(context, "Vui lòng đăng nhập để viết đánh giá.", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Login.route)
                    return@MovieReviewsDetailDialog
                }
                viewModel.checkEligibility(movie.movieId) { elig ->
                    if (elig.eligible) {
                        selectedMovieForReviews = null
                        navController.navigate(
                            Screen.ReviewForm.createRoute(
                                movieId = movie.movieId,
                                movieTitle = movie.title,
                                posterUrl = movie.posterUrl
                            )
                        )
                    } else {
                        ineligibilityReason = elig.message.ifBlank {
                            "Bạn chỉ có thể đánh giá sau khi đã mua vé và suất chiếu đã kết thúc."
                        }
                        showIneligibilityDialog = true
                    }
                }
            },
            onLoadMore = { displayedCommunityReviewsLimit += 10 }
        )
    }

    if (showIneligibilityDialog) {
        AlertDialog(
            onDismissRequest = { showIneligibilityDialog = false },
            containerColor = CineSurfaceCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CineWarning,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Điều kiện viết đánh giá",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CineTextWhite,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = ineligibilityReason,
                    fontSize = 14.sp,
                    color = CineTextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showIneligibilityDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                ) {
                    Text("Đã hiểu", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun CommunityReviewsTab(
    movies: List<Movie>,
    movieReviewStats: Map<Long, MovieReviewsData>,
    isLoading: Boolean,
    errorMessage: String?,
    searchQuery: String,
    displayedLimit: Int,
    onSearchQueryChange: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onReviewActionClick: (Movie) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val visibleMovies = remember(movies, displayedLimit) {
        movies.take(displayedLimit)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Tìm phim để xem đánh giá...", color = CineTextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = CineTextGray
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = CineTextGray
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        when {
            isLoading && movies.isEmpty() -> {
                LoadingView(message = "Đang tải danh sách phim & đánh giá...")
            }
            errorMessage != null && movies.isEmpty() -> {
                ErrorView(message = errorMessage, onRetry = onRetry)
            }
            movies.isEmpty() -> {
                EmptyView(message = "Không tìm thấy phim phù hợp.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleMovies, key = { it.movieId }) { movie ->
                        MovieReviewCard(
                            movie = movie,
                            reviewStats = movieReviewStats[movie.movieId],
                            onCardClick = { onMovieClick(movie) },
                            onReviewClick = { onReviewActionClick(movie) }
                        )
                    }

                    if (displayedLimit < movies.size) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    shape = RoundedCornerShape(8.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder))
                                ) {
                                    Text("Xem thêm", color = CineTextWhite, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieReviewCard(
    movie: Movie,
    reviewStats: MovieReviewsData?,
    onCardClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    val avgRating = reviewStats?.averageRating
        ?: (if (movie.rating > 5.0) movie.rating / 2.0 else movie.rating)
    val reviewCount = reviewStats?.reviewCount

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoviePosterBox(
                posterUrl = movie.posterUrl,
                title = movie.title,
                modifier = Modifier
                    .width(76.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = movie.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (movie.duration > 0) {
                    val ageSuffix = if (movie.ageRating.isNotBlank()) " • ${movie.ageRating}" else ""
                    Text(
                        text = "${movie.duration} phút$ageSuffix",
                        fontSize = 12.sp,
                        color = CineTextGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (avgRating > 0.0) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = CineWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (avgRating > 0.0) {
                            String.format(Locale.US, "%.1f/5", avgRating).replace(".0/5", "/5")
                        } else {
                            "Chưa có đánh giá"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )

                    if (reviewCount != null && reviewCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$reviewCount đánh giá",
                            fontSize = 12.sp,
                            color = CineTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCardClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Xem", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MovieReviewsDetailDialog(
    movie: Movie,
    reviewsData: com.example.cineone.domain.model.MovieReviewsData?,
    isLoading: Boolean,
    aiSummary: com.example.cineone.data.dto.ai.ReviewSummaryResponseDto?,
    aiSummaryError: String? = null,
    isAiLoading: Boolean,
    isCheckingEligibility: Boolean,
    displayedLimit: Int,
    onDismiss: () -> Unit,
    onRefreshAiSummary: () -> Unit = {},
    onWriteReviewClick: () -> Unit,
    onLoadMore: () -> Unit
) {
    val reviews = reviewsData?.reviews ?: emptyList()
    val avgScore = reviewsData?.averageRating ?: (if (movie.rating > 5.0) movie.rating / 2.0 else movie.rating)
    val totalCount = reviewsData?.reviewCount ?: reviews.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = CineTextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đánh giá phim",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MoviePosterBox(
                            posterUrl = movie.posterUrl,
                            title = movie.title,
                            modifier = Modifier
                                .width(70.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { star ->
                                    val icon = when {
                                        avgScore >= star -> Icons.Filled.Star
                                        avgScore >= star - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                                        else -> Icons.Outlined.StarOutline
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = CineWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f/5", avgScore).replace(".0/5", "/5"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalCount > 0) "$totalCount lượt đánh giá" else "Chưa có đánh giá nào",
                                fontSize = 12.sp,
                                color = CineTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAiLoading || (aiSummary != null && !aiSummary.summary.isNullOrBlank()) || !aiSummaryError.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CineInputBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorderMuted)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Summary",
                                    tint = CineWarning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAiLoading) "AI đang tóm tắt đánh giá..." else "AI Tóm tắt đánh giá",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineWarning
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isAiLoading) {
                                    CircularProgressIndicator(
                                        color = CineWarning,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = onRefreshAiSummary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Làm mới tóm tắt AI",
                                            tint = CineTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val displayText = if (isAiLoading) {
                                if (!aiSummary?.summary.isNullOrBlank()) {
                                    "Đang cập nhật lại phân tích mới nhất...\n${aiSummary?.summary}"
                                } else {
                                    "Đang đọc và tổng hợp ý kiến từ các đánh giá của người xem..."
                                }
                            } else if (aiSummary != null && !aiSummary.summary.isNullOrBlank()) {
                                aiSummary.summary ?: ""
                            } else {
                                aiSummaryError ?: "Chưa có đủ nhận xét hiển thị để tóm tắt."
                            }
                            Text(
                                text = displayText,
                                fontSize = 13.sp,
                                color = if (isAiLoading && aiSummary?.summary.isNullOrBlank()) {
                                    CineTextMuted
                                } else if (!aiSummaryError.isNullOrBlank() && aiSummary == null) {
                                    CineWarning
                                } else {
                                    CineTextWhite
                                },
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = onWriteReviewClick,
                    enabled = !isCheckingEligibility,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isCheckingEligibility) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Viết đánh giá",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Nhận xét từ khán giả (${reviews.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CineRedPrimary, modifier = Modifier.size(32.dp))
                    }
                } else if (reviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có nhận xét nào cho phim này.\nHãy là người đầu tiên trải nghiệm và chia sẻ cảm nhận!",
                            color = CineTextGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    val visible = reviews.take(displayedLimit)
                    visible.forEach { review ->
                        CommunityReviewItem(review = review)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (displayedLimit < reviews.size) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder))
                            ) {
                                Text("Xem thêm nhận xét", color = CineTextWhite, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityReviewItem(review: Review) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CineInputBackground)
                        .border(1.dp, CineBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.userName.take(1).uppercase(Locale.getDefault()),
                        color = CineRedPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineTextWhite
                    )
                    if (review.createdAt.isNotBlank()) {
                        Text(
                            text = review.createdAt,
                            fontSize = 11.sp,
                            color = CineTextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = CineWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${review.rating}/5",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                }
            }

            if (review.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.content,
                    fontSize = 13.sp,
                    color = CineTextGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun MyReviewsTab(
    reviews: List<Review>,
    isLoading: Boolean,
    errorMessage: String?,
    isLoggedIn: Boolean,
    displayedLimit: Int,
    onLoginClick: () -> Unit,
    onEditReviewClick: (Review) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    if (!isLoggedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CineTextMuted,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Vui lòng đăng nhập để xem các bài đánh giá của bạn.",
                    color = CineTextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary)
                ) {
                    Text("Đăng nhập ngay", color = Color.White)
                }
            }
        }
        return
    }

    when {
        isLoading && reviews.isEmpty() -> {
            LoadingView(message = "Đang tải các đánh giá của bạn...")
        }
        errorMessage != null && reviews.isEmpty() -> {
            ErrorView(message = errorMessage, onRetry = onRetry)
        }
        reviews.isEmpty() -> {
            EmptyView(message = "Bạn chưa viết bài đánh giá nào.")
        }
        else -> {
            val visible = remember(reviews, displayedLimit) {
                reviews.take(displayedLimit)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.reviewId }) { review ->
                    MyReviewCard(
                        review = review,
                        onEditClick = { onEditReviewClick(review) }
                    )
                }

                if (displayedLimit < reviews.size) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = onLoadMore,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder))
                            ) {
                                Text("Xem thêm", color = CineTextWhite, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyReviewCard(
    review: Review,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.movieTitle.ifBlank { "Phim đã đánh giá" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = CineWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${review.rating}/5",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                }
            }

            if (review.createdAt.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• ${review.createdAt}",
                    fontSize = 11.sp,
                    color = CineTextMuted
                )
            }

            if (review.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.content,
                    fontSize = 13.sp,
                    color = CineTextGray,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CineBorder)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CineRedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉnh sửa", color = CineTextWhite, fontSize = 12.sp)
                }
            }
        }
    }
}
