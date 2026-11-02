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
    object MovieDetail : Screen("movie_detail")
}
