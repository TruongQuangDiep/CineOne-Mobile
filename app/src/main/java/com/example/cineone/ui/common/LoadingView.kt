package com.example.cineone.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cineone.ui.theme.CineRedPrimary
import com.example.cineone.ui.theme.CineTextGray

@Composable
fun LoadingView(
    modifier: Modifier = Modifier.fillMaxSize(),
    message: String? = "Đang tải dữ liệu..."
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = CineRedPrimary,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = CineTextGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
