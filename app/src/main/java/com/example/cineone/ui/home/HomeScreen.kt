package com.example.cineone.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.domain.model.Movie
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.cinema.CinemaListScreen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.movie.formatMovieRatingBadge
import com.example.cineone.ui.movie.normalizeAgeBadge
import com.example.cineone.ui.theme.*

private enum class HomeBottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Trang chủ", Icons.Filled.Home, Icons.Outlined.Home),
    CINEMA("Chọn rạp", Icons.Filled.Theaters, Icons.Outlined.Theaters),
    REVIEW("Đánh giá", Icons.Filled.StarOutline, Icons.Outlined.StarOutline),
    NOTIFICATION("Thông báo", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE("Tôi", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedBottomTab by remember { mutableStateOf(HomeBottomTab.HOME) }
    var selectedPromo by remember { mutableStateOf<HomePromotionItem?>(null) }
    val context = LocalContext.current

    Scaffold(
        containerColor = CineBackgroundDark,
        bottomBar = {
            CineOneBottomBar(
                selectedTab = selectedBottomTab,
                onTabSelected = { selectedBottomTab = it }
            )
        }
    ) { innerPadding ->
        when (selectedBottomTab) {
            HomeBottomTab.HOME -> {
                HomeTabContent(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onSearchQueryChange = viewModel::onSearchQueryChanged,
                    onSearchSubmit = {
                        navController.navigate(
                            Screen.MovieSearch.createRoute(query = uiState.searchQuery)
                        )
                    },
                    onOpenSearchScreen = {
                        navController.navigate(
                            Screen.MovieSearch.createRoute(query = uiState.searchQuery)
                        )
                    },
                    onAiAssistantClick = { navController.navigate(Screen.Assistant.route) },
                    onMovieStatusTabSelected = viewModel::onTabSelected,
                    onMovieClick = { movieId ->
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    onSeeAllMoviesClick = {
                        navController.navigate(
                            Screen.MovieList.createRoute(uiState.selectedTab.statusCode)
                        )
                    },
                    onPromotionClick = { promo -> selectedPromo = promo },
                    onRetry = viewModel::loadHomeData
                )
            }

            HomeBottomTab.CINEMA -> {
                CinemaListScreen(
                    navController = navController,
                    isEmbeddedInHome = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            HomeBottomTab.PROFILE -> {
                QuickProfileTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onLogout = {
                        runCatching {
                            CineOneApp.instance.networkModule.sessionManager.clearSession()
                        }
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            else -> {
                PlaceholderBottomTab(
                    tab = selectedBottomTab,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onBackToHome = { selectedBottomTab = HomeBottomTab.HOME }
                )
            }
        }
    }

    if (selectedPromo != null) {
        val promo = selectedPromo!!
        AlertDialog(
            onDismissRequest = { selectedPromo = null },
            containerColor = CineSurfaceCard,
            title = {
                Text(
                    text = promo.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mã voucher",
                                fontSize = 11.sp,
                                color = CineTextGray
                            )
                            Text(
                                text = promo.code,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CineRedPrimary
                            )
                        }
                        TextButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("voucher_code", promo.code))
                            }
                        ) {
                            Text(
                                text = "[ Sao chép ]",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite
                            )
                        }
                    }
                    Text(
                        text = promo.subtitle,
                        fontSize = 13.sp,
                        color = CineTextGray
                    )
                    Text(
                        text = "Thời gian áp dụng: ${promo.validPeriod}",
                        fontSize = 12.sp,
                        color = CineTextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPromo = null }) {
                    Text("Đóng", color = CineRedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

}

@Composable
private fun HomeTabContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenSearchScreen: () -> Unit = {},
    onAiAssistantClick: () -> Unit,
    onMovieStatusTabSelected: (HomeMovieTab) -> Unit,
    onMovieClick: (Long) -> Unit,
    onSeeAllMoviesClick: () -> Unit,
    onPromotionClick: (HomePromotionItem) -> Unit,
    onRetry: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val statusTabs = remember { HomeMovieTab.entries }
    val selectedTabIndex = remember(uiState.selectedTab) {
        statusTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
    }

    val carouselMovies = uiState.tabMovies

    val featuredMovies = remember(uiState.featuredMovies, uiState.tabMovies) {
        if (uiState.featuredMovies.isNotEmpty()) uiState.featuredMovies else uiState.tabMovies
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. THANH TÌM KIẾM & NÚT TRỢ LÝ AI (Giống thiết kế CineOne_giaodien.pdf)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(21.dp))
                    .clickable { onOpenSearchScreen() }
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
                    Text(
                        text = uiState.searchQuery.ifEmpty { "Tìm phim, rạp ..." },
                        color = if (uiState.searchQuery.isEmpty()) CineTextGray else CineTextWhite,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Nút Trợ lý AI
            Row(
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineRedPrimary.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .clickable { onAiAssistantClick() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Trợ lý AI",
                    tint = CineRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Trợ lý AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. TAB TRẠNG THÁI PHIM: Đang chiếu | Đặc biệt | Sắp chiếu
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
            divider = {
                HorizontalDivider(color = CineDivider, thickness = 1.dp)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            statusTabs.forEach { tab ->
                val isSelected = tab == uiState.selectedTab
                Tab(
                    selected = isSelected,
                    onClick = { onMovieStatusTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CineTextWhite else CineTextGray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. CAROUSEL PHIM ĐANG CHỌN (Có số thứ tự 1, 2, 3... & Badge độ tuổi từ DB)
        when {
            uiState.isLoading && carouselMovies.isEmpty() -> {
                LoadingView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp),
                    message = "Đang tải danh sách phim..."
                )
            }

            uiState.errorMessage != null && carouselMovies.isEmpty() -> {
                ErrorView(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }

            carouselMovies.isEmpty() -> {
                EmptyView(
                    message = "Chưa có phim trong mục ${uiState.selectedTab.label}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            else -> {
                HeroMovieCarousel(
                    movies = carouselMovies,
                    onMovieClick = onMovieClick
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. PHIM NỔI BẬT + Xem tất cả >
        SectionHeaderRow(
            title = "Phim nổi bật",
            actionLabel = "Xem tất cả >",
            onActionClick = onSeeAllMoviesClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (featuredMovies.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(featuredMovies, key = { "featured_${it.movieId}" }) { movie ->
                    FeaturedMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.movieId) }
                    )
                }
            }
        } else if (!uiState.isLoading) {
            SectionEmptyBox(message = "Chưa có dữ liệu phim nổi bật")
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 5. KHUYẾN MÃI NỔI BẬT + Xem tất cả > (Đồng bộ 100% với /vouchers trên Web Admin)
        SectionHeaderRow(
            title = "Khuyến mãi nổi bật",
            actionLabel = if (uiState.promotions.isNotEmpty()) "Xem tất cả >" else "",
            onActionClick = {
                uiState.promotions.firstOrNull()?.let(onPromotionClick)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.promotions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.promotions, key = { it.id }) { promo ->
                    PromotionBannerCard(
                        promotion = promo,
                        onClick = { onPromotionClick(promo) }
                    )
                }
            }
        } else {
            SectionEmptyBox(
                message = "Chưa có chương trình khuyến mãi đang áp dụng"
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 6. ĐÁNH GIÁ NỔI BẬT + Xem tất cả > (Đồng bộ 100% với /reviews trên Web Admin)
        SectionHeaderRow(
            title = "Đánh giá nổi bật",
            actionLabel = if (uiState.featuredReviews.isNotEmpty()) "Xem tất cả >" else "",
            onActionClick = {
                val targetId = uiState.featuredReviews.firstOrNull()?.movieId ?: 0L
                if (targetId > 0L) onMovieClick(targetId)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.featuredReviews.isNotEmpty()) {
            FeaturedReviewsCarousel(
                reviews = uiState.featuredReviews,
                onReviewClick = { review ->
                    if (review.movieId > 0L) {
                        onMovieClick(review.movieId)
                    }
                }
            )
        } else {
            SectionEmptyBox(
                message = "Chưa có đánh giá nào được hiển thị"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionEmptyBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CineSurfaceCard)
            .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 18.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = CineTextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeroMovieCarousel(
    movies: List<Movie>,
    onMovieClick: (Long) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { movies.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 64.dp),
        pageSpacing = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val movie = movies[page]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMovieClick(movie.movieId) }
        ) {
            MoviePosterBox(
                posterUrl = movie.posterUrl,
                title = movie.title,
                ageRating = normalizeAgeBadge(movie.ageRating),
                rankBadge = page + 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "* ${formatMovieRatingBadge(movie.rating)}",
                    fontSize = 12.sp,
                    color = CineWarning,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = movie.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = movie.genres.ifBlank { "Đang cập nhật thể loại" },
                fontSize = 12.sp,
                color = CineTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )
        if (actionLabel.isNotBlank()) {
            Text(
                text = actionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun FeaturedMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(126.dp)
            .clickable { onClick() }
    ) {
        MoviePosterBox(
            posterUrl = movie.posterUrl,
            title = movie.title,
            ageRating = normalizeAgeBadge(movie.ageRating),
            modifier = Modifier
                .width(126.dp)
                .height(168.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "* ${formatMovieRatingBadge(movie.rating)}",
            fontSize = 10.sp,
            color = CineWarning,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = movie.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = movie.genres.ifBlank { "Đang cập nhật" },
            fontSize = 11.sp,
            color = CineTextGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PromotionBannerCard(
    promotion: HomePromotionItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(210.dp)
            .height(114.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CineSurfaceCard,
                        Color(0xFF2B1216)
                    )
                )
            )
            .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineRedPrimary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = promotion.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CineTextWhite
                    )
                }
                Text(
                    text = promotion.code,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineWarning
                )
            }

            Column {
                Text(
                    text = promotion.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = promotion.subtitle,
                    fontSize = 11.sp,
                    color = CineTextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = promotion.validPeriod,
                fontSize = 10.sp,
                color = CineTextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeaturedReviewsCarousel(
    reviews: List<HomeFeaturedReviewItem>,
    onReviewClick: (HomeFeaturedReviewItem) -> Unit
) {
    if (reviews.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { reviews.size })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val item = reviews[page]
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReviewClick(item) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Dòng sao: ★★★★☆ 4/5
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..item.maxRating) {
                            Icon(
                                imageVector = if (i <= item.rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = CineWarning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.rating}/${item.maxRating}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Khung trích dẫn nội dung đánh giá
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "\"${item.content}\"",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = CineTextWhite,
                            lineHeight = 19.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Phim: ${item.movieTitle}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${item.reviewerName} • ${item.reviewDate}",
                        fontSize = 12.sp,
                        color = CineTextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chấm tròn chỉ báo trang (Indicator Dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(reviews.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) CineRedPrimary else CineBorderMuted)
                )
            }
        }
    }
}

@Composable
private fun CineOneBottomBar(
    selectedTab: HomeBottomTab,
    onTabSelected: (HomeBottomTab) -> Unit
) {
    NavigationBar(
        containerColor = CineSurfaceCard,
        tonalElevation = 8.dp
    ) {
        HomeBottomTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CineRedPrimary,
                    selectedTextColor = CineRedPrimary,
                    indicatorColor = CineRedPrimary.copy(alpha = 0.15f),
                    unselectedIconColor = CineTextGray,
                    unselectedTextColor = CineTextGray
                )
            )
        }
    }
}

@Composable
private fun QuickProfileTab(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    Box(
        modifier = modifier
            .background(CineBackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tài khoản CineOne",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quản lý hồ sơ, hạng thành viên & vé của bạn",
                    fontSize = 13.sp,
                    color = CineTextGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Đăng xuất",
                        color = CineTextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderBottomTab(
    tab: HomeBottomTab,
    modifier: Modifier = Modifier,
    onBackToHome: () -> Unit
) {
    Box(
        modifier = modifier
            .background(CineBackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = tab.selectedIcon,
                contentDescription = null,
                tint = CineRedPrimary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tab.label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onBackToHome,
                border = BorderStroke(1.dp, CineRedPrimary)
            ) {
                Text("Quay lại Trang chủ", color = CineTextWhite)
            }
        }
    }
}
