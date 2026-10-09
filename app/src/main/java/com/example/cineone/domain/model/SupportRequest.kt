package com.example.cineone.domain.model

data class SupportRequest(
    val id: String,
    val userId: String,
    val subject: String,
    val message: String
)
