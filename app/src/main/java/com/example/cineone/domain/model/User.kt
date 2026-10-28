package com.example.cineone.domain.model

data class User(
    val userId: Long,
    val email: String,
    val fullName: String,
    val phone: String? = null,
    val membershipTier: String = "NORMAL",
    val isVerified: Boolean = false
)