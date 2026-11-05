package com.example.cineone.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    object MovieSearch : Screen("movie_search?query={query}&tab={tab}&locationMode={locationMode}") {
        fun createRoute(
            query: String = "",
            tab: String = "CINEMA",
            locationMode: String = "NONE"
        ): String {
            val encodedQuery = encodeParam(query)
            return "movie_search?query=$encodedQuery&tab=$tab&locationMode=$locationMode"
        }
    }

    object CinemaList : Screen("cinema_list")

    object CinemaDetail : Screen("cinema_detail/{cinemaId}?cinemaName={cinemaName}&date={date}&timeSlot={timeSlot}") {
        fun createRoute(
            cinemaId: Long,
            cinemaName: String = "",
            date: String = "",
            timeSlot: String = "ALL"
        ): String {
            val encodedName = encodeParam(cinemaName)
            return "cinema_detail/$cinemaId?cinemaName=$encodedName&date=$date&timeSlot=$timeSlot"
        }
    }

    object Showtime : Screen("showtime/{movieId}?movieTitle={movieTitle}&date={date}&timeSlot={timeSlot}&locationMode={locationMode}&city={city}") {
        fun createRoute(
            movieId: Long,
            movieTitle: String = "",
            date: String = "",
            timeSlot: String = "ALL",
            locationMode: String = "NONE",
            city: String = ""
        ): String {
            if (date.isEmpty() && timeSlot == "ALL" && locationMode == "NONE" && city.isEmpty()) {
                return "showtime/$movieId?movieTitle=$movieTitle"
            }
            val encodedTitle = encodeParam(movieTitle)
            val encodedCity = encodeParam(city)
            return "showtime/$movieId?movieTitle=$encodedTitle&date=$date&timeSlot=$timeSlot&locationMode=$locationMode&city=$encodedCity"
        }
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

private fun encodeParam(value: String): String {
    if (value.isEmpty()) return ""
    return try {
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    } catch (_: Exception) {
        value
    }
}
