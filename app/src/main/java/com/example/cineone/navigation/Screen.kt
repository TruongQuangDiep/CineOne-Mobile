package com.example.cineone.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Otp : Screen("otp/{email}?flowType={flowType}") {
        fun createRoute(email: String, flowType: String = "REGISTER") = "otp/$email?flowType=$flowType"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{email}") {
        fun createRoute(email: String) = "reset_password/$email"
    }
    object Home : Screen("home")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: Long) = "movie_detail/$movieId"
    }

    // SCRUM-92: Chọn suất chiếu
    object Showtime : Screen("showtime/{movieId}?movieTitle={movieTitle}") {
        fun createRoute(movieId: Long, movieTitle: String = "") = "showtime/$movieId?movieTitle=$movieTitle"
    }

    // SCRUM-93: Sơ đồ chọn ghế Realtime
    object SeatSelection : Screen("seat_selection/{showtimeId}?movieTitle={movieTitle}&cinemaName={cinemaName}&startTime={startTime}&basePrice={basePrice}") {
        fun createRoute(
            showtimeId: Long,
            movieTitle: String = "",
            cinemaName: String = "",
            startTime: String = "",
            basePrice: String = "0"
        ) = "seat_selection/$showtimeId?movieTitle=$movieTitle&cinemaName=$cinemaName&startTime=$startTime&basePrice=$basePrice"
    }
}