package com.example.cineone.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Brand / Primary
val CineRedPrimary = Color(0xFFE50914)
val CineRedGradientEnd = Color(0xFFB8050E)

// Dynamic Background & Surface (Tự động đổi theo Sáng / Tối)
val CineBackgroundDark: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.background

val CineSurfaceCard: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.surface

val CineInputBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.inputBackground

// Dynamic Borders & Dividers
val CineBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.border

val CineBorderMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.borderMuted

val CineDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.divider

// Dynamic Text Colors
val CineTextWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.textPrimary

val CineTextGray: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.textSecondary

val CineTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = CineTheme.colors.textMuted

// Status / Feedback
val CineError = Color(0xFFFF5252)
val CineSuccess = Color(0xFF4CAF50)
val CineWarning = Color(0xFFF59E0B)
