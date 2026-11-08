package com.example.cineone.domain.model

data class User(
    val userId: Long = 0L,
    val email: String = "",
    val fullName: String = "",
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val membershipTier: String = "NORMAL",
    val isVerified: Boolean = false,
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val totalSpending: Long = 0L,
    val role: String = "USER"
)