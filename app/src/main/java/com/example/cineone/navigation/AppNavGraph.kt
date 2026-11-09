package com.example.cineone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cineone.ui.assistant.AssistantScreen
import com.example.cineone.ui.auth.ForgotPasswordScreen
import com.example.cineone.ui.auth.LoginScreen
import com.example.cineone.ui.auth.OtpScreen
import com.example.cineone.ui.auth.RegisterScreen
import com.example.cineone.ui.auth.ResetPasswordScreen
import com.example.cineone.ui.cinema.CinemaDetailScreen
import com.example.cineone.ui.cinema.CinemaListScreen
import com.example.cineone.ui.food.FoodSelectionScreen
import com.example.cineone.ui.home.HomeScreen
import com.example.cineone.ui.movie.MovieDetailScreen
import com.example.cineone.ui.movie.MovieListScreen
import com.example.cineone.ui.movie.MovieSearchScreen
import com.example.cineone.ui.seat.SeatSelectionScreen
import com.example.cineone.ui.showtime.ShowtimeScreen
import com.example.cineone.ui.booking.BookingScreen
import com.example.cineone.ui.payment.PaymentScreen
import com.example.cineone.ui.payment.PaymentResultScreen
import com.example.cineone.ui.ticket.TicketListScreen
import com.example.cineone.ui.ticket.TicketDetailScreen
import com.example.cineone.ui.membership.MembershipScreen
import com.example.cineone.ui.voucher.VoucherScreen

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

        // 6.0. Màn hình Trợ lý CineOne AI Chatbot (SCRUM-26 / CINE-22)
        composable(Screen.Assistant.route) {
            AssistantScreen(navController = navController)
        }

        // 6.1. Màn hình Chi tiết phim (SCRUM-87)
        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(
                navArgument("movieId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
            MovieDetailScreen(
                navController = navController,
                movieId = movieId
            )
        }

        // 6.2. Màn hình Danh sách phim ("Xem tất cả >" từ Trang chủ)
        composable(
            route = Screen.MovieList.route,
            arguments = listOf(
                navArgument("status") {
                    type = NavType.StringType
                    defaultValue = "NOW_SHOWING"
                }
            )
        ) { backStackEntry ->
            val status = backStackEntry.arguments?.getString("status") ?: "NOW_SHOWING"
            MovieListScreen(
                navController = navController,
                initialStatus = status
            )
        }

        // 6.3. Màn hình Tìm kiếm đa tiêu chí & Vị trí (SCRUM-25 / CINE-21 & SCRUM-26)
        composable(
            route = Screen.MovieSearch.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "CINEMA"
                },
                navArgument("locationMode") {
                    type = NavType.StringType
                    defaultValue = "NONE"
                },
                navArgument("city") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("genre") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("timeSlot") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            val tab = backStackEntry.arguments?.getString("tab") ?: "CINEMA"
            val locationMode = backStackEntry.arguments?.getString("locationMode") ?: "NONE"
            val city = backStackEntry.arguments?.getString("city") ?: ""
            val genre = backStackEntry.arguments?.getString("genre") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: "ALL"
            MovieSearchScreen(
                navController = navController,
                initialQuery = query,
                initialTab = tab,
                initialLocationMode = locationMode,
                initialCity = city,
                initialGenre = genre,
                initialDate = date,
                initialTimeSlot = timeSlot
            )
        }

        // 6.4. Màn hình Chọn theo rạp & Chi tiết rạp
        composable(Screen.CinemaList.route) {
            CinemaListScreen(navController = navController)
        }

        composable(
            route = Screen.CinemaDetail.route,
            arguments = listOf(
                navArgument("cinemaId") { type = NavType.LongType },
                navArgument("cinemaName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("timeSlot") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                }
            )
        ) { backStackEntry ->
            val cinemaId = backStackEntry.arguments?.getLong("cinemaId") ?: 0L
            val cinemaName = backStackEntry.arguments?.getString("cinemaName") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: "ALL"
            CinemaDetailScreen(
                navController = navController,
                cinemaId = cinemaId,
                cinemaName = cinemaName,
                initialDate = date,
                initialTimeSlot = timeSlot
            )
        }

        // 7. Màn hình Chọn suất chiếu (Duy trì tiêu chí tìm kiếm từ SCRUM-25 & SCRUM-26)
        composable(
            route = Screen.Showtime.route,
            arguments = listOf(
                navArgument("movieId") { type = NavType.LongType },
                navArgument("movieTitle") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("timeSlot") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                },
                navArgument("locationMode") {
                    type = NavType.StringType
                    defaultValue = "NONE"
                },
                navArgument("city") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("brand") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getLong("movieId") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val timeSlot = backStackEntry.arguments?.getString("timeSlot") ?: "ALL"
            val locationMode = backStackEntry.arguments?.getString("locationMode") ?: "NONE"
            val city = backStackEntry.arguments?.getString("city") ?: ""
            val brand = backStackEntry.arguments?.getString("brand") ?: ""
            ShowtimeScreen(
                navController = navController,
                movieId = movieId,
                movieTitle = movieTitle,
                initialDate = date,
                initialTimeSlot = timeSlot,
                initialLocationMode = locationMode,
                initialCity = city,
                initialBrand = brand
            )
        }

        // 8. Màn hình Sơ đồ chọn ghế Realtime
        composable(
            route = Screen.SeatSelection.route,
            arguments = listOf(
                navArgument("showtimeId") { type = NavType.LongType },
                navArgument("cinemaId") {
                    type = NavType.LongType
                    defaultValue = 1L
                },
                navArgument("movieTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("cinemaName") { type = NavType.StringType; defaultValue = "" },
                navArgument("startTime") { type = NavType.StringType; defaultValue = "" },
                navArgument("basePrice") { type = NavType.StringType; defaultValue = "85000" }
            )
        ) { backStackEntry ->
            val showtimeId = backStackEntry.arguments?.getLong("showtimeId") ?: 0L
            val cinemaId = backStackEntry.arguments?.getLong("cinemaId") ?: 1L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val cinemaName = backStackEntry.arguments?.getString("cinemaName") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
            val basePrice = backStackEntry.arguments?.getString("basePrice") ?: "85000"
            SeatSelectionScreen(
                navController = navController,
                showtimeId = showtimeId,
                cinemaId = cinemaId,
                movieTitle = movieTitle,
                cinemaName = cinemaName,
                startTime = startTime,
                basePriceStr = basePrice
            )
        }

        // 9. Màn hình Chọn Combo Bắp Nước & Tạm tính giỏ hàng (SCRUM-28)
        composable(
            route = Screen.FoodSelection.route,
            arguments = listOf(
                navArgument("cinemaId") { type = NavType.LongType },
                navArgument("showtimeId") { type = NavType.LongType },
                navArgument("movieTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("cinemaName") { type = NavType.StringType; defaultValue = "" },
                navArgument("startTime") { type = NavType.StringType; defaultValue = "" },
                navArgument("seatNames") { type = NavType.StringType; defaultValue = "" },
                navArgument("seatPrice") { type = NavType.StringType; defaultValue = "0" }
            )
        ) { backStackEntry ->
            val cinemaId = backStackEntry.arguments?.getLong("cinemaId") ?: 0L
            val showtimeId = backStackEntry.arguments?.getLong("showtimeId") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val cinemaName = backStackEntry.arguments?.getString("cinemaName") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
            val seatNames = backStackEntry.arguments?.getString("seatNames") ?: ""
            val seatPrice = backStackEntry.arguments?.getString("seatPrice") ?: "0"
            FoodSelectionScreen(
                navController = navController,
                cinemaId = cinemaId,
                showtimeId = showtimeId,
                movieTitle = movieTitle,
                cinemaName = cinemaName,
                startTime = startTime,
                seatNames = seatNames,
                seatPriceStr = seatPrice
            )
        }
        // 10. Màn hình Checkout chọn Voucher giảm giá (SCRUM-100)
        composable(
            route = Screen.Booking.route,
            arguments = listOf(
                navArgument("showtimeId") { type = NavType.LongType },
                navArgument("cinemaId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("movieTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("cinemaName") { type = NavType.StringType; defaultValue = "" },
                navArgument("startTime") { type = NavType.StringType; defaultValue = "" },
                navArgument("seatNames") { type = NavType.StringType; defaultValue = "" },
                navArgument("seatPrice") { type = NavType.LongType; defaultValue = 0L },
                navArgument("foodPrice") { type = NavType.LongType; defaultValue = 0L },
                navArgument("foodSummary") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val showtimeId = backStackEntry.arguments?.getLong("showtimeId") ?: 0L
            val cinemaId = backStackEntry.arguments?.getLong("cinemaId") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val cinemaName = backStackEntry.arguments?.getString("cinemaName") ?: ""
            val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
            val seatNames = backStackEntry.arguments?.getString("seatNames") ?: ""
            val seatPrice = backStackEntry.arguments?.getLong("seatPrice") ?: 0L
            val foodPrice = backStackEntry.arguments?.getLong("foodPrice") ?: 0L
            val foodSummary = backStackEntry.arguments?.getString("foodSummary") ?: ""

            BookingScreen(
                navController = navController,
                showtimeId = showtimeId,
                cinemaId = cinemaId,
                movieTitle = movieTitle,
                cinemaName = cinemaName,
                startTime = startTime,
                seatNames = seatNames,
                seatPrice = seatPrice,
                foodPrice = foodPrice,
                foodSummary = foodSummary
            )
        }

        // 11. Màn hình Thanh toán VietQR & Đếm ngược 10 phút (SCRUM-101)
        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.LongType },
                navArgument("bookingCode") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.LongType; defaultValue = 0L },
                navArgument("movieTitle") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: 0L
            val bookingCode = backStackEntry.arguments?.getString("bookingCode") ?: ""
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""

            PaymentScreen(
                navController = navController,
                bookingId = bookingId,
                bookingCode = bookingCode,
                amount = amount,
                movieTitle = movieTitle
            )
        }

        // 12. Màn hình Thông báo Đặt vé thành công (SCRUM-102)
        composable(
            route = Screen.PaymentResult.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.LongType },
                navArgument("bookingCode") { type = NavType.StringType; defaultValue = "" },
                navArgument("movieTitle") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong("bookingId") ?: 0L
            val bookingCode = backStackEntry.arguments?.getString("bookingCode") ?: ""
            val movieTitle = backStackEntry.arguments?.getString("movieTitle") ?: ""
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L

            PaymentResultScreen(
                navController = navController,
                bookingId = bookingId,
                bookingCode = bookingCode,
                movieTitle = movieTitle,
                amount = amount
            )
        }

        // 13. Màn hình Vé của tôi (SCRUM-103)
        composable(route = Screen.TicketList.route) {
            TicketListScreen(navController = navController)
        }

        // 14. Màn hình Chi tiết vé & Mã QR Check-in (SCRUM-103)
        composable(
            route = Screen.TicketDetail.route,
            arguments = listOf(
                navArgument("ticketId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getLong("ticketId") ?: 0L
            TicketDetailScreen(
                navController = navController,
                ticketId = ticketId
            )
        }

        composable(Screen.Membership.route) {
            MembershipScreen(navController = navController)
        }

        composable(Screen.VoucherList.route) {
            VoucherScreen(navController = navController)
        }
    }
}
