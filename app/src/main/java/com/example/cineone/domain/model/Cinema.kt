package com.example.cineone.domain.model

data class Cinema(
    val cinemaId: Long,
    val name: String,
    val address: String,
    val city: String,
    val roomCount: Long = 0L,
    val brandId: Long? = null,
    val brandName: String = "",
    val brandLogoUrl: String = "",
    val phone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String = "ACTIVE",
)

