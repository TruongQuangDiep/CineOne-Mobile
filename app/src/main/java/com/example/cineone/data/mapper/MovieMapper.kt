package com.example.cineone.data.mapper

import com.example.cineone.data.dto.movie.MovieDetailDto
import com.example.cineone.data.dto.movie.MovieDto
import com.example.cineone.domain.model.Movie

fun MovieDto.toDomain(): Movie = Movie(
    movieId = movieId,
    title = title,
    poster = poster ?: posterUrl.orEmpty(),
    trailer = trailer ?: trailerUrl.orEmpty(),
    duration = duration,
    releaseDate = releaseDate.orEmpty(),
    genres = genres,
    rating = rating,
    description = description.orEmpty(),
    ageRating = ageRating.orEmpty(),
    country = country.orEmpty(),
    director = director.orEmpty(),
    castInfo = castInfo.orEmpty(),
    status = status,
)

fun MovieDetailDto.toDomain(): Movie = Movie(
    movieId = movieId,
    title = title,
    poster = poster ?: posterUrl.orEmpty(),
    trailer = trailer ?: trailerUrl.orEmpty(),
    duration = duration,
    releaseDate = releaseDate.orEmpty(),
    genres = genres,
    rating = rating,
    description = description.orEmpty(),
    ageRating = ageRating.orEmpty(),
    country = country.orEmpty(),
    director = director.orEmpty(),
    castInfo = castInfo.orEmpty(),
    status = status,
)
