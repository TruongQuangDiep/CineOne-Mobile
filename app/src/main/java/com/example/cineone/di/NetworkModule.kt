package com.example.cineone.di

import android.content.Context
import com.example.cineone.core.auth.AuthTokenProvider
import com.example.cineone.core.auth.SessionManager
import com.example.cineone.core.network.HttpClientFactory
import com.example.cineone.data.remote.auth.AuthApi
import com.example.cineone.data.remote.booking.BookingApi
import com.example.cineone.data.remote.seat.SeatApi
import com.example.cineone.data.remote.ticket.TicketApi
import com.example.cineone.data.remote.voucher.VoucherApi
import io.ktor.client.*

class NetworkModule(context: Context) {
    val sessionManager = SessionManager(context)
    val authTokenProvider = AuthTokenProvider(sessionManager)
    val httpClient: HttpClient = HttpClientFactory.create(authTokenProvider)

    val authApi by lazy { AuthApi(httpClient) }
    val seatApi by lazy { SeatApi(httpClient) }
    val bookingApi by lazy { BookingApi(httpClient) }
    val voucherApi by lazy { VoucherApi(httpClient) }
    val ticketApi by lazy { TicketApi(httpClient) }
}