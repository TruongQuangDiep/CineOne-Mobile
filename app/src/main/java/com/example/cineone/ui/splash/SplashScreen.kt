package com.example.cineone.ui.splash

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cineone.CineOneApp
import com.example.cineone.R
import com.example.cineone.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(
    navController: NavController
) {
    val splashGreen = Color(0xFF07C160)
    val context = LocalContext.current
    val view = LocalView.current

    var isVisible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.88f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "momo_style_splash_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "momo_style_splash_alpha"
    )

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val greenArgb = splashGreen.toArgb()
        window?.statusBarColor = greenArgb
        window?.navigationBarColor = greenArgb
        onDispose {
            val darkArgb = 0xFF0F1014.toInt()
            window?.setBackgroundDrawableResource(R.color.cine_background_dark)
            window?.statusBarColor = darkArgb
            window?.navigationBarColor = darkArgb
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
        runCatching {
            context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                .edit()
                .putString("active_home_tab", "HOME")
                .apply()
        }

        val preloadJob = launch(Dispatchers.IO) {
            val getMoviesUseCase = runCatching {
                CineOneApp.instance.useCaseModule.getMoviesUseCase
            }.getOrNull()
            if (getMoviesUseCase != null) {
                launch {
                    runCatching {
                        getMoviesUseCase(q = null, status = "NOW_SHOWING", page = 0, size = 20).collect { }
                    }
                }
                launch {
                    runCatching {
                        getMoviesUseCase(q = null, status = null, page = 0, size = 20).collect { }
                    }
                }
            }
        }

        delay(1200L)
        withTimeoutOrNull(800L) {
            preloadJob.join()
        }

        val isLoggedIn = runCatching {
            CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        }.getOrDefault(false)
        val destination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashGreen),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_cineone_splash),
            contentDescription = "CineOne",
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = contentAlpha
                }
        )
    }
}
