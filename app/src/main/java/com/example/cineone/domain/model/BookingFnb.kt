package com.example.cineone.domain.model

data class BookingFnb(
    val bookingId: String,
    val fnbItemId: String,
    val quantity: Int,
    val price: Double
)
