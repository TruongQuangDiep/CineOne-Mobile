package com.example.cineone.data.dto.cinema

import kotlinx.serialization.Serializable

@Serializable
data class CinemaDto(
    val id: String? = null,
    val name: String? = null,
    val address: String? = null,
    val city: String? = null
)
