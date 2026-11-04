package com.example.cineone.ui.movie

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.domain.model.Movie
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.common.ErrorView
import com.example.cineone.ui.common.LoadingView
import com.example.cineone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val posterBitmapCache = object : LruCache<String, ImageBitmap>(40) {}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    navController: NavController? = null,
    movieId: Long = 0L,
    viewModel: MovieViewModel = viewModel(factory = MovieViewModel.Factory)
) {
    val uiState by viewModel.detailUiState.collectAsState()
    val context = LocalContext.current
    var showTrailerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(movieId) {
        if (movieId > 0L) {
            viewModel.loadMovieDetail(movieId)
        }
    }

    val movie = uiState.movie

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thông tin",
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
        bottomBar = {
            if (movie != null) {
                Surface(
                    color = CineSurfaceCard,
                    tonalElevation = 8.dp,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(CineBorder)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = {
                                val encodedTitle = Uri.encode(movie.title)
                                navController?.navigate(
                                    Screen.Showtime.createRoute(
                                        movieId = movie.movieId,
                                        movieTitle = encodedTitle
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CineRedPrimary
                            )
                        ) {
                            Text(
                                text = "TIẾP TỤC",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    message = "Đang tải thông tin phim..."
                )
            }

            uiState.errorMessage != null && movie == null -> {
                ErrorView(
                    message = uiState.errorMessage ?: "Không thể tải thông tin phim",
                    onRetry = { viewModel.loadMovieDetail(movieId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            movie != null -> {
                MovieDetailContent(
                    movie = movie,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onTrailerClick = {
                        val trailerUrl = movie.trailerUrl.trim()
                        if (trailerUrl.startsWith("http://") || trailerUrl.startsWith("https://")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showTrailerDialog = true
                            }
                        } else {
                            showTrailerDialog = true
                        }
                    }
                )
            }
        }
    }

    if (showTrailerDialog && movie != null) {
        AlertDialog(
            onDismissRequest = { showTrailerDialog = false },
            containerColor = CineSurfaceCard,
            title = {
                Text(
                    text = "Trailer - ${movie.title}",
                    color = CineTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = if (movie.trailerUrl.isNotBlank()) {
                        "Đường dẫn Trailer: ${movie.trailerUrl}"
                    } else {
                        "Trailer chính thức của bộ phim \"${movie.title}\" đang được cập nhật."
                    },
                    color = CineTextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showTrailerDialog = false }) {
                    Text("Đóng", color = CineRedPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun MovieDetailContent(
    movie: Movie,
    modifier: Modifier = Modifier,
    onTrailerClick: () -> Unit
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val castMembers = remember(movie.castInfo, movie.director) {
        val rawCast = movie.castInfo
            .split(",", ";", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (rawCast.isNotEmpty()) {
            rawCast
        } else if (movie.director.isNotBlank()) {
            listOf(movie.director)
        } else {
            listOf("Đang cập nhật")
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. HEADER: Poster + Nút Trailer bên trái | Tên phim, Thể loại, Độ tuổi bên phải
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(118.dp)
            ) {
                MoviePosterBox(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    ageRating = normalizeAgeBadge(movie.ageRating),
                    modifier = Modifier
                        .width(118.dp)
                        .height(164.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onTrailerClick,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, CineRedPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CineSurfaceCard,
                        contentColor = CineTextWhite
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CineRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Trailer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CineTextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineTextWhite,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                BulletInfoRow(
                    text = movie.genres.ifBlank { "Đang cập nhật thể loại" }
                )

                Spacer(modifier = Modifier.height(6.dp))

                BulletInfoRow(
                    text = formatAgeDescription(movie.ageRating)
                )

                if (movie.director.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    BulletInfoRow(
                        text = "Đạo diễn: ${movie.director}"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. THÔNG TIN 3 CỘT: Ngày khởi chiếu | Thời lượng | Ngôn ngữ
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataColumn(
                    label = "Ngày khởi chiếu",
                    value = DateTimeUtils.formatReleaseDate(movie.releaseDate),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(CineBorder)
                )

                MetadataColumn(
                    label = "Thời lượng",
                    value = if (movie.duration > 0) "${movie.duration} phút" else "Đang cập nhật",
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(CineBorder)
                )

                MetadataColumn(
                    label = "Ngôn ngữ",
                    value = formatLanguageLabel(movie.country),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. NỘI DUNG PHIM (Có nút Xem thêm / Thu gọn)
        Text(
            text = "Nội dung phim",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val descriptionText = movie.description.ifBlank {
                    "Nội dung chi tiết của bộ phim đang được cập nhật."
                }
                Text(
                    text = descriptionText,
                    fontSize = 14.sp,
                    color = CineTextGray,
                    lineHeight = 21.sp,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (descriptionText.length > 140) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Thu gọn" else "Xem thêm",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineRedPrimary,
                        modifier = Modifier.clickable {
                            isDescriptionExpanded = !isDescriptionExpanded
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. DIỄN VIÊN (Cuộn ngang giống thiết kế)
        Text(
            text = "Diễn Viên",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(castMembers) { actorName ->
                CastMemberItem(name = actorName)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. ĐÁNH GIÁ (Điểm rating thực của phim từ Web Admin)
        RatingSummaryCard(rating = movie.rating)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BulletInfoRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = CineRedPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = CineTextGray,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun MetadataColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CineTextGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CineTextWhite,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CastMemberItem(name: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CineInputBackground)
                .border(1.dp, CineBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = name,
                tint = CineTextGray,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            color = CineTextGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun RatingSummaryCard(
    rating: Double
) {
    val scoreOutOf5 = remember(rating) {
        when {
            rating <= 0.0 -> 0.0
            rating > 5.0 -> (rating / 2.0).coerceIn(0.0, 5.0)
            else -> rating.coerceIn(0.0, 5.0)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(CineBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(116.dp)
            ) {
                Text(
                    text = "Lượt đánh giá",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (scoreOutOf5 > 0.0) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = CineWarning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (scoreOutOf5 > 0.0) {
                            String.format(Locale.US, "%.1f/5", scoreOutOf5).replace(".0/5", "/5")
                        } else {
                            "Chưa có"
                        },
                        fontSize = if (scoreOutOf5 > 0.0) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (scoreOutOf5 > 0.0) {
                        formatMovieRatingBadge(rating)
                    } else {
                        "Chưa có đánh giá"
                    },
                    fontSize = 11.sp,
                    color = CineTextMuted
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Hiển thị 5 ngôi sao theo điểm rating thực của phim
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { index ->
                    val icon = when {
                        scoreOutOf5 >= index -> Icons.Filled.Star
                        scoreOutOf5 >= index - 0.5 -> Icons.AutoMirrored.Filled.StarHalf
                        else -> Icons.Outlined.StarOutline
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CineWarning,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MoviePosterBox(
    posterUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    ageRating: String = "",
    rankBadge: Int? = null
) {
    val cleanUrl = posterUrl.trim()
    val imageBitmap by produceState<ImageBitmap?>(initialValue = posterBitmapCache.get(cleanUrl), key1 = cleanUrl) {
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            val cached = posterBitmapCache.get(cleanUrl)
            if (cached != null) {
                value = cached
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    try {
                        val connection = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 5000
                            readTimeout = 5000
                            doInput = true
                        }
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (loaded != null) {
                    posterBitmapCache.put(cleanUrl, loaded)
                    value = loaded
                }
            }
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    CineInputBackground,
                    CineSurfaceCard,
                    Color(0xFF14161D)
                )
            )
        )
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = CineRedPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CineTextWhite,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Badge độ tuổi ở góc trên trái (chỉ hiển thị khi DB có phân loại độ tuổi)
        if (ageRating.isNotBlank()) {
            val badgeBg = when {
                ageRating.contains("18") -> CineRedPrimary
                ageRating.contains("16") -> Color(0xFFEA580C)
                ageRating.contains("13") -> CineWarning
                else -> CineSuccess
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ageRating,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CineTextWhite
                )
            }
        }

        // Số thứ tự xếp hạng ở góc dưới trái (1, 2, 3...) trên Carousel Trang chủ
        if (rankBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC0F1014))
                    .border(1.dp, CineBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rankBadge.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CineTextWhite
                )
            }
        }
    }
}

fun normalizeAgeBadge(raw: String): String {
    val upper = raw.trim().uppercase(Locale.ROOT)
    return when {
        upper.isBlank() || upper == "—" -> ""
        upper.contains("18") -> "18+"
        upper.contains("16") -> "16+"
        upper.contains("13") -> "13+"
        upper == "K" -> "K"
        upper == "P" -> "P"
        else -> raw.trim()
    }
}

fun formatAgeDescription(raw: String): String {
    val badge = normalizeAgeBadge(raw)
    return when (badge) {
        "" -> "Phân loại độ tuổi: Chưa cập nhật"
        "18+" -> "Phim phổ biến cho người trên 18 tuổi"
        "16+" -> "Phim phổ biến cho người trên 16 tuổi"
        "13+" -> "Phim phổ biến cho người trên 13 tuổi"
        "K" -> "Phim phổ biến cho khán giả dưới 13 tuổi có người bảo hộ"
        "P" -> "Phim phổ biến cho mọi độ tuổi"
        else -> "Phân loại độ tuổi: $badge"
    }
}

private fun formatLanguageLabel(country: String): String {
    val clean = country.trim()
    return when {
        clean.equals("Việt Nam", ignoreCase = true) || clean.equals("VN", ignoreCase = true) -> "Tiếng Việt"
        clean.isNotBlank() -> "Phụ đề / Thuyết minh"
        else -> "Thuyết minh"
    }
}
