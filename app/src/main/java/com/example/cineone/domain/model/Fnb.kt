package com.example.cineone.domain.model

data class Fnb(
    val fnbId: Long,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val cinemaId: Long,
    val cinemaName: String = "",
    val status: String = "ACTIVE",
)

