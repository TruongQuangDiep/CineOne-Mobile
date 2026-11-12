package com.example.cineone.di

import android.content.Context
import com.example.cineone.core.auth.AuthTokenProvider
import com.example.cineone.core.auth.SessionManager
import com.example.cineone.core.network.HttpClientFactory
import com.example.cineone.data.remote.ai.AiApi
import com.example.cineone.data.remote.auth.AuthApi
import com.example.cineone.data.remote.booking.BookingApi
import com.example.cineone.data.remote.cinema.CinemaApi
import com.example.cineone.data.remote.movie.MovieApi
import com.example.cineone.data.remote.seat.SeatApi
import com.example.cineone.data.remote.showtime.ShowtimeApi
import com.example.cineone.data.remote.ticket.TicketApi
import com.example.cineone.data.remote.voucher.VoucherApi
import com.example.cineone.data.remote.payment.PaymentApi
import com.example.cineone.data.remote.membership.MembershipApi
import com.example.cineone.data.remote.review.ReviewApi
import com.example.cineone.data.remote.support.SupportApi
import com.example.cineone.data.remote.notification.NotificationApi
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
    val paymentApi by lazy { PaymentApi(httpClient) }

    val membershipApi by lazy { MembershipApi(httpClient) }
    val notificationApi by lazy { NotificationApi(httpClient) }

    // Catalog APIs (SCRUM-23)
    val movieApi by lazy { MovieApi(httpClient) }
    val cinemaApi by lazy { CinemaApi(httpClient) }
    val showtimeApi by lazy { ShowtimeApi(httpClient) }

    // AI Assistant API (SCRUM-26)
    val aiApi by lazy { AiApi(httpClient) }

    val reviewApi by lazy { ReviewApi(httpClient) }

    val supportApi by lazy { SupportApi(httpClient) }
}
