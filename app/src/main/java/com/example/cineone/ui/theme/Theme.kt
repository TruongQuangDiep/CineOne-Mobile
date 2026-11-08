package com.example.cineone.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppThemePreferences {
    const val PREFS_NAME = "cineone_settings"
    const val KEY_THEME_MODE = "theme_mode"
    const val MODE_SYSTEM = "SYSTEM"
    const val MODE_LIGHT = "LIGHT"
    const val MODE_DARK = "DARK"

    private val _themeMode = MutableStateFlow(MODE_DARK)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME_MODE, MODE_DARK) ?: MODE_DARK
    }

    fun setThemeMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }
}

data class CineColorPalette(
    val background: Color,
    val surface: Color,
    val inputBackground: Color,
    val border: Color,
    val borderMuted: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val DarkCineColors = CineColorPalette(
    background = Color(0xFF0F1014),
    surface = Color(0xFF1A1C23),
    inputBackground = Color(0xFF262933),
    border = Color(0xFF2E3342),
    borderMuted = Color(0xFF374151),
    divider = Color(0xFF262933),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280)
)

val LightCineColors = CineColorPalette(
    background = Color(0xFFF4F5F8),
    surface = Color(0xFFFFFFFF),
    inputBackground = Color(0xFFE5E7EB),
    border = Color(0xFFD1D5DB),
    borderMuted = Color(0xFFE5E7EB),
    divider = Color(0xFFE5E7EB),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF6B7280)
)

val LocalCineColors = staticCompositionLocalOf { DarkCineColors }

object CineTheme {
    val colors: CineColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCineColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = CineRedPrimary,
    background = Color(0xFF0F1014),
    surface = Color(0xFF1A1C23),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = CineRedPrimary,
    background = Color(0xFFF4F5F8),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

@Composable
fun CineOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val currentMode by AppThemePreferences.themeMode.collectAsState()
    val isDark = when (currentMode) {
        AppThemePreferences.MODE_LIGHT -> false
        AppThemePreferences.MODE_DARK -> true
        else -> darkTheme
    }

    val cineColors = if (isDark) DarkCineColors else LightCineColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalCineColors provides cineColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
