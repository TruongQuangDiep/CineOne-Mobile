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
    object Assistant : Screen("assistant")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: Long) = "movie_detail/$movieId"
    }
    object MovieList : Screen("movie_list?status={status}") {
        fun createRoute(status: String = "NOW_SHOWING") = "movie_list?status=$status"
    }

    object MovieSearch : Screen(
        "movie_search?query={query}&tab={tab}&locationMode={locationMode}&city={city}&genre={genre}&date={date}&timeSlot={timeSlot}"
    ) {
        fun createRoute(
            query: String = "",
            tab: String = "CINEMA",
            locationMode: String = "NONE",
            city: String = "",
            genre: String = "",
            date: String = "",
            timeSlot: String = "ALL"
        ): String {
            val encodedQuery = encodeParam(query)
            if (city.isEmpty() && genre.isEmpty() && date.isEmpty() && timeSlot == "ALL") {
                return "movie_search?query=$encodedQuery&tab=$tab&locationMode=$locationMode"
            }
            val encodedCity = encodeParam(city)
            val encodedGenre = encodeParam(genre)
            return "movie_search?query=$encodedQuery&tab=$tab&locationMode=$locationMode&city=$encodedCity&genre=$encodedGenre&date=$date&timeSlot=$timeSlot"
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

    object Showtime : Screen(
        "showtime/{movieId}?movieTitle={movieTitle}&date={date}&timeSlot={timeSlot}&locationMode={locationMode}&city={city}&brand={brand}"
    ) {
        fun createRoute(
            movieId: Long,
            movieTitle: String = "",
            date: String = "",
            timeSlot: String = "ALL",
            locationMode: String = "NONE",
            city: String = "",
            brand: String = ""
        ): String {
            if (date.isEmpty() && timeSlot == "ALL" && locationMode == "NONE" && city.isEmpty() && brand.isEmpty()) {
                return "showtime/$movieId?movieTitle=$movieTitle"
            }
            val encodedTitle = encodeParam(movieTitle)
            val encodedCity = encodeParam(city)
            val encodedBrand = encodeParam(brand)
            return "showtime/$movieId?movieTitle=$encodedTitle&date=$date&timeSlot=$timeSlot&locationMode=$locationMode&city=$encodedCity&brand=$encodedBrand"
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

    object Booking : Screen("booking/{showtimeId}?cinemaId={cinemaId}&movieTitle={movieTitle}&cinemaName={cinemaName}&startTime={startTime}&seatNames={seatNames}&seatPrice={seatPrice}&foodPrice={foodPrice}&foodSummary={foodSummary}") {
        fun createRoute(
            showtimeId: Long,
            cinemaId: Long = 0L,
            movieTitle: String = "",
            cinemaName: String = "",
            startTime: String = "",
            seatNames: String = "",
            seatPrice: Long = 0L,
            foodPrice: Long = 0L,
            foodSummary: String = ""
        ) = "booking/$showtimeId?cinemaId=$cinemaId&movieTitle=${encodeParam(movieTitle)}&cinemaName=${encodeParam(cinemaName)}&startTime=${encodeParam(startTime)}&seatNames=${encodeParam(seatNames)}&seatPrice=$seatPrice&foodPrice=$foodPrice&foodSummary=${encodeParam(foodSummary)}"
    }

    object Payment : Screen("payment/{bookingId}?bookingCode={bookingCode}&amount={amount}&movieTitle={movieTitle}") {
        fun createRoute(
            bookingId: Long,
            bookingCode: String = "",
            amount: Long = 0L,
            movieTitle: String = ""
        ) = "payment/$bookingId?bookingCode=${encodeParam(bookingCode)}&amount=$amount&movieTitle=${encodeParam(movieTitle)}"
    }

    object PaymentResult : Screen("payment_result/{bookingId}?bookingCode={bookingCode}&movieTitle={movieTitle}&amount={amount}") {
        fun createRoute(
            bookingId: Long,
            bookingCode: String = "",
            movieTitle: String = "",
            amount: Long = 0L
        ) = "payment_result/$bookingId?bookingCode=${encodeParam(bookingCode)}&movieTitle=${encodeParam(movieTitle)}&amount=$amount"
    }

    object TicketList : Screen("ticket_list")

    object TicketDetail : Screen("ticket_detail/{ticketId}") {
        fun createRoute(ticketId: Long) = "ticket_detail/$ticketId"
    }

    object Membership : Screen("membership")
    object VoucherList : Screen("voucher_list")
    object ProfileDetail : Screen("profile_detail")
    object HomeLocationSetup : Screen("home_location_setup")
    object Settings : Screen("settings")
    object TermsAndPolicies : Screen("terms_and_policies")

    object Review : Screen("review?movieId={movieId}&tab={tab}") {
        fun createRoute(movieId: Long = 0L, tab: String = "ALL"): String =
            "review?movieId=$movieId&tab=$tab"
    }

    object ReviewForm : Screen("review_form/{movieId}?movieTitle={movieTitle}&posterUrl={posterUrl}") {
        fun createRoute(movieId: Long, movieTitle: String = "", posterUrl: String = ""): String =
            "review_form/$movieId?movieTitle=${encodeParam(movieTitle)}&posterUrl=${encodeParam(posterUrl)}"
    }

    object MyReviews : Screen("my_reviews")

    object SupportList : Screen("support_list")

    object SupportForm : Screen("support_form?bookingCode={bookingCode}") {
        fun createRoute(bookingCode: String = ""): String =
            if (bookingCode.isBlank()) "support_form"
            else "support_form?bookingCode=${encodeParam(bookingCode)}"
    }

    object Faq : Screen("faq")
}

private fun encodeParam(value: String): String {
    if (value.isEmpty()) return ""
    return try {
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    } catch (_: Exception) {
        value
    }
}
