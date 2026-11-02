package com.example.cineone.di

import com.example.cineone.data.repository.*
import com.example.cineone.domain.repository.*

class RepositoryModule(private val networkModule: NetworkModule) {
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(networkModule.authApi, networkModule.sessionManager)
    }

    val seatRepository: SeatRepository by lazy {
        SeatRepositoryImpl(networkModule.seatApi)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepositoryImpl(networkModule.bookingApi)
    }

    val voucherRepository: VoucherRepository by lazy {
        VoucherRepositoryImpl(networkModule.voucherApi)
    }

    val ticketRepository: TicketRepository by lazy {
        TicketRepositoryImpl(networkModule.ticketApi)
    }

    // Catalog Repositories (SCRUM-23)
    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(networkModule.movieApi)
    }

    val cinemaRepository: CinemaRepository by lazy {
        CinemaRepositoryImpl(networkModule.cinemaApi)
    }

    val showtimeRepository: ShowtimeRepository by lazy {
        ShowtimeRepositoryImpl(networkModule.showtimeApi)
    }
}
