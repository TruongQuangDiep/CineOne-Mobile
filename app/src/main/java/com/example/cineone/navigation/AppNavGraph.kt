package com.example.cineone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cineone.ui.auth.ForgotPasswordScreen
import com.example.cineone.ui.auth.LoginScreen
import com.example.cineone.ui.auth.OtpScreen
import com.example.cineone.ui.auth.RegisterScreen
import com.example.cineone.ui.auth.ResetPasswordScreen
import com.example.cineone.ui.home.HomeScreen
import com.example.cineone.ui.showtime.ShowtimeScreen
import com.example.cineone.ui.seat.SeatSelectionScreen

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Màn hình Đăng nhập (Màn mặc định khi mở app)
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // 2. Màn hình Đăng ký tài khoản
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // 3. Màn hình Quên mật khẩu
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }

        // 4. Màn hình Xác thực OTP (Nhận tham số email và loại flow)
        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("flowType") {
                    type = NavType.StringType
                    defaultValue = "REGISTER"
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val flowType = backStackEntry.arguments?.getString("flowType") ?: "REGISTER"
            OtpScreen(
                navController = navController,
                email = email,
                flowType = flowType
            )
        }

        // 5. Màn hình Thiết lập mật khẩu mới (Nhận tham số email)
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(
                navController = navController,
                email = email
            )
        }

        // 6. Màn hình Trang chủ (Khi đăng nhập xong hoặc chọn "Khám phá với tư cách Khách")
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // 7. Màn hình Chọn suất chiếu
        composable(
            route = Screen.Showtime.route,
            arguments = listOf(
                navArgument("movieId") { type = NavType.LongType },
                navArgument("movieTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            ShowtimeScreen(
                navController = navController,
                movieId = movieId,
                movieTitle = movieTitle
            )
        }

        // 8. Màn hình Sơ đồ chọn ghế Realtime
        composable(
            route = Screen.SeatSelection.route,
            arguments = listOf(
                navArgument("showtimeId") { type = NavType.LongType },
                navArgument("movieTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("cinemaName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("startTime") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("basePrice") {
                    type = NavType.StringType
                    defaultValue = "85000"
                }
            )
        ) { backStackEntry ->
            val showtimeId = backStackEntry.arguments?.getLong("showtimeId") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val cinemaName = backStackEntry.arguments?.getString("cinemaName") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
            val basePrice = backStackEntry.arguments?.getString("basePrice") ?: "85000"
            SeatSelectionScreen(
                navController = navController,
                showtimeId = showtimeId,
                movieTitle = movieTitle,
                cinemaName = cinemaName,
                startTime = startTime,
                basePriceStr = basePrice
            )
        }
    }
}