package com.example.cineone.data.dto.food

import kotlinx.serialization.Serializable

@Serializable
data class FnbDto(
    val fnbId: Long = 0L,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val cinemaId: Long = 0L,
    val cinemaName: String? = null,
    val brandId: Long? = null,
    val brandName: String? = null,
    val status: String = "ACTIVE",
)

