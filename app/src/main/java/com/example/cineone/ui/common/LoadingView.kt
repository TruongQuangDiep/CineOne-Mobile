package com.example.cineone.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cineone.ui.theme.CineBorder
import com.example.cineone.ui.theme.CineRedPrimary
import com.example.cineone.ui.theme.CineSurfaceCard
import com.example.cineone.ui.theme.CineTextGray

@Composable
fun LoadingView(
    modifier: Modifier = Modifier.fillMaxSize(),
    message: String? = "Đang tải dữ liệu..."
) {
    val pulseTransition = rememberInfiniteTransition(label = "cine_loading_pulse")
    val iconScale by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cine_icon_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(CineSurfaceCard.copy(alpha = 0.92f))
                .border(1.dp, CineBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(CineRedPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalMovies,
                        contentDescription = null,
                        tint = CineRedPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                CircularProgressIndicator(
                    color = CineRedPrimary,
                    trackColor = CineBorder.copy(alpha = 0.45f),
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 2.8.dp,
                    strokeCap = StrokeCap.Round
                )
            }

            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = message,
                    color = CineTextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(110.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp)
                )
            }
        }
    }
}
