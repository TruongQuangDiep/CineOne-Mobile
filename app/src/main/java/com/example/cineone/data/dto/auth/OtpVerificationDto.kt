package com.example.cineone.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class OtpVerificationDto(
    val email: String? = null,
    val otpCode: String? = null
)
