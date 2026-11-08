package com.example.cineone.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.domain.model.AiClarificationOption
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiFaqSource
import com.example.cineone.domain.model.AiMatchedCinemaShowtime
import com.example.cineone.domain.model.AiMessage
import com.example.cineone.domain.model.AiReviewSummary
import com.example.cineone.domain.model.AiSender
import com.example.cineone.domain.model.AiSuggestedMovieCard
import com.example.cineone.domain.model.Showtime
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.movie.MoviePosterBox
import com.example.cineone.ui.movie.SearchCategoryTab
import com.example.cineone.ui.movie.normalizeAgeBadge
import com.example.cineone.ui.showtime.formatTime
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    navController: NavController? = null,
    viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var pendingGpsPrompt by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val prompt = pendingGpsPrompt
        pendingGpsPrompt = null
        if (!prompt.isNullOrBlank()) {
            viewModel.sendPrompt(prompt, context)
        }
    }

    fun dispatchPromptWithPermissionCheck(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        focusManager.clearFocus()
        val norm = LocationUtils.normalizeSearchText(clean)
        val wantsGps = norm.contains("gan toi") || norm.contains("gan day") ||
            norm.contains("quanh day") || norm.contains("vi tri hien tai")
        if (wantsGps) {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) {
                pendingGpsPrompt = clean
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }
        }
        viewModel.sendPrompt(clean, context)
    }

    val coroutineScope = rememberCoroutineScope()
    var previousListHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            val targetIndex = (1 + uiState.messages.size + (if (uiState.isLoading) 1 else 0) - 1)
                .coerceAtLeast(0)
            runCatching {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trợ lý CineOne",
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
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearConversation() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới hội thoại",
                                tint = CineTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Làm mới",
                                fontSize = 12.sp,
                                color = CineTextGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CineBackgroundDark
                )
            )
        },
        bottomBar = {
            AssistantBottomInputBar(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                onInputChanged = viewModel::onInputTextChanged,
                onSendClick = {
                    dispatchPromptWithPermissionCheck(uiState.inputText)
                }
            )
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { newSize ->
                    val prev = previousListHeightPx
                    previousListHeightPx = newSize.height
                    if (prev > 0 && newSize.height < prev) {
                        val heightDelta = (prev - newSize.height).toFloat()
                        coroutineScope.launch {
                            listState.scrollBy(heightDelta)
                        }
                    }
                }
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            // 1. Phần mở đầu: Chào hỏi + Khám phá + Gợi ý nhanh + Hỏi đáp FAQ (Đúng thiết kế CineOne_giaodien.pdf)
            item(key = "assistant_welcome_section") {
                AssistantWelcomeOverview(
                    userGreetingName = uiState.userGreetingName,
                    discoverySlides = uiState.discoverySlides,
                    quickSuggestions = uiState.quickSuggestions,
                    onPromptSelected = { prompt ->
                        dispatchPromptWithPermissionCheck(prompt)
                    }
                )
            }

            // 2. Luồng tin nhắn hội thoại động (SCRUM-90) & Card phim gợi ý (SCRUM-91)
            if (uiState.messages.isNotEmpty()) {
                item(key = "conversation_divider") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = CineDivider
                        )
                        Text(
                            text = "  Nội dung hội thoại  ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CineTextMuted
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = CineDivider
                        )
                    }
                }
            }

            items(
                items = uiState.messages,
                key = { it.id }
            ) { message ->
                when (message.sender) {
                    AiSender.USER -> {
                        UserChatBubble(message = message)
                    }
                    AiSender.ASSISTANT -> {
                        AssistantChatBubble(
                            message = message,
                            onOptionClick = { opt ->
                                dispatchPromptWithPermissionCheck(opt.queryToSend)
                            },
                            onMovieDetailClick = { movieId ->
                                navController?.navigate(Screen.MovieDetail.createRoute(movieId))
                            },
                            onBookMovieWithCriteria = { card ->
                                val c = card.appliedCriteria
                                navController?.navigate(
                                    Screen.Showtime.createRoute(
                                        movieId = card.movie.movieId,
                                        movieTitle = card.movie.title,
                                        date = c.date.orEmpty(),
                                        timeSlot = c.timeSlot.name,
                                        locationMode = c.locationMode.name,
                                        city = c.city,
                                        brand = c.brand
                                    )
                                )
                            },
                            onSelectShowtimeDirect = { card, cinemaGroup, showtime ->
                                navController?.navigate(
                                    Screen.SeatSelection.createRoute(
                                        showtimeId = showtime.showtimeId,
                                        cinemaId = cinemaGroup.cinemaId,
                                        movieTitle = card.movie.title,
                                        cinemaName = cinemaGroup.cinemaName,
                                        startTime = showtime.startTime,
                                        basePrice = showtime.price.toString()
                                    )
                                )
                            },
                            onOpenSearchWithCriteria = { criteria ->
                                val targetTab = when {
                                    !criteria.date.isNullOrBlank() || criteria.timeSlot.name != "ALL" ->
                                        SearchCategoryTab.SHOW_DATE.name
                                    !criteria.genre.isNullOrBlank() ->
                                        SearchCategoryTab.GENRE.name
                                    !criteria.movieTitle.isNullOrBlank() ->
                                        SearchCategoryTab.MOVIE.name
                                    else ->
                                        SearchCategoryTab.CINEMA.name
                                }
                                val querySeed = criteria.movieTitle?.takeIf { it.isNotBlank() }
                                    ?: criteria.cinemaName.ifBlank { criteria.brand }
                                navController?.navigate(
                                    Screen.MovieSearch.createRoute(
                                        query = querySeed,
                                        tab = targetTab,
                                        locationMode = criteria.locationMode.name,
                                        city = criteria.city,
                                        genre = criteria.genre.orEmpty(),
                                        date = criteria.date.orEmpty(),
                                        timeSlot = criteria.timeSlot.name
                                    )
                                )
                            },
                            onOpenCinemaDetailWithCriteria = { cinemaItem, criteria ->
                                navController?.navigate(
                                    Screen.CinemaDetail.createRoute(
                                        cinemaId = cinemaItem.cinemaId,
                                        cinemaName = cinemaItem.cinemaName,
                                        date = criteria?.date.orEmpty(),
                                        timeSlot = criteria?.timeSlot?.name ?: "ALL"
                                    )
                                )
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item(key = "assistant_typing_indicator") {
                    AssistantTypingBubble()
                }
            }
        }
    }
}

@Composable
private fun AssistantWelcomeOverview(
    userGreetingName: String,
    discoverySlides: List<DiscoveryBannerSlide>,
    quickSuggestions: List<QuickSuggestionItem>,
    onPromptSelected: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { discoverySlides.size })

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Lời chào cá nhân hóa
        Text(
            text = "Chào $userGreetingName!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hôm nay bạn muốn tìm phim\nhay cần giải đáp thông tin gì",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = CineTextWhite,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Đường kẻ nét đứt (Dashed divider giống thiết kế PDF)
        DashedHorizontalDivider(
            color = CineBorder,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Карт Khám phá cùng trợ lý CineOne (1/3) + Pager Dots
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val slide = discoverySlides[page]
            DiscoveryBannerCard(
                slide = slide,
                onTryPromptClick = { onPromptSelected(slide.promptToSend) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Indicator Dots (● ○ ○)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(discoverySlides.size) { idx ->
                val isSelected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) CineRedPrimary else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CineRedPrimary else CineTextGray,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Gợi ý tìm kiếm nhanh
        Text(
            text = "Gợi ý tìm kiếm nhanh",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickSuggestions.forEach { item ->
                QuickSearchSuggestionCard(
                    title = item.title,
                    onClick = { onPromptSelected(item.promptToSend) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Hỏi đáp & Chính sách (FAQ):
        Text(
            text = "Hỏi đáp & Chính sách (FAQ):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FaqActionCard(
                icon = Icons.Default.Description,
                label = "Quy định đổi/ hủy vé",
                onClick = {
                    onPromptSelected("Quy định đổi/ hủy vé tại CineOne như thế nào?")
                },
                modifier = Modifier.weight(1.35f)
            )
            FaqActionCard(
                icon = Icons.Default.WorkspacePremium,
                label = "VIP",
                onClick = {
                    onPromptSelected("Quyền lợi và chính sách thành viên VIP tại CineOne?")
                },
                modifier = Modifier.weight(0.85f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FaqActionCard(
            icon = Icons.Default.VerifiedUser,
            label = "Hướng dẫn check-in",
            onClick = {
                onPromptSelected("Hướng dẫn check-in nhận vé tại rạp CineOne như thế nào?")
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiscoveryBannerCard(
    slide: DiscoveryBannerSlide,
    onTryPromptClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Khung Banner phía trên kèm nhãn "Khám phá cùng trợ lý CineOne (1/3)"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CineSurfaceCard,
                                CineInputBackground
                            )
                        )
                    )
                    .border(1.dp, CineBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineBackgroundDark.copy(alpha = 0.75f))
                        .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = slide.badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineTextWhite
                    )
                }
            }

            // Thanh hành động phía dưới: Câu mẫu bên trái + Nút "Dùng thử câu này >" bên phải
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slide.promptDisplay,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CineTextWhite,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CineInputBackground,
                    border = BorderStroke(1.dp, CineRedPrimary.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onTryPromptClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dùng thử câu này",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = CineTextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchSuggestionCard(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CineSurfaceCard,
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CineTextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FaqActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CineSurfaceCard,
        border = BorderStroke(1.dp, CineBorder),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CineInputBackground)
                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UserChatBubble(message: AiMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 14.dp,
                bottomEnd = 4.dp
            ),
            color = CineRedPrimary,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = CineTextWhite,
                    lineHeight = 20.sp
                )
                if (message.timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = CineTextWhite.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantChatBubble(
    message: AiMessage,
    onOptionClick: (AiClarificationOption) -> Unit,
    onMovieDetailClick: (Long) -> Unit,
    onBookMovieWithCriteria: (AiSuggestedMovieCard) -> Unit,
    onSelectShowtimeDirect: (AiSuggestedMovieCard, AiMatchedCinemaShowtime, Showtime) -> Unit,
    onOpenSearchWithCriteria: (AiExtractedCriteria) -> Unit,
    onOpenCinemaDetailWithCriteria: (AiMatchedCinemaShowtime, AiExtractedCriteria?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Header trợ lý
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(CineRedPrimary.copy(alpha = 0.2f))
                    .border(1.dp, CineRedPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = CineRedPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Trợ lý CineOne",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
            if (message.timestamp.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = message.timestamp,
                    fontSize = 11.sp,
                    color = CineTextMuted
                )
            }
        }

        // Khung nội dung trả lời
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 14.dp,
                bottomStart = 14.dp,
                bottomEnd = 14.dp
            ),
            color = CineSurfaceCard,
            border = BorderStroke(1.dp, CineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = parseBoldMarkdown(message.content),
                    fontSize = 14.sp,
                    color = CineTextWhite,
                    lineHeight = 20.sp
                )

                // Hiển thị các tiêu chí lọc đã bóc tách & nút điều hướng giữ bộ lọc sang màn Tìm kiếm
                val criteria = message.extractedCriteria
                val filterTags = criteria?.activeFilterTags().orEmpty()
                if (criteria != null && filterTags.isNotEmpty() && !criteria.requiresClarification) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CineInputBackground)
                            .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                tint = CineRedPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bộ lọc đã trích xuất:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextGray
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filterTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CineRedPrimary.copy(alpha = 0.16f))
                                        .border(
                                            1.dp,
                                            CineRedPrimary.copy(alpha = 0.55f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CineTextWhite
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenSearchWithCriteria(criteria) }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mở bộ lọc trên màn Tìm kiếm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineRedPrimary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CineRedPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Tóm tắt Review từ AI (UC22)
                if (message.reviewSummary != null) {
                    AiReviewSummaryCardView(summary = message.reviewSummary)
                }

                // Nguồn trích dẫn FAQ (UC22)
                if (message.faqSources.isNotEmpty()) {
                    AiFaqSourcesSection(sources = message.faqSources)
                }

                // Danh sách rạp phù hợp (nếu chưa có phim cụ thể)
                if (message.matchedCinemasOnly.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Rạp chiếu phù hợp:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextGray
                        )
                        message.matchedCinemasOnly.forEach { cinemaItem ->
                            val distStr = LocationUtils.formatDistanceKm(cinemaItem.distanceKm)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineInputBackground)
                                    .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onOpenCinemaDetailWithCriteria(
                                            cinemaItem,
                                            message.extractedCriteria
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cinemaItem.cinemaName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CineTextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cinemaItem.address.isNotBlank()) {
                                        Text(
                                            text = cinemaItem.address,
                                            fontSize = 11.sp,
                                            color = CineTextGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (distStr.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = distStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CineWarning
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = CineTextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Các nút chọn nhanh làm rõ câu hỏi (Clarification Chips)
                if (message.clarificationOptions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.clarificationOptions.forEach { option ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = CineInputBackground,
                                border = BorderStroke(1.dp, CineRedPrimary),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onOptionClick(option) }
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CineTextWhite,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card phim gợi ý & Điều hướng giữ trạng thái bộ lọc (SCRUM-91)
        if (message.suggestedMovies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                message.suggestedMovies.forEach { suggestedCard ->
                    SuggestedMovieChatCard(
                        card = suggestedCard,
                        onDetailClick = { onMovieDetailClick(suggestedCard.movie.movieId) },
                        onBookWithFiltersClick = { onBookMovieWithCriteria(suggestedCard) },
                        onShowtimeChipClick = { cinemaGroup, showtime ->
                            onSelectShowtimeDirect(suggestedCard, cinemaGroup, showtime)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiReviewSummaryCardView(summary: AiReviewSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CineInputBackground)
            .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = CineWarning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f/5.0", summary.averageRating),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CineWarning
                )
            }
            Text(
                text = "${summary.reviewCount} đánh giá từ khán giả",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray
            )
        }

        Text(
            text = summary.summary,
            fontSize = 13.sp,
            color = CineTextWhite,
            lineHeight = 19.sp
        )

        if (summary.highlights.isNotEmpty()) {
            HorizontalDivider(color = CineDivider)
            Text(
                text = "Điểm nổi bật:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite
            )
            summary.highlights.forEach { point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineRedPrimary
                    )
                    Text(
                        text = point,
                        fontSize = 12.sp,
                        color = CineTextGray,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiFaqSourcesSection(sources: List<AiFaqSource>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CineInputBackground)
            .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Nguồn thông tin tham khảo:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextGray
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sources.forEach { src ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "[${src.category}] ${src.title}",
                        fontSize = 11.sp,
                        color = CineTextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedMovieChatCard(
    card: AiSuggestedMovieCard,
    onDetailClick: () -> Unit,
    onBookWithFiltersClick: () -> Unit,
    onShowtimeChipClick: (AiMatchedCinemaShowtime, Showtime) -> Unit
) {
    val movie = card.movie
    val ageBadge = normalizeAgeBadge(movie.ageRating)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thông tin phim (Poster + Tên + Thể loại + Thời lượng + Rating)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                MoviePosterBox(
                    posterUrl = movie.poster,
                    title = movie.title,
                    ageRating = ageBadge,
                    modifier = Modifier
                        .width(78.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDetailClick)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = movie.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onDetailClick)
                    )

                    if (movie.genres.isNotBlank()) {
                        Text(
                            text = "Thể loại: ${movie.genres}",
                            fontSize = 12.sp,
                            color = CineTextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val metaParts = buildList {
                        if (movie.duration > 0) add("${movie.duration} phút")
                        if (movie.releaseDate.isNotBlank()) {
                            add("KC: ${DateTimeUtils.formatReleaseDate(movie.releaseDate)}")
                        }
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            fontSize = 11.sp,
                            color = CineTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Nút hành động: Chi tiết & Đặt vé (giữ trạng thái bộ lọc)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDetailClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CineBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(0.9f)
                        ) {
                            Text(
                                text = "Chi tiết",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CineTextWhite
                            )
                        }

                        Button(
                            onClick = onBookWithFiltersClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .weight(1.1f)
                        ) {
                            Text(
                                text = "Đặt vé",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite
                            )
                        }
                    }
                }
            }

            // Hiển thị các rạp và suất chiếu khớp trực tiếp trên Card phim gợi ý
            if (card.matchedCinemas.isNotEmpty()) {
                HorizontalDivider(color = CineDivider)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.matchedCinemas.forEach { cinemaGroup ->
                        val distLabel = LocationUtils.formatDistanceKm(cinemaGroup.distanceKm)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineInputBackground)
                                .border(1.dp, CineBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cinemaGroup.cinemaName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (distLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = distLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CineWarning
                                    )
                                }
                            }

                            if (cinemaGroup.showtimes.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = cinemaGroup.showtimes,
                                        key = { it.showtimeId }
                                    ) { st ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CineSurfaceCard)
                                                .border(
                                                    1.dp,
                                                    CineRedPrimary.copy(alpha = 0.7f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { onShowtimeChipClick(cinemaGroup, st) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formatTime(st.startTime),
                                                fontSize = 12.sp,
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

@Composable
private fun AssistantTypingBubble() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CineSurfaceCard)
            .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        CircularProgressIndicator(
            color = CineRedPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Trợ lý CineOne đang phân tích yêu cầu...",
            fontSize = 13.sp,
            color = CineTextGray
        )
    }
}

@Composable
private fun AssistantBottomInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = CineBackgroundDark,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        Column {
            HorizontalDivider(color = CineDivider, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CineInputBackground)
                        .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Nhập yêu cầu cho trợ lý CineOne",
                            fontSize = 13.sp,
                            color = CineTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = CineTextWhite,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(CineRedPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    onSendClick()
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                val canSend = inputText.trim().isNotBlank() && !isLoading
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSend) CineRedPrimary else CineInputBackground)
                        .border(
                            width = 1.dp,
                            color = if (canSend) CineRedPrimary else CineBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canSend, onClick = onSendClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi yêu cầu",
                        tint = if (canSend) CineTextWhite else CineTextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashedHorizontalDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

@Composable
private fun parseBoldMarkdown(text: String) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { index, segment ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CineTextWhite)) {
                append(segment)
            }
        } else {
            append(segment)
        }
    }
}
