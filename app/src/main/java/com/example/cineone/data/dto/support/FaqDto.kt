package com.example.cineone.data.dto.support

import kotlinx.serialization.Serializable

@Serializable
data class FaqDto(
    val id: Long = 0L,
    val title: String? = null,
    val content: String? = null,
    val category: String? = null
)

