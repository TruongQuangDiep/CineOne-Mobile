package com.example.cineone.domain.model

data class ProviderSyncLog(
    val id: String,
    val providerId: String,
    val syncTime: String,
    val status: String
)
