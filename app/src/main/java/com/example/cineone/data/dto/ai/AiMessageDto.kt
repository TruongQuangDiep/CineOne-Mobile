package com.example.cineone.data.dto.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiMessageDto(
    val id: String? = null,
    val sender: String? = null,
    val content: String? = null,
    val timestamp: String? = null
)
