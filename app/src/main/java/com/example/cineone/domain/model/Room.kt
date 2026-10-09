package com.example.cineone.domain.model

data class Room(
    val id: String,
    val name: String,
    val cinemaId: String,
    val capacity: Int
)
