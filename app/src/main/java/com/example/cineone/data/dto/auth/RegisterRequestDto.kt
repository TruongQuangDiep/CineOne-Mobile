package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String? = null,
    val password: String? = null,
    val fullName: String? = null,
    val phoneNumber: String? = null
)
