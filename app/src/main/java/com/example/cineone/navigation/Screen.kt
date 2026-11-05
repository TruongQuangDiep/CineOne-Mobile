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
    object MovieList : Screen("movie_list?status={status}") {
        fun createRoute(status: String = "NOW_SHOWING") = "movie_list?status=$status"
    }

    object Showtime : Screen("showtime/{movieId}?movieTitle={movieTitle}") {
        fun createRoute(movieId: Long, movieTitle: String = "") = "showtime/$movieId?movieTitle=$movieTitle"
    }

    object SeatSelection : Screen("seat_selection/{showtimeId}?cinemaId={cinemaId}&movieTitle={movieTitle}&cinemaName={cinemaName}&startTime={startTime}&basePrice={basePrice}") {
        fun createRoute(
            showtimeId: Long,
            cinemaId: Long = 0L,
            movieTitle: String = "",
            cinemaName: String = "",
            startTime: String = "",
            basePrice: String = "85000"
        ) = "seat_selection/$showtimeId?cinemaId=$cinemaId&movieTitle=$movieTitle&cinemaName=$cinemaName&startTime=$startTime&basePrice=$basePrice"
    }

    object FoodSelection : Screen("food_selection/{cinemaId}?showtimeId={showtimeId}&movieTitle={movieTitle}&cinemaName={cinemaName}&startTime={startTime}&seatNames={seatNames}&seatPrice={seatPrice}") {
        fun createRoute(
            cinemaId: Long,
            showtimeId: Long,
            movieTitle: String = "",
            cinemaName: String = "",
            startTime: String = "",
            seatNames: String = "",
            seatPrice: String = "0"
        ) = "food_selection/$cinemaId?showtimeId=$showtimeId&movieTitle=$movieTitle&cinemaName=$cinemaName&startTime=$startTime&seatNames=$seatNames&seatPrice=$seatPrice"
    }
}
