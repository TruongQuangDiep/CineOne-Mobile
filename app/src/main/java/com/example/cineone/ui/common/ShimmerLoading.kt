package com.example.cineone.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cineone.ui.theme.CineBorder
import com.example.cineone.ui.theme.CineDivider
import com.example.cineone.ui.theme.CineRedPrimary
import com.example.cineone.ui.theme.CineSurfaceCard
import com.example.cineone.ui.theme.CineTheme
import com.example.cineone.ui.theme.DarkCineColors

private val LocalShimmerTranslate = compositionLocalOf<State<Float>?> { null }

@Composable
fun ProvideCineShimmer(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "cine_shared_shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cine_shimmer_offset"
    )
    CompositionLocalProvider(LocalShimmerTranslate provides translateAnim) {
        content()
    }
}

@Composable
fun rememberCineShimmerBrush(onLightCard: Boolean = false): Brush {
    val sharedState = LocalShimmerTranslate.current
    val translate = if (sharedState != null) {
        sharedState.value
    } else {
        val localTransition = rememberInfiniteTransition(label = "cine_local_shimmer")
        val anim by localTransition.animateFloat(
            initialValue = -400f,
            targetValue = 1400f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cine_local_shimmer_offset"
        )
        anim
    }

    val isDark = CineTheme.colors.background == DarkCineColors.background
    val shimmerColors = when {
        onLightCard -> listOf(
            Color(0xFFE5E7EB),
            Color(0xFFF3F4F6),
            Color(0xFFE5E7EB)
        )
        isDark -> listOf(
            Color(0xFF1E212B),
            Color(0xFF2E3344),
            Color(0xFF1E212B)
        )
        else -> listOf(
            Color(0xFFE2E5EC),
            Color(0xFFF6F7FA),
            Color(0xFFE2E5EC)
        )
    }

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translate - 360f, translate - 360f),
        end = Offset(translate, translate)
    )
}

fun Modifier.cineShimmer(
    shape: Shape = RoundedCornerShape(8.dp),
    onLightCard: Boolean = false
): Modifier = composed {
    val brush = rememberCineShimmerBrush(onLightCard = onLightCard)
    this
        .clip(shape)
        .background(brush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    onLightCard: Boolean = false
) {
    Box(modifier = modifier.cineShimmer(shape = shape, onLightCard = onLightCard))
}

@Composable
fun ShimmerCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        modifier = modifier.size(size),
        shape = CircleShape
    )
}

@Composable
fun HomeCarouselSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(0.22f)
                        .height(210.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Column(
                    modifier = Modifier.weight(0.56f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(16.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(12.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                ShimmerBox(
                    modifier = Modifier
                        .weight(0.22f)
                        .height(210.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@Composable
fun HomeFeaturedRowSkeleton(
    itemCount: Int = 4
) {
    ProvideCineShimmer {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(itemCount) {
                Column(modifier = Modifier.width(136.dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(188.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(14.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(11.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomePromotionRowSkeleton(
    itemCount: Int = 3
) {
    ProvideCineShimmer {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(itemCount) {
                Column(
                    modifier = Modifier
                        .width(256.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(13.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(11.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeReviewRowSkeleton(
    itemCount: Int = 2
) {
    ProvideCineShimmer {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(itemCount) {
                Column(
                    modifier = Modifier
                        .width(272.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerCircle(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(13.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .height(10.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                        ShimmerBox(
                            modifier = Modifier
                                .width(44.dp)
                                .height(20.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MovieGridSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            repeat(rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    repeat(2) {
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.68f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(15.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(12.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieDetailSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(126.dp)
                        .height(184.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(20.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(46.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(72.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                repeat(3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(54.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(68.dp)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(5.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            repeat(4) { idx ->
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(if (idx == 3) 0.68f else 1f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineRedPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerCircle(size = 28.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(160.dp)
                            .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(7.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(7.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

@Composable
fun CinemaListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
    includeHorizontalPadding: Boolean = true
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (includeHorizontalPadding) 16.dp else 0.dp,
                    vertical = 6.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(56.dp)
                            .height(26.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShowtimeListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(itemCount) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(15.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(11.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CineDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    ShimmerBox(
                        modifier = Modifier
                            .width(96.dp)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) {
                            ShimmerBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatMapSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(7) { rowIdx ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(width = 16.dp, height = 14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier.size(26.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier.size(26.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
                if (rowIdx == 2) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
fun FoodListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(82.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(74.dp)
                                    .height(15.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(84.dp)
                                    .height(28.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(72.dp)
                            .height(102.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(16.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(22.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(22.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketDetailSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(13.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))

                ShimmerBox(
                    modifier = Modifier.size(188.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = CineDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                repeat(4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(90.dp)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeSkeleton(
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        onLightCard = true
    )
}

@Composable
fun NotificationListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .height(14.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(10.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(16.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                repeat(5) { idx ->
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(if (idx == 4) 0.6f else 1f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewMovieListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(68.dp)
                            .height(96.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(11.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(18.dp),
                                shape = RoundedCornerShape(5.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCommentListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(itemCount) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerCircle(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(13.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(10.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                        ShimmerBox(
                            modifier = Modifier
                                .width(44.dp)
                                .height(18.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(11.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(11.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VoucherListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(10.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(68.dp)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MembershipSkeleton(
    modifier: Modifier = Modifier
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerCircle(size = 48.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        ShimmerBox(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(
                            modifier = Modifier
                                .width(170.dp)
                                .height(12.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }
                ShimmerBox(
                    modifier = Modifier
                        .width(72.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CineSurfaceCard)
                    .border(1.dp, CineBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(180.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    shape = RoundedCornerShape(5.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

@Composable
fun SupportListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 4
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(itemCount) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .height(15.dp),
                            shape = RoundedCornerShape(5.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(74.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.38f)
                            .height(10.dp),
                            shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FaqListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    ProvideCineShimmer {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CineSurfaceCard)
                        .border(1.dp, CineBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(14.dp),
                        shape = RoundedCornerShape(5.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier.size(18.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}
