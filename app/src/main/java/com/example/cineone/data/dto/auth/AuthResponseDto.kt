package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    val token: String? = null,
    val tokenType: String? = "Bearer",
    val userId: Long? = 0L,
    val email: String? = "",
    val fullName: String? = "",
    val role: String? = "USER"
) {
    val user: UserDto
        get() = UserDto(
            userId = userId ?: 0L,
            email = email ?: "",
            fullName = fullName ?: ""
        )
}
@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)
@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val newPassword: String
)