package com.example.cineone.data.dto.food

import kotlinx.serialization.Serializable

@Serializable
data class FnbItemDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val imageUrl: String? = null
)
