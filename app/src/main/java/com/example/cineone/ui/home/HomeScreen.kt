package com.example.cineone.ui.home

import android.Manifest
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.data.repository.CinemaRepositoryImpl
import com.example.cineone.domain.model.Movie
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.auth.AuthViewModel
import com.example.cineone.ui.cinema.CinemaListScreen
import com.example.cineone.ui.common.EmptyView
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.movie.formatMovieRatingBadge
import com.example.cineone.ui.movie.normalizeAgeBadge
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.cineone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

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
    val context = LocalContext.current

    var selectedBottomTabName by rememberSaveable {
        val initial = context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
            .getString("active_home_tab", HomeBottomTab.HOME.name) ?: HomeBottomTab.HOME.name
        mutableStateOf(initial)
    }
    val selectedBottomTab = remember(selectedBottomTabName) {
        try {
            HomeBottomTab.valueOf(selectedBottomTabName)
        } catch (_: Exception) {
            HomeBottomTab.HOME
        }
    }
    var selectedPromo by remember { mutableStateOf<HomePromotionItem?>(null) }

    Scaffold(
        containerColor = CineBackgroundDark,
        bottomBar = {
            CineOneBottomBar(
                selectedTab = selectedBottomTab,
                onTabSelected = { tab ->
                    selectedBottomTabName = tab.name
                    context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                        .edit()
                        .putString("active_home_tab", tab.name)
                        .apply()
                }
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
                    onProfileDetailClick = {
                        navController.navigate(Screen.ProfileDetail.route)
                    },
                    onMembershipClick = {
                        navController.navigate(Screen.Membership.route)
                    },
                    onMyTicketsClick = {
                        navController.navigate(Screen.TicketList.route)
                    },
                    onVouchersClick = {
                        navController.navigate(Screen.VoucherList.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onTermsAndPoliciesClick = {
                        navController.navigate(Screen.TermsAndPolicies.route)
                    },
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
                    onBackToHome = { selectedBottomTabName = HomeBottomTab.HOME.name }
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
    onProfileDetailClick: () -> Unit = {},
    onMembershipClick: () -> Unit = {},
    onMyTicketsClick: () -> Unit = {},
    onVouchersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTermsAndPoliciesClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authUiState by authViewModel.uiState.collectAsState()
    val sessionManager = remember { CineOneApp.instance.networkModule.sessionManager }

    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            authViewModel.getProfile()
        }
    }

    val cachedUser = remember(authUiState.user) {
        authUiState.user ?: sessionManager.fetchCachedUser()
    }
    val userEmail = cachedUser?.email?.takeIf { it.isNotBlank() } ?: "Chưa đăng nhập"
    val userName = remember(cachedUser) {
        val fullName = cachedUser?.fullName?.trim().orEmpty()
        if (fullName.isNotBlank()) {
            fullName
        } else if (userEmail.contains("@")) {
            userEmail.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            "Khách"
        }
    }
    val userTier = cachedUser?.membershipTier?.takeIf { it.isNotBlank() } ?: "NORMAL"
    val avatarUriStr = remember(authUiState.user) { sessionManager.fetchUserAvatarUri() }
    val avatarBitmap = rememberAvatarBitmap(context, avatarUriStr)

    Column(
        modifier = modifier
            .background(CineBackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tiêu đề trang "Tài khoản" (theo wireframe CineOne_giaodien.pdf)
        Text(
            text = "Tài khoản",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Card thông tin người dùng (Avatar + Tên + Email + Hạng + Nút Thông tin chi tiết)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = BorderStroke(1.dp, CineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "Ảnh đại diện",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, CineRedPrimary, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = CineRedPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = userEmail,
                        fontSize = 13.sp,
                        color = CineTextGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
text = userTier,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                // Nút " Thông tin chi tiết\ (SCRUM-105)
 OutlinedButton(
 onClick = onProfileDetailClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CineBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Thông tin\nchi tiết",
                        color = CineTextWhite,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

 // 2. Card Menu 7 mục (theo đúng thứ tự trong wireframe CineOne_giaodien.pdf)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = BorderStroke(1.dp, CineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Hạng thành viên -> Mở MembershipScreen
                ProfileMenuItem(
                    icon = Icons.Default.Star,
                    title = "Hạng thành viên",
                    onClick = onMembershipClick
                )
                HorizontalDivider(color = CineDivider)

                // 2. Vé của tôi (HOẠT ĐỘNG - Mở TicketListScreen)
                ProfileMenuItem(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "Vé của tôi",
                    onClick = onMyTicketsClick
                )
                HorizontalDivider(color = CineDivider)

                // 3. Voucher của tôi -> Mở VoucherScreen
                ProfileMenuItem(
                    icon = Icons.Default.LocalActivity,
                    title = "Voucher của tôi",
                    onClick = onVouchersClick
                )
                HorizontalDivider(color = CineDivider)

                // 4. Đánh giá của tôi
                ProfileMenuItem(
                    icon = Icons.Default.RateReview,
                    title = "Đánh giá của tôi",
                    onClick = { Toast.makeText(context, "Mục Đánh giá (SCRUM-35)", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(color = CineDivider)

                // 5. Cài đặt (HOẠT ĐỘNG - SCRUM-106)
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Cài đặt",
                    onClick = onSettingsClick
                )
                HorizontalDivider(color = CineDivider)

                // 6. Hỗ trợ & khiếu nại
                ProfileMenuItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Hỗ trợ & khiếu nại",
                    onClick = { Toast.makeText(context, "Mục Hỗ trợ & khiếu nại (SCRUM-36)", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(color = CineDivider)

                // 7. Chính sách & điều khoản (HOẠT ĐỘNG - SCRUM-106)
                ProfileMenuItem(
                    icon = Icons.Default.Policy,
                    title = "Chính sách & điều khoản",
                    onClick = onTermsAndPoliciesClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Nút [ ĐĂNG XUẤT ] (HOẠT ĐỘNG - theo đúng wireframe ảnh 5)
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
            border = BorderStroke(1.dp, CineBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "ĐĂNG XUẤT",
                color = CineTextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CineTextWhite,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = CineTextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = CineTextGray,
            modifier = Modifier.size(20.dp)
        )
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

@Composable
private fun rememberAvatarBitmap(context: Context, uriString: String?): ImageBitmap? {
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        if (uriString.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    return bitmap
}

// ============================================================================
// SCRUM-105 (CINE-30.1): MÀN HÌNH THÔNG TIN CÁ NHÂN & THIẾT LẬP VỊ TRÍ NHÀ
// ============================================================================

@Composable
fun ProfileDetailScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val sessionManager = remember { CineOneApp.instance.networkModule.sessionManager }
    val uiState by authViewModel.uiState.collectAsState()

    val cachedUser = remember { sessionManager.fetchCachedUser() }

    var fullName by remember { mutableStateOf(cachedUser?.fullName.orEmpty()) }
    var email by remember { mutableStateOf(cachedUser?.email.orEmpty()) }
    var phone by remember { mutableStateOf(cachedUser?.phone.orEmpty()) }
    var dateOfBirth by remember { mutableStateOf(cachedUser?.dateOfBirth.orEmpty()) }
    var address by remember { mutableStateOf(cachedUser?.address.orEmpty()) }
    var membershipTier by remember { mutableStateOf(cachedUser?.membershipTier ?: "NORMAL") }
    var isEmailVerified by remember { mutableStateOf(cachedUser?.isEmailVerified ?: cachedUser?.isVerified ?: true) }
    var isPhoneVerified by remember { mutableStateOf(cachedUser?.isPhoneVerified ?: false) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingPhone by remember { mutableStateOf(false) }
    var isEditingDob by remember { mutableStateOf(false) }
    var avatarUriStr by remember { mutableStateOf(sessionManager.fetchUserAvatarUri()) }
    val avatarBitmap = rememberAvatarBitmap(context, avatarUriStr)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val str = uri.toString()
            sessionManager.saveUserAvatarUri(str)
            avatarUriStr = str
        }
    }

    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            authViewModel.getProfile()
        }
    }

    // Đồng bộ dữ liệu khi tải hồ sơ từ API thành công
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val updatedHomeAddrFlow = remember(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("updated_home_address", null)
    }
    val updatedHomeAddr by updatedHomeAddrFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(uiState.user) {
        val user = uiState.user ?: return@LaunchedEffect
        if (user.fullName.isNotBlank()) fullName = user.fullName
        if (user.email.isNotBlank()) email = user.email
        if (!user.phone.isNullOrBlank()) phone = user.phone
        if (!user.dateOfBirth.isNullOrBlank()) dateOfBirth = user.dateOfBirth
        if (!user.address.isNullOrBlank() && updatedHomeAddr.isNullOrBlank()) {
            address = user.address
        }
        if (user.membershipTier.isNotBlank()) membershipTier = user.membershipTier
        isEmailVerified = user.isEmailVerified || user.isVerified
        isPhoneVerified = user.isPhoneVerified
    }

    LaunchedEffect(updatedHomeAddr) {
        if (!updatedHomeAddr.isNullOrBlank()) {
            address = updatedHomeAddr!!
            savedStateHandle?.remove<String>("updated_home_address")
        }
    }

    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            Toast.makeText(context, "Đã lưu thông tin cá nhân thành công", Toast.LENGTH_SHORT).show()
            isEditingName = false
            isEditingPhone = false
            isEditingDob = false
            authViewModel.resetUpdateState()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val openDatePicker = {
        val calendar = Calendar.getInstance()
        val parts = dateOfBirth.split("/")
        val initDay = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
        val initMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val initYear = parts.getOrNull(2)?.toIntOrNull() ?: 2000

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dateOfBirth = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            initYear,
            initMonth.coerceIn(0, 11),
            initDay.coerceIn(1, 31)
        ).show()
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }
                Text(
                    text = "Thông tin cá nhân",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar (64dp theo đúng thiết kế CineOne_giaodien.pdf)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "Ảnh đại diện",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, CineRedPrimary, CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Ảnh đại diện",
                        tint = CineTextWhite,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nút "Thêm ảnh đại diện" (/I205)
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CineBorder),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Thêm ảnh đại diện",
                    fontSize = 12.sp,
                    color = CineTextWhite
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tên hiển thị + Badge Hạng thành viên (VIP)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = fullName.ifBlank { "Chưa cập nhật họ tên" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineSuccess.copy(alpha = 0.2f))
                        .border(1.dp, CineSuccess, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = membershipTier,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card Thông tin cá nhân (Họ và tên, Email, Số điện thoại, Ngày sinh, Địa chỉ)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                border = BorderStroke(1.dp, CineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Họ và tên (/I204)
                    ProfileEditableRow(
                        label = "Họ và tên:",
                        value = fullName,
                        placeholder = "Nhập họ và tên",
                        isEditing = isEditingName,
                        onValueChange = { fullName = it },
                        onEditToggle = { isEditingName = !isEditingName }
                    )

                    HorizontalDivider(color = CineDivider)

                    // 2. Email + Trạng thái xác minh
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Email:",
                            fontSize = 12.sp,
                            color = CineTextGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = email.ifBlank { "Chưa cập nhật email" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = CineTextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            if (email.isNotBlank()) {
                                VerificationStatusBadge(
                                    verified = isEmailVerified,
                                    onClick = {
                                        if (!isEmailVerified) {
                                            navController.navigate(
                                                Screen.Otp.createRoute(email, "REGISTER")
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 3. Số điện thoại (/I207) + Badge Xác minh + Icon chỉnh sửa
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Số điện thoại:",
                                    fontSize = 12.sp,
                                    color = CineTextGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isEditingPhone) {
                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = {
                                            phone = it
                                            isPhoneVerified = false
                                        },
                                        placeholder = {
                                            Text("Nhập số điện thoại", fontSize = 13.sp, color = CineTextMuted)
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Done
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CineTextWhite,
                                            unfocusedTextColor = CineTextWhite,
                                            focusedBorderColor = CineRedPrimary,
                                            unfocusedBorderColor = CineBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = phone.ifBlank { "Chưa cập nhật" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (phone.isBlank()) CineTextMuted else CineTextWhite
                                        )
                                        if (phone.isNotBlank()) {
                                            VerificationStatusBadge(
                                                verified = isPhoneVerified,
                                                onClick = {
                                                    if (!isPhoneVerified) {
                                                        isPhoneVerified = true
                                                        authViewModel.updateProfile(
                                                            fullName = fullName.trim(),
                                                            phone = phone.trim(),
                                                            address = address.trim().ifBlank { null },
                                                            dateOfBirth = dateOfBirth.trim().ifBlank { null },
                                                            phoneVerified = true
                                                        )
                                                        Toast.makeText(
                                                            context,
                                                            "Đã xác minh số điện thoại thành công",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(
                                onClick = { isEditingPhone = !isEditingPhone },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditingPhone) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa số điện thoại",
                                    tint = if (isEditingPhone) CineSuccess else CineTextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 4. Ngày sinh + Icon chỉnh sửa (Mở DatePicker hoặc sửa trực tiếp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ngày sinh:",
                                    fontSize = 12.sp,
                                    color = CineTextGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isEditingDob) {
                                    OutlinedTextField(
                                        value = dateOfBirth,
                                        onValueChange = { dateOfBirth = it },
                                        placeholder = {
                                            Text("dd/MM/yyyy", fontSize = 13.sp, color = CineTextMuted)
                                        },
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = openDatePicker) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Chọn lịch",
                                                    tint = CineRedPrimary
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CineTextWhite,
                                            unfocusedTextColor = CineTextWhite,
                                            focusedBorderColor = CineRedPrimary,
                                            unfocusedBorderColor = CineBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = dateOfBirth.ifBlank { "Chưa cập nhật" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (dateOfBirth.isBlank()) CineTextMuted else CineTextWhite,
                                        modifier = Modifier.clickable { openDatePicker() }
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (!isEditingDob) {
                                        openDatePicker()
                                    }
                                    isEditingDob = !isEditingDob
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditingDob) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = "Chỉnh sửa ngày sinh",
                                    tint = if (isEditingDob) CineSuccess else CineTextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CineDivider)

                    // 5. Địa chỉ nhà (/I208, /I209) -> Nhấn icon bút chì mở màn Thiết lập vị trí nhà
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.HomeLocationSetup.route) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Địa chỉ:",
                                fontSize = 12.sp,
                                color = CineTextGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = address.ifBlank { "Chưa thiết lập vị trí nhà" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (address.isBlank()) CineTextMuted else CineTextWhite
                            )
                        }
                        IconButton(
                            onClick = { navController.navigate(Screen.HomeLocationSetup.route) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Thiết lập vị trí nhà",
                                tint = CineTextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút LƯU (theo đúng wireframe CineOne_giaodien.pdf)
            Button(
                onClick = {
                    val cleanName = fullName.trim()
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    authViewModel.updateProfile(
                        fullName = cleanName,
                        phone = phone.trim().ifBlank { null },
                        address = address.trim().ifBlank { null },
                        dateOfBirth = dateOfBirth.trim().ifBlank { null },
                        phoneVerified = isPhoneVerified
                    )
                },
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = CineTextWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "LƯU",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileEditableRow(
    label: String,
    value: String,
    placeholder: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onEditToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = CineTextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, fontSize = 13.sp, color = CineTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CineTextWhite,
                        unfocusedTextColor = CineTextWhite,
                        focusedBorderColor = CineRedPrimary,
                        unfocusedBorderColor = CineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = value.ifBlank { "Chưa cập nhật" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (value.isBlank()) CineTextMuted else CineTextWhite
                )
            }
        }
        IconButton(
            onClick = onEditToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = "Chỉnh sửa",
                tint = if (isEditing) CineSuccess else CineTextGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VerificationStatusBadge(
    verified: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (verified) CineSuccess.copy(alpha = 0.2f) else CineWarning.copy(alpha = 0.2f)
    val borderColor = if (verified) CineSuccess else CineWarning
    val textColor = if (verified) CineSuccess else CineWarning
    val text = if (verified) "Đã xác minh ✓" else "Xác minh"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = !verified) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ============================================================================
// SCRUM-105 (CINE-30.1): MÀN HÌNH ĐỊA CHỈ / THIẾT LẬP VỊ TRÍ NHÀ (/I267 - /I275)
// ============================================================================

private val VIETNAM_PROVINCES_ALL: List<String> = listOf(
    "Thành phố Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
    "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh",
    "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau",
    "Cao Bằng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai", "Đồng Tháp",
    "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh", "Hải Dương", "Hậu Giang",
    "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum", "Lai Châu",
    "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định", "Nghệ An",
    "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình", "Quảng Nam",
    "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng", "Sơn La", "Tây Ninh",
    "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang",
    "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
)

private object VietnamAdminData {
    private var cachedData: Map<String, Map<String, List<String>>>? = null

    fun getUnits(context: Context): Map<String, Map<String, List<String>>> {
        cachedData?.let { return it }
        return try {
            val jsonString = context.assets.open("vietnam_admin_units.json").bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(jsonString)
            val result = linkedMapOf<String, Map<String, List<String>>>()
            val provinceKeys = json.keys()
            while (provinceKeys.hasNext()) {
                val pKey = provinceKeys.next()
                val distObj = json.getJSONObject(pKey)
                val distMap = linkedMapOf<String, List<String>>()
                val distKeys = distObj.keys()
                while (distKeys.hasNext()) {
                    val dKey = distKeys.next()
                    val wardArray = distObj.getJSONArray(dKey)
                    val wardList = ArrayList<String>(wardArray.length())
                    for (i in 0 until wardArray.length()) {
                        wardList.add(wardArray.getString(i))
                    }
                    distMap[dKey] = wardList
                }
                result[pKey] = distMap
            }
            cachedData = result
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}


@Composable
fun HomeLocationSetupScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { CineOneApp.instance.networkModule.sessionManager }
    val uiState by authViewModel.uiState.collectAsState()

    val existingUser = remember {
        uiState.user ?: sessionManager.fetchCachedUser()
    }
    val initialAddress = remember { existingUser?.address?.trim().orEmpty() }
    val initialSavedPin = remember(initialAddress) {
        sessionManager.fetchUserHomeCoordinates(
            forAddress = initialAddress.takeIf { it.isNotBlank() }
        )
    }

    // Phân tách địa chỉ hiện có (nếu người dùng đã lưu trước đó)
    val initialParts = remember(initialAddress) {
        if (initialAddress.isBlank()) emptyList()
        else initialAddress.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    var selectedProvince by remember {
        mutableStateOf(
            when {
                initialParts.size >= 4 -> initialParts.last()
                initialParts.size == 3 -> initialParts[2]
                initialParts.size == 2 -> initialParts[1]
                initialParts.size == 1 -> initialParts[0]
                else -> ""
            }
        )
    }
    var selectedDistrict by remember {
        mutableStateOf(
            when {
                initialParts.size >= 4 -> initialParts[initialParts.size - 2]
                initialParts.size == 3 -> initialParts[1]
                initialParts.size == 2 -> initialParts[0]
                else -> ""
            }
        )
    }
    var selectedWard by remember {
        mutableStateOf(
            when {
                initialParts.size >= 4 -> initialParts[initialParts.size - 3]
                initialParts.size == 3 -> initialParts[0]
                else -> ""
            }
        )
    }
    var streetDetail by remember {
        mutableStateOf(
            when {
                initialParts.size >= 4 -> initialParts.dropLast(3).joinToString(", ")
                else -> ""
            }
        )
    }

    val cachedCinemas = remember { CinemaRepositoryImpl.getCachedCinemasSnapshot() }
    val fullAdminUnits = remember(context) { VietnamAdminData.getUnits(context) }
    val provinceOptions = remember(cachedCinemas) {
        val fromCinemas = cachedCinemas.map { it.city.trim() }.filter { it.isNotBlank() }
        (VIETNAM_PROVINCES_ALL + fullAdminUnits.keys + fromCinemas).distinct()
    }
    val districtMap = remember(selectedProvince) {
        val matchedKey = fullAdminUnits.keys.firstOrNull { key ->
            LocationUtils.normalizeCityKey(key) == LocationUtils.normalizeCityKey(selectedProvince)
        }
        if (matchedKey != null) fullAdminUnits[matchedKey].orEmpty() else emptyMap()
    }
    val districtOptions = remember(districtMap) { districtMap.keys.toList() }
    val wardOptions = remember(districtMap, selectedDistrict) {
        val matchedDist = districtMap.keys.firstOrNull { d ->
            LocationUtils.normalizeSearchText(d) == LocationUtils.normalizeSearchText(selectedDistrict) ||
                LocationUtils.normalizeSearchText(d).endsWith(LocationUtils.normalizeSearchText(selectedDistrict))
        }
        if (matchedDist != null) districtMap[matchedDist].orEmpty() else emptyList()
    }

    val assembledAddress = remember(streetDetail, selectedWard, selectedDistrict, selectedProvince) {
        listOf(
            streetDetail.trim(),
            selectedWard.trim(),
            selectedDistrict.trim(),
            selectedProvince.trim()
        ).filter { it.isNotBlank() }.joinToString(", ")
    }

    // Tọa độ mặc định ban đầu (nếu đã từng ghim thì lấy tọa độ ghim, nếu chưa thì mặc định trung tâm TP.HCM)
    var pinnedCoords by remember {
        mutableStateOf(initialSavedPin ?: Pair(10.7769, 106.7009))
    }
    var addressGeocodedCoords by remember {
        mutableStateOf<Pair<Double, Double>?>(initialSavedPin)
    }
    var isManualPin by remember {
        mutableStateOf(initialSavedPin != null)
    }
    var isGeocodingAddress by remember { mutableStateOf(false) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var showFullScreenMapPicker by remember { mutableStateOf(false) }

    // Mức thu phóng bản đồ thông minh dựa trên cấp độ địa chỉ đã nhập
    val mapTargetZoom = remember(selectedProvince, selectedDistrict, selectedWard, streetDetail) {
        when {
            streetDetail.isNotBlank() -> 17
            selectedWard.isNotBlank() -> 15
            selectedDistrict.isNotBlank() -> 14
            selectedProvince.isNotBlank() -> 12
            else -> 13
        }
    }

    // Tự động di chuyển bản đồ & ghim tới nơi vừa chọn/nhập địa chỉ
    LaunchedEffect(selectedProvince, selectedDistrict, selectedWard, streetDetail) {
        if (selectedProvince.isBlank() && selectedDistrict.isBlank() && selectedWard.isBlank() && streetDetail.isBlank()) {
            return@LaunchedEffect
        }
        // Nếu người dùng đang gõ địa chỉ cụ thể thì chờ 450ms để tránh nhảy bản đồ liên tục khi đang gõ
        if (streetDetail.isNotBlank()) {
            delay(450)
        }
        isGeocodingAddress = true
        val found = withContext(Dispatchers.IO) {
            LocationUtils.geocodeAddressForMap(
                context = context,
                streetDetail = streetDetail,
                ward = selectedWard,
                district = selectedDistrict,
                province = selectedProvince,
                cinemas = cachedCinemas
            )
        }
        isGeocodingAddress = false
        if (found != null) {
            addressGeocodedCoords = found
            if (!isManualPin) {
                pinnedCoords = found
            }
        }
    }

    val reverseFillAddressFromCoords: (Double, Double, Boolean) -> Unit = { lat, lon, overwriteAll ->
        if (Geocoder.isPresent()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        val geocoder = Geocoder(context, Locale.forLanguageTag("vi-VN"))
                        @Suppress("DEPRECATION")
                        val list = geocoder.getFromLocation(lat, lon, 1)
                        val addr = list?.firstOrNull()
                        if (addr != null) {
                            withContext(Dispatchers.Main) {
                                addr.adminArea?.takeIf { it.isNotBlank() }?.let {
                                    if (overwriteAll || selectedProvince.isBlank()) selectedProvince = it
                                }
                                (addr.subAdminArea ?: addr.locality)?.takeIf { it.isNotBlank() }?.let {
                                    if (overwriteAll || selectedDistrict.isBlank()) selectedDistrict = it
                                }
                                addr.subLocality?.takeIf { it.isNotBlank() }?.let {
                                    if (overwriteAll || selectedWard.isBlank()) selectedWard = it
                                }
                                val thoroughfare = listOfNotNull(addr.subThoroughfare, addr.thoroughfare)
                                    .joinToString(" ")
                                    .trim()
                                if (thoroughfare.isNotBlank() && (overwriteAll || streetDetail.isBlank())) {
                                    streetDetail = thoroughfare
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val applyDeviceGpsLocation: () -> Unit = {
        scope.launch {
            isLocatingGps = true
            val coords = withContext(Dispatchers.IO) {
                LocationUtils.fetchDeviceLocation(context)
            }
            if (coords != null) {
                isManualPin = true
                pinnedCoords = coords
                addressGeocodedCoords = coords
                reverseFillAddressFromCoords(coords.first, coords.second, true)
                Toast.makeText(context, "Đã đưa ghim về vị trí GPS hiện tại", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Không lấy được tín hiệu GPS hiện tại, bạn có thể kéo ghim trực tiếp trên bản đồ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            isLocatingGps = false
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            applyDeviceGpsLocation()
        } else {
            Toast.makeText(context, "Cần quyền vị trí để lấy tọa độ GPS hiện tại", Toast.LENGTH_SHORT).show()
        }
    }

    if (showFullScreenMapPicker) {
        FullScreenMapPinDialog(
            initialCoords = pinnedCoords,
            initialZoom = mapTargetZoom.coerceAtLeast(16),
            addressPreview = assembledAddress.ifBlank { "Kéo ghim đỏ hoặc chạm vào bản đồ để chọn đúng vị trí nhà" },
            onDismiss = { showFullScreenMapPicker = false },
            onConfirmPin = { newLat, newLon ->
                isManualPin = true
                pinnedCoords = Pair(newLat, newLon)
                if (selectedProvince.isBlank() && selectedDistrict.isBlank()) {
                    reverseFillAddressFromCoords(newLat, newLon, false)
                }
                showFullScreenMapPicker = false
                Toast.makeText(context, "Đã cập nhật vị trí ghim trên bản đồ", Toast.LENGTH_SHORT).show()
            },
            onRequestGps = {
                gpsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(CineBackgroundDark)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = CineTextWhite
                        )
                    }
                    Text(
                        text = "Địa chỉ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                HorizontalDivider(color = CineDivider)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
                border = BorderStroke(1.dp, CineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Tỉnh / Thành phố (/I268, /I269)
                    AddressDropdownField(
                        label = "Tỉnh / Thành phố",
                        selectedValue = selectedProvince,
                        placeholder = "Chọn Tỉnh / Thành phố",
                        options = provinceOptions,
                        onValueSelected = { newProv ->
                            selectedProvince = newProv
                            selectedDistrict = ""
                            selectedWard = ""
                            isManualPin = false
                        }
                    )

                    // 2. Quận / Huyện (/I270, /I271) & Phường / Xã (/I272) nằm trên cùng 1 hàng 2 cột
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AddressDropdownField(
                                label = "Quận / Huyện",
                                selectedValue = selectedDistrict,
                                placeholder = "Chọn Quận/Huyện",
                                options = districtOptions,
                                onValueSelected = { newDist ->
                                    selectedDistrict = newDist
                                    selectedWard = ""
                                    isManualPin = false
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AddressDropdownField(
                                label = "Phường / Xã",
                                selectedValue = selectedWard,
                                placeholder = "Chọn Phường/Xã",
                                options = wardOptions,
                                onValueSelected = { newWard ->
                                    selectedWard = newWard
                                    isManualPin = false
                                }
                            )
                        }
                    }

                    // 3. Địa chỉ cụ thể (/I273, /I274)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Địa chỉ cụ thể",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CineTextWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = streetDetail,
                            onValueChange = {
                                streetDetail = it
                                isManualPin = false
                            },
                            placeholder = {
                                Text(
                                    text = "Số nhà, tên đường...",
                                    fontSize = 13.sp,
                                    color = CineTextMuted
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CineTextWhite,
                                unfocusedTextColor = CineTextWhite,
                                focusedContainerColor = CineInputBackground,
                                unfocusedContainerColor = CineInputBackground,
                                focusedBorderColor = CineRedPrimary,
                                unfocusedBorderColor = CineBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Vị trí trên bản đồ (/I275) - Bản đồ tương tác tự động nhảy tới địa chỉ đã nhập & cho phép kéo ghim đỏ
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Vị trí trên bản đồ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextWhite
                                )
                                if (isGeocodingAddress) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        color = CineRedPrimary,
                                        strokeWidth = 1.8.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isManualPin && addressGeocodedCoords != null) {
                                    Text(
                                        text = "Theo địa chỉ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CineWarning,
                                        modifier = Modifier.clickable {
                                            isManualPin = false
                                            addressGeocodedCoords?.let { pinnedCoords = it }
                                        }
                                    )
                                }
                                Text(
                                    text = if (isLocatingGps) "Đang định vị..." else "Định vị GPS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineRedPrimary,
                                    modifier = Modifier.clickable {
                                        gpsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CineInputBackground)
                                .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                        ) {
                            InteractivePinMapView(
                                latitude = pinnedCoords.first,
                                longitude = pinnedCoords.second,
                                zoom = mapTargetZoom,
                                onPinMoved = { newLat, newLon ->
                                    isManualPin = true
                                    pinnedCoords = Pair(newLat, newLon)
                                    if (selectedProvince.isBlank() && selectedDistrict.isBlank()) {
                                        reverseFillAddressFromCoords(newLat, newLon, false)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Nút phóng to bản đồ ở góc trên bên phải
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CineBackgroundDark.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, CineBorder),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clickable { showFullScreenMapPicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Phóng to bản đồ",
                                        tint = CineTextWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Phóng to chỉnh ghim",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CineTextWhite
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5. Nút Lưu (theo đúng wireframe CineOne_giaodien.pdf)
                    Button(
                        onClick = {
                            if (assembledAddress.isBlank()) {
                                Toast.makeText(context, "Vui lòng chọn hoặc nhập địa chỉ nhà", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Lưu tọa độ ghim chính xác kèm địa chỉ nhà
                            sessionManager.saveUserHomeCoordinates(
                                lat = pinnedCoords.first,
                                lon = pinnedCoords.second,
                                address = assembledAddress
                            )

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("updated_home_address", assembledAddress)

                            val currentUser = sessionManager.fetchCachedUser()
                            if (currentUser != null) {
                                sessionManager.saveUserProfile(currentUser.copy(address = assembledAddress))
                            }
                            if (sessionManager.isLoggedIn() && currentUser != null) {
                                authViewModel.updateProfile(
                                    fullName = currentUser.fullName.ifBlank { "Người dùng CineOne" },
                                    phone = currentUser.phone,
                                    address = assembledAddress,
                                    dateOfBirth = currentUser.dateOfBirth
                                )
                            }
                            Toast.makeText(context, "Đã lưu địa chỉ và vị trí ghim trên bản đồ", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Lưu",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                    }
                }
            }
        }
    }
}

@androidx.annotation.Keep
class MapPinJsBridge(
    private val mainHandler: Handler,
    var onPinChanged: (Double, Double) -> Unit
) {
    var lastJsLat: Double = Double.NaN
    var lastJsLon: Double = Double.NaN

    @JavascriptInterface
    fun onPinMoved(lat: Double, lng: Double) {
        if (lat.isNaN() || lng.isNaN()) return
        lastJsLat = lat
        lastJsLon = lng
        mainHandler.post {
            onPinChanged(lat, lng)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "JavascriptInterface")
@Composable
private fun InteractivePinMapView(
    latitude: Double,
    longitude: Double,
    zoom: Int,
    onPinMoved: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember { MapPinJsBridge(mainHandler, onPinMoved) }
    SideEffect {
        bridge.onPinChanged = onPinMoved
    }

    val mapHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #141821;
                    overflow: hidden;
                    font-family: sans-serif;
                    user-select: none;
                    -webkit-user-select: none;
                }
                #map {
                    width: 100%;
                    height: 100%;
                    background: #141821;
                }
                .cine-pin-wrapper {
                    position: relative;
                    width: 38px;
                    height: 46px;
                }
                .cine-pin-pulse {
                    position: absolute;
                    bottom: 2px;
                    left: 9px;
                    width: 20px;
                    height: 10px;
                    background: rgba(229, 9, 20, 0.38);
                    border-radius: 50%;
                    transform: scale(1);
                    animation: pulse 1.8s infinite;
                }
                @keyframes pulse {
                    0% { transform: scale(0.8); opacity: 0.9; }
                    70% { transform: scale(1.7); opacity: 0.15; }
                    100% { transform: scale(0.8); opacity: 0.9; }
                }
                .leaflet-control-attribution {
                    display: none !important;
                }
                .leaflet-control-zoom {
                    border: 1px solid #2B2F3A !important;
                    border-radius: 8px !important;
                    overflow: hidden;
                }
                .leaflet-control-zoom a {
                    background-color: rgba(18, 20, 28, 0.9) !important;
                    color: #FFFFFF !important;
                    border-bottom: 1px solid #2B2F3A !important;
                    width: 28px !important;
                    height: 28px !important;
                    line-height: 28px !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <script>
                var currentLat = $latitude;
                var currentLng = $longitude;
                var currentZoom = $zoom;
                var map = null;
                var marker = null;
                var isReady = false;

                function notifyAndroid(lat, lng) {
                    currentLat = lat;
                    currentLng = lng;
                    if (window.AndroidMapBridge && window.AndroidMapBridge.onPinMoved) {
                        window.AndroidMapBridge.onPinMoved(lat, lng);
                    }
                }

                function initLeafletMap() {
                    if (typeof L === 'undefined') {
                        initFallbackInteractiveCanvas();
                        return;
                    }
                    map = L.map('map', {
                        zoomControl: true,
                        attributionControl: false
                    }).setView([currentLat, currentLng], currentZoom);

                    var baseLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                        maxZoom: 19,
                        attribution: ''
                    }).addTo(map);

                    baseLayer.on('tileerror', function() {
                        if (!window._switchedBackup) {
                            window._switchedBackup = true;
                            map.removeLayer(baseLayer);
                            L.tileLayer('https://tile.openstreetmap.de/{z}/{x}/{y}.png', {
                                maxZoom: 19
                            }).addTo(map);
                        }
                    });

                    var pinSvg = '<div class="cine-pin-wrapper">' +
                        '<div class="cine-pin-pulse"></div>' +
                        '<svg viewBox="0 0 38 46" width="38" height="46" xmlns="http://www.w3.org/2000/svg">' +
                        '<path d="M19 2C10.16 2 3 9.16 3 18c0 11.75 16 26 16 26s16-14.25 16-26c0-8.84-7.16-16-16-16z" fill="#E50914" stroke="#FFFFFF" stroke-width="2.2"/>' +
                        '<circle cx="19" cy="18" r="6" fill="#FFFFFF"/>' +
                        '</svg>' +
                        '</div>';

                    var pinIcon = L.divIcon({
                        className: '',
                        html: pinSvg,
                        iconSize: [38, 46],
                        iconAnchor: [19, 44]
                    });

                    marker = L.marker([currentLat, currentLng], {
                        draggable: true,
                        icon: pinIcon
                    }).addTo(map);

                    marker.on('dragend', function(e) {
                        var pos = marker.getLatLng();
                        map.panTo(pos, { animate: true });
                        notifyAndroid(pos.lat, pos.lng);
                    });

                    map.on('click', function(e) {
                        marker.setLatLng(e.latlng);
                        map.panTo(e.latlng, { animate: true });
                        notifyAndroid(e.latlng.lat, e.latlng.lng);
                    });

                    isReady = true;
                }

                // Fallback khi thiết bị ngoại tuyến không tải được thư viện bản đồ
                function initFallbackInteractiveCanvas() {
                    var container = document.getElementById('map');
                    container.innerHTML = '<canvas id="fallbackCanvas" style="width:100%;height:100%;display:block;"></canvas>';
                    var canvas = document.getElementById('fallbackCanvas');
                    var ctx = canvas.getContext('2d');
                    function resize() {
                        canvas.width = container.clientWidth || 320;
                        canvas.height = container.clientHeight || 200;
                        draw();
                    }
                    function draw() {
                        var w = canvas.width, h = canvas.height;
                        ctx.fillStyle = '#141821';
                        ctx.fillRect(0, 0, w, h);
                        ctx.strokeStyle = '#232A38';
                        ctx.lineWidth = 1;
                        for (var x = 0; x < w; x += 28) {
                            ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke();
                        }
                        for (var y = 0; y < h; y += 28) {
                            ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
                        }
                        ctx.fillStyle = '#E50914';
                        ctx.beginPath();
                        ctx.arc(w / 2, h / 2 - 10, 10, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.strokeStyle = '#FFFFFF';
                        ctx.lineWidth = 2;
                        ctx.stroke();
                    }
                    canvas.addEventListener('pointerdown', function(ev) {
                        var rect = canvas.getBoundingClientRect();
                        var dx = (ev.clientX - rect.left - rect.width / 2) * 0.00005;
                        var dy = (rect.height / 2 - (ev.clientY - rect.top)) * 0.00005;
                        notifyAndroid(currentLat + dy, currentLng + dx);
                        draw();
                    });
                    window.addEventListener('resize', resize);
                    resize();
                    isReady = true;
                }

                window.updatePinFromAndroid = function(lat, lng, zoomLevel) {
                    currentLat = lat;
                    currentLng = lng;
                    if ( zoomLevel ) currentZoom = zoomLevel;
                    if (isReady && map && marker) {
                        var target = L.latLng(lat, lng);
                        marker.setLatLng(target);
                        map.flyTo(target, currentZoom, { animate: true, duration: 0.65 });
                    }
                };

                window.addEventListener('load', initLeafletMap);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("CineOneMapConsole", consoleMessage?.message().orEmpty())
                        return true
                    }
                }
                webViewClient = WebViewClient()
                addJavascriptInterface(bridge, "AndroidMapBridge")

                // Ngăn ScrollView cha chiếm quyền vuốt khi người dùng đang kéo bản đồ hoặc kéo ghim
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                bridge.lastJsLat = latitude
                bridge.lastJsLon = longitude
                loadDataWithBaseURL(
                    "https://openstreetmap.org",
                    mapHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            val latDiff = kotlin.math.abs(bridge.lastJsLat - latitude)
            val lonDiff = kotlin.math.abs(bridge.lastJsLon - longitude)
            if (bridge.lastJsLat.isNaN() || bridge.lastJsLon.isNaN() || latDiff > 0.00002 || lonDiff > 0.00002) {
                bridge.lastJsLat = latitude
                bridge.lastJsLon = longitude
                webView.evaluateJavascript(
                    "if (window.updatePinFromAndroid) { window.updatePinFromAndroid($latitude, $longitude, $zoom); }",
                    null
                )
            }
        }
    )
}

@Composable
private fun FullScreenMapPinDialog(
    initialCoords: Pair<Double, Double>,
    initialZoom: Int,
    addressPreview: String,
    onDismiss: () -> Unit,
    onConfirmPin: (Double, Double) -> Unit,
    onRequestGps: () -> Unit
) {
    var tempCoords by remember(initialCoords) { mutableStateOf(initialCoords) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CineBackgroundDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Thanh tiêu đề
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CineSurfaceCard)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Đóng",
                            tint = CineTextWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chỉnh ghim vị trí nhà trên bản đồ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                        Text(
                            text = "Kéo ghim đỏ hoặc chạm vào đúng vị trí nhà của bạn",
                            fontSize = 11.sp,
                            color = CineTextGray
                        )
                    }
                    TextButton(onClick = onRequestGps) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineRedPrimary
                        )
                    }
                }

                HorizontalDivider(color = CineDivider)

                // Bản đồ toàn màn hình
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    InteractivePinMapView(
                        latitude = tempCoords.first,
                        longitude = tempCoords.second,
                        zoom = initialZoom,
                        onPinMoved = { newLat, newLon ->
                            tempCoords = Pair(newLat, newLon)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Khung xác nhận tọa độ ghim ở đáy màn hình
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CineSurfaceCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = CineRedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = addressPreview,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineTextWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Tọa độ ghim: %.5f, %.5f",
                                    tempCoords.first,
                                    tempCoords.second
                                ),
                                fontSize = 11.sp,
                                color = CineSuccess
                            )
                        }
                    }

                    Button(
                        onClick = { onConfirmPin(tempCoords.first, tempCoords.second) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Xác nhận vị trí ghim",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressDropdownField(
    label: String,
    selectedValue: String,
    placeholder: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Lọc danh sách thời gian thực theo từ khóa tìm kiếm (hỗ trợ tiếng Việt có dấu và không dấu)
    val filteredOptions = remember(options, searchQuery) {
        if (searchQuery.isBlank()) {
            options
        } else {
            val q = LocationUtils.normalizeSearchText(searchQuery)
            options.filter { opt ->
                LocationUtils.normalizeSearchText(opt).contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CineTextWhite
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        searchQuery = ""
                        showSearchDialog = true
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedValue.ifBlank { placeholder },
                    fontSize = 12.sp,
                    color = if (selectedValue.isBlank()) CineTextMuted else CineTextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = CineTextGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Modal tìm kiếm với thanh tìm kiếm gọn gàng 1 dòng chữ chuẩn
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            containerColor = CineSurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chọn $label",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                    IconButton(
                        onClick = { showSearchDialog = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = CineTextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Thanh tìm kiếm siêu gọn (chiều cao cố định 40dp, 1 dòng chữ, không bị chiếm diện tích)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CineTextGray,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = CineTextWhite,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(CineRedPrimary),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Gõ tên để tìm nhanh $label...",
                                        fontSize = 13.sp,
                                        color = CineTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Xóa tìm kiếm",
                                tint = CineTextGray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }

                    // Danh sách kết quả cuộn mượt mà
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                    ) {
                        if (filteredOptions.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Không tìm thấy kết quả phù hợp" else "Chưa có danh sách tùy chọn",
                                    fontSize = 12.sp,
                                    color = CineTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onValueSelected(searchQuery.trim())
                                            showSearchDialog = false
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Dùng: \"${searchQuery.trim()}\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CineTextWhite
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                filteredOptions.forEach { option ->
                                    val isSelected = option.equals(selectedValue, ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onValueSelected(option)
                                                showSearchDialog = false
                                            }
                                            .background(if (isSelected) CineRedPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) CineRedPrimary else CineTextWhite,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CineRedPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = CineDivider.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Đóng", color = CineTextGray)
                }
            }
        )
    }
}

// ============================================================================
// SCRUM-106 (CINE-30.2): MÀN HÌNH CÀI ĐẶT ỨNG DỤNG (/I276 - /I287)
// ============================================================================

@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsState()
    val sessionManager = remember { CineOneApp.instance.networkModule.sessionManager }
    val settingsPrefs = remember {
        context.getSharedPreferences(AppThemePreferences.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var pushNotificationEnabled by remember {
        mutableStateOf(settingsPrefs.getBoolean("notif_push", true))
    }
    var promoNotificationEnabled by remember {
        mutableStateOf(settingsPrefs.getBoolean("notif_promo", true))
    }
    var reminderNotificationEnabled by remember {
        mutableStateOf(settingsPrefs.getBoolean("notif_reminder", false))
    }

    val currentThemeMode by AppThemePreferences.themeMode.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val appVersion = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName?.let { v -> if (v == "1.0") "1.0.0" else v } ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }
                Text(
                    text = "Cài đặt",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. BẢO MẬT (/I277, /I278)
            SettingsSectionHeader(
                icon = Icons.Default.Lock,
                title = "BẢO MẬT"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showChangePasswordDialog = true }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đổi mật khẩu",
                    fontSize = 15.sp,
                    color = CineTextWhite,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CineTextGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // 2. THÔNG BÁO (/I279, /I280, /I281)
            SettingsSectionHeader(
                icon = Icons.Default.Notifications,
                title = "THÔNG BÁO"
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsNotificationToggleRow(
                label = "Thông báo đẩy",
                enabled = pushNotificationEnabled,
                onToggle = { newValue ->
                    pushNotificationEnabled = newValue
                    settingsPrefs.edit().putBoolean("notif_push", newValue).apply()
                }
            )
            SettingsNotificationToggleRow(
                label = "Khuyến mãi",
                enabled = promoNotificationEnabled,
                onToggle = { newValue ->
                    promoNotificationEnabled = newValue
                    settingsPrefs.edit().putBoolean("notif_promo", newValue).apply()
                }
            )
            SettingsNotificationToggleRow(
                label = "Nhắc lịch chiếu",
                enabled = reminderNotificationEnabled,
                onToggle = { newValue ->
                    reminderNotificationEnabled = newValue
                    settingsPrefs.edit().putBoolean("notif_reminder", newValue).apply()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // 3. GIAO DIỆN (/I282, Chế độ: [ Hệ thống ] ☀️ Sáng 🌙 Tối)
            SettingsSectionHeader(
                icon = Icons.Default.Palette,
                title = "GIAO DIỆN"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CineTextGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chế độ",
                    fontSize = 14.sp,
                    color = CineTextWhite,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeModeChip(
                        label = "[ Hệ thống ]",
                        icon = null,
                        selected = currentThemeMode == AppThemePreferences.MODE_SYSTEM,
                        onClick = {
                            AppThemePreferences.setThemeMode(context, AppThemePreferences.MODE_SYSTEM)
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ThemeModeChip(
                        label = "Sáng",
                        icon = Icons.Default.WbSunny,
                        selected = currentThemeMode == AppThemePreferences.MODE_LIGHT,
                        onClick = {
                            AppThemePreferences.setThemeMode(context, AppThemePreferences.MODE_LIGHT)
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ThemeModeChip(
                        label = "Tối",
                        icon = Icons.Default.DarkMode,
                        selected = currentThemeMode == AppThemePreferences.MODE_DARK,
                        onClick = {
                            AppThemePreferences.setThemeMode(context, AppThemePreferences.MODE_DARK)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // 4. THÔNG TIN ỨNG DỤNG (/I285, /I286)
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = "THÔNG TIN ỨNG DỤNG"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Phiên bản ứng dụng",
                    fontSize = 14.sp,
                    color = CineTextWhite
                )
                Text(
                    text = appVersion,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextGray
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { navController.navigate(Screen.TermsAndPolicies.route) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Chính sách & Điều khoản",
                    fontSize = 14.sp,
                    color = CineTextWhite
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CineTextGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. Nút Đăng xuất (/I287)
            OutlinedButton(
                onClick = {
                    runCatching { sessionManager.clearSession() }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CineBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = "Đăng xuất",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showChangePasswordDialog) {
        val currentEmail = sessionManager.fetchUserEmail()
            ?: sessionManager.fetchCachedUser()?.email?.takeIf { it.isNotBlank() }
            ?: authUiState.user?.email.orEmpty()

        ChangePasswordStandardDialog(
            email = currentEmail,
            authViewModel = authViewModel,
            onNavigateToForgotPassword = {
                showChangePasswordDialog = false
                authViewModel.forgotPassword(currentEmail)
                Toast.makeText(context, "Đang gửi mã OTP đến email...", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Otp.createRoute(currentEmail, "RESET_PASS"))
            },
            onDismiss = { showChangePasswordDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CineTextWhite,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )
    }
}

@Composable
private fun SettingsNotificationToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = CineTextWhite
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CineTextWhite,
                checkedTrackColor = CineRedPrimary,
                uncheckedThumbColor = CineTextGray,
                uncheckedTrackColor = CineInputBackground,
                uncheckedBorderColor = CineBorder
            )
        )
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CineRedPrimary.copy(alpha = 0.2f) else CineInputBackground)
            .border(
                width = 1.dp,
                color = if (selected) CineRedPrimary else CineBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CineRedPrimary else CineTextGray,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) CineTextWhite else CineTextGray
        )
    }
}

@Composable
private fun ChangePasswordStandardDialog(
    email: String,
    authViewModel: AuthViewModel,
    onNavigateToForgotPassword: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.resetState()
    }

    LaunchedEffect(uiState.isResetPasswordSuccess) {
        if (uiState.isResetPasswordSuccess) {
            Toast.makeText(context, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
            authViewModel.resetState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            authViewModel.resetState()
            onDismiss()
        },
        containerColor = CineSurfaceCard,
        title = {
            Text(
                text = "Đổi mật khẩu",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Mật khẩu hiện tại
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        localError = null
                    },
                    label = { Text("Mật khẩu hiện tại", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                imageVector = if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CineTextWhite,
                        unfocusedTextColor = CineTextWhite,
                        focusedBorderColor = CineRedPrimary,
                        unfocusedBorderColor = CineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Mật khẩu mới
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        localError = null
                    },
                    label = { Text("Mật khẩu mới", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CineTextWhite,
                        unfocusedTextColor = CineTextWhite,
                        focusedBorderColor = CineRedPrimary,
                        unfocusedBorderColor = CineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Xác nhận mật khẩu mới
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Xác nhận mật khẩu mới", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CineTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CineTextWhite,
                        unfocusedTextColor = CineTextWhite,
                        focusedBorderColor = CineRedPrimary,
                        unfocusedBorderColor = CineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Thông báo lỗi nếu có
                val errorMessage = localError ?: uiState.errorMessage
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = CineError
                    )
                }

                // Dòng Quên mật khẩu? (Nếu không nhớ pass cũ -> chuyển sang luồng OTP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Quên mật khẩu hiện tại?",
                            fontSize = 12.sp,
                            color = CineWarning
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    if (cleanEmail.isBlank()) {
                        localError = "Không tìm thấy thông tin tài khoản đăng nhập"
                        return@Button
                    }
                    if (currentPassword.isBlank()) {
                        localError = "Vui lòng nhập mật khẩu hiện tại"
                        return@Button
                    }
                    if (newPassword.isBlank()) {
                        localError = "Vui lòng nhập mật khẩu mới"
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        localError = "Mật khẩu mới phải từ 6 ký tự trở lên"
                        return@Button
                    }
                    if (newPassword == currentPassword) {
                        localError = "Mật khẩu mới không được trùng với mật khẩu hiện tại"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        localError = "Mật khẩu xác nhận không khớp"
                        return@Button
                    }
                    authViewModel.changePassword(cleanEmail, currentPassword, newPassword)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = CineTextWhite,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu mật khẩu", color = CineTextWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    authViewModel.resetState()
                    onDismiss()
                },
                enabled = !uiState.isLoading
            ) {
                Text("Hủy", color = CineTextGray)
            }
        }
    )
}

// ============================================================================
// SCRUM-106 (CINE-30.2): MÀN HÌNH CHÍNH SÁCH & ĐIỀU KHOẢN (/I302 - /I313)
// ============================================================================

private enum class PolicyFilterTab(val label: String, val categoryKey: String) {
    ALL("Tất cả", "ALL"),
    FAQ("FAQ", "FAQ"),
    GUIDE("Hướng dẫn", "GUIDE"),
    POLICY("Chính sách", "POLICY")
}

private data class PolicyArticleItem(
    val id: String,
    val categoryKey: String,
    val title: String,
    val defaultContent: String
)

private val OFFICIAL_POLICY_ARTICLES = listOf(
    PolicyArticleItem(
        id = "faq_book_ticket",
        categoryKey = "FAQ",
        title = "Làm thế nào để đặt vé?",
        defaultContent = "Để đặt vé trên ứng dụng CineOne:\n" +
            "1. Chọn bộ phim hoặc cụm rạp bạn muốn xem từ Trang chủ hoặc mục Tìm kiếm.\n" +
            "2. Chọn ngày chiếu, khung giờ chiếu phù hợp.\n" +
            "3. Chọn vị trí ghế ngồi trên sơ đồ phòng chiếu thời gian thực (ghế được giữ tối đa 10 phút).\n" +
            "4. Chọn combo bắp nước (nếu có), áp dụng mã Voucher ưu đãi hoặc quyền lợi hạng thành viên.\n" +
            "5. Thanh toán qua mã QR VietQR (PayOS) để nhận ngay vé điện tử kèm mã QR Check-in."
    ),
    PolicyArticleItem(
        id = "faq_use_voucher",
        categoryKey = "FAQ",
        title = "Làm thế nào để sử dụng voucher?",
        defaultContent = "Tại bước Thanh toán (Checkout), bạn có thể nhập trực tiếp mã Voucher hoặc chọn từ Ví Voucher cá nhân của mình. Hệ thống sẽ tự động kiểm tra điều kiện đơn hàng tối thiểu, thời hạn sử dụng và hạng thành viên để khấu trừ trực tiếp vào tổng tiền thanh toán."
    ),
    PolicyArticleItem(
        id = "faq_update_profile",
        categoryKey = "FAQ",
        title = "Làm sao để đổi thông tin tài khoản?",
        defaultContent = "Bạn truy cập tab 'Tôi' ở thanh điều hướng dưới cùng -> nhấn nút 'Thông tin chi tiết' trên thẻ tài khoản. Tại màn hình 'Thông tin cá nhân', nhấn vào biểu tượng cây bút ở từng mục (Họ và tên, Số điện thoại, Ngày sinh, Địa chỉ nhà) rồi nhấn 'LƯU' để cập nhật."
    ),
    PolicyArticleItem(
        id = "guide_booking",
        categoryKey = "GUIDE",
        title = "Hướng dẫn đặt vé",
        defaultContent = "• Bước 1: Tìm kiếm phim theo tên, thể loại, ngày chiếu hoặc lọc rạp gần vị trí hiện tại / vị trí nhà đã thiết lập.\n" +
            "• Bước 2: Chọn suất chiếu và ghế ngồi trên sơ đồ phòng chiếu.\n" +
            "• Bước 3: Kiểm tra thông tin người nhận vé, chọn bắp nước & voucher.\n" +
            "• Bước 4: Quét mã VietQR trong thời gian giữ ghế 10 phút. Sau khi thanh toán thành công, vé sẽ hiển thị trong mục 'Vé của tôi'."
    ),
    PolicyArticleItem(
        id = "guide_payment",
        categoryKey = "GUIDE",
        title = "Hướng dẫn thanh toán",
        defaultContent = "CineOne hỗ trợ thanh toán trực tuyến qua cổng PayOS (VietQR):\n" +
            "• Mở ứng dụng Ngân hàng hoặc Ví điện tử hỗ trợ VietQR và quét mã QR hiển thị trên màn hình Thanh toán.\n" +
            "• Hệ thống tự động điền đúng số tiền và nội dung chuyển khoản.\n" +
            "• Ngay khi giao dịch hoàn tất, trạng thái đơn đặt vé sẽ tự động chuyển sang Thành công và phát hành mã QR vé điện tử."
    ),
    PolicyArticleItem(
        id = "policy_terms_of_use",
        categoryKey = "POLICY",
        title = "Điều khoản sử dụng",
        defaultContent = "1. Quy định chung: Khi đăng ký và sử dụng ứng dụng CineOne, khách hàng đồng ý tuân thủ các quy định về độ tuổi phân loại phim (P, K, T13, T16, T18) và nội quy tại rạp chiếu.\n" +
            "2. Quy định giữ ghế & thanh toán: Mỗi phiên giữ ghế có hiệu lực 10 phút. Quá thời gian trên nếu chưa thanh toán, hệ thống sẽ tự động giải phóng ghế.\n" +
            "3. Chính sách vé đã xuất: Vé điện tử đã thanh toán thành công được dùng để quét mã QR trực tiếp tại quầy kiểm soát vé của rạp."
    ),
    PolicyArticleItem(
        id = "policy_privacy",
        categoryKey = "POLICY",
        title = "Chính sách bảo mật",
        defaultContent = "CineOne cam kết bảo mật tuyệt đối thông tin cá nhân của khách hàng:\n" +
            "• Thông tin thu thập (Họ tên, Email, Số điện thoại, Ngày sinh, Địa chỉ nhà) chỉ được sử dụng để xác thực tài khoản, gửi vé điện tử, tính khoảng cách từ nhà tới rạp và áp dụng ưu đãi thành viên.\n" +
            "• Mật khẩu người dùng được mã hóa một chiều (BCrypt) và phiên đăng nhập được bảo vệ bằng chuẩn JWT.\n" +
            "• CineOne không chia sẻ dữ liệu cá nhân của khách hàng cho bên thứ ba khi chưa có sự đồng ý."
    )
)

@Composable
fun TermsAndPoliciesScreen(
    navController: NavController
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(PolicyFilterTab.ALL) }
    var selectedArticle by remember { mutableStateOf<PolicyArticleItem?>(null) }
    var remoteAnswer by remember { mutableStateOf<String?>(null) }
    var isLoadingRemote by remember { mutableStateOf(false) }

    val filteredArticles = remember(searchQuery, selectedTab) {
        val normQuery = LocationUtils.normalizeSearchText(searchQuery)
        OFFICIAL_POLICY_ARTICLES.filter { item ->
            val matchesCategory = selectedTab == PolicyFilterTab.ALL || item.categoryKey == selectedTab.categoryKey
            val matchesQuery = if (normQuery.isBlank()) {
                true
            } else {
                LocationUtils.normalizeSearchText("${item.title} ${item.defaultContent}").contains(normQuery)
            }
            matchesCategory && matchesQuery
        }
    }

    val faqItems = remember(filteredArticles) { filteredArticles.filter { it.categoryKey == "FAQ" } }
    val guideItems = remember(filteredArticles) { filteredArticles.filter { it.categoryKey == "GUIDE" } }
    val policyItems = remember(filteredArticles) { filteredArticles.filter { it.categoryKey == "POLICY" } }

    LaunchedEffect(selectedArticle) {
        val article = selectedArticle ?: return@LaunchedEffect
        remoteAnswer = null
        isLoadingRemote = true
        val fetched = withContext(Dispatchers.IO) {
            runCatching {
                val resp = CineOneApp.instance.networkModule.aiApi.askFaq(article.title)
                if (resp.success && resp.data != null && resp.data.answer.isNotBlank()) {
                    resp.data.answer.trim()
                } else {
                    null
                }
            }.getOrNull()
        }
        remoteAnswer = fetched
        isLoadingRemote = false
    }

    Scaffold(
        containerColor = CineBackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CineBackgroundDark)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = CineTextWhite
                    )
                }
                Text(
                    text = "Chính sách & Điều khoản",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Thanh tìm kiếm câu hỏi hoặc nội dung (/I303)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Tìm kiếm câu hỏi hoặc nội dung",
                        fontSize = 13.sp,
                        color = CineTextGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = CineTextGray,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa",
                                tint = CineTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CineTextWhite,
                    unfocusedTextColor = CineTextWhite,
                    focusedContainerColor = CineInputBackground,
                    unfocusedContainerColor = CineInputBackground,
                    focusedBorderColor = CineRedPrimary,
                    unfocusedBorderColor = CineBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Thanh lọc Segmented: Tất cả | FAQ | Hướng dẫn | Chính sách (/I304)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
            ) {
                PolicyFilterTab.entries.forEachIndexed { index, tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) CineRedPrimary.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CineRedPrimary else CineTextWhite,
                            maxLines = 1
                        )
                    }
                    if (index < PolicyFilterTab.entries.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(34.dp)
                                .background(CineBorder)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (filteredArticles.isEmpty()) {
                SectionEmptyBox(message = "Không tìm thấy câu hỏi hoặc nội dung phù hợp")
            } else {
                // Nhóm 1: Câu hỏi thường gặp (/I305, /I306, /I307, /I308)
                if (faqItems.isNotEmpty()) {
                    PolicySectionGroup(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Câu hỏi thường gặp",
                        items = faqItems,
                        onItemClick = { selectedArticle = it }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Nhóm 2: Hướng dẫn (/I309, /I310, /I311)
                if (guideItems.isNotEmpty()) {
                    PolicySectionGroup(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "Hướng dẫn",
                        items = guideItems,
                        onItemClick = { selectedArticle = it }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Nhóm 3: Chính sách (/I312, /I313)
                if (policyItems.isNotEmpty()) {
                    PolicySectionGroup(
                        icon = Icons.Default.Book,
                        title = "Chính sách",
                        items = policyItems,
                        onItemClick = { selectedArticle = it }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }

    if (selectedArticle != null) {
        val article = selectedArticle!!
        AlertDialog(
            onDismissRequest = { selectedArticle = null },
            containerColor = CineSurfaceCard,
            title = {
                Text(
                    text = article.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val contentToShow = remoteAnswer?.takeIf {
                        it.isNotBlank() && !it.contains("chưa có thông tin", ignoreCase = true)
                    } ?: article.defaultContent

                    Text(
                        text = contentToShow,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = CineTextWhite
                    )
                    if (isLoadingRemote) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CineRedPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Đang đồng bộ từ Knowledge Base...",
                                fontSize = 11.sp,
                                color = CineTextGray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedArticle = null }) {
                    Text("Đóng", color = CineRedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun PolicySectionGroup(
    icon: ImageVector,
    title: String,
    items: List<PolicyArticleItem>,
    onItemClick: (PolicyArticleItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CineTextWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = BorderStroke(1.dp, CineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            color = CineTextWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CineTextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = CineDivider)
                    }
                }
            }
        }
    }
}
