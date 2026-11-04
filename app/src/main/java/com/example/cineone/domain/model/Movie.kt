package com.example.cineone.domain.model

data class Movie(
    val movieId: Long,
    val title: String,
    val poster: String,
    val trailer: String,
    val duration: Int,
    val releaseDate: String,
    val genres: String,
    val rating: Double,
    val description: String,
    val ageRating: String = "",
    val country: String = "",
    val director: String = "",
    val castInfo: String = "",
    val status: String = "NOW_SHOWING",
) {
    val posterUrl: String get() = poster
    val trailerUrl: String get() = trailer
}

typealias MovieDetail = Movie
