package com.example.cineone.data.dto.cinema

import kotlinx.serialization.Serializable

@Serializable
data class CinemaDto(
    val cinemaId: Long = 0L,
    val brandId: Long? = null,
    val brandName: String? = null,
    val brandLogoUrl: String? = null,
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val roomCount: Long = 0L,
    val status: String = "ACTIVE",
)
