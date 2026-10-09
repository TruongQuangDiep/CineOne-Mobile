package com.example.cineone.data.dto.review

import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String? = null,
    val movieId: String? = null,
    val userId: String? = null,
    val rating: Int? = null,
    val comment: String? = null
)
