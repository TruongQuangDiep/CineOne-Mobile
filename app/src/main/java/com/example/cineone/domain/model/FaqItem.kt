package com.example.cineone.domain.model

data class FaqItem(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val category: String = "FAQ"
)

