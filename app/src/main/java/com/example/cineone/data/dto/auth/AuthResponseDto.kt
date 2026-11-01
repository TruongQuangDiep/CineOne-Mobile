package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val token: String,
    val tokenType: String = "Bearer",
    val userId: Long = 0L,
    val email: String = "",
    val fullName: String = "",
    val role: String = "USER"
) {
    val user: UserDto
        get() = UserDto(
            userId = userId,
            email = email,
            fullName = fullName
        )
}