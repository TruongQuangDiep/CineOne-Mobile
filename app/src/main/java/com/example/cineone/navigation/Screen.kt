package com.example.cineone.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object MovieDetail : Screen("movie_detail")
}
