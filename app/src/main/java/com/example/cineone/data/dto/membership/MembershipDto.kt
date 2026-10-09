package com.example.cineone.data.dto.membership

import kotlinx.serialization.Serializable

@Serializable
data class MembershipDto(
    val id: String? = null,
    val userId: String? = null,
    val tier: String? = null,
    val points: Int? = null
)
