package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val userId: Long,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val membershipTier: String? = "NORMAL",
    val isVerified: Boolean? = false,
    val totalSpending: Long? = 0L
)