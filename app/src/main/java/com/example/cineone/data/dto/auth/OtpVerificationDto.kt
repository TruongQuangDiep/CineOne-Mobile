package com.example.cineone.data.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpVerificationDto(
    val email: String? = null,
    @SerialName("otp")
    val otp: String? = null,
    val type: String? = "EMAIL_VERIFY"
) {
    val otpCode: String? get() = otp
}