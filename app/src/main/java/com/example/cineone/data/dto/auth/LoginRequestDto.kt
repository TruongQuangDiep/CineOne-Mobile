package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String? = null,
    val password: String? = null
)
