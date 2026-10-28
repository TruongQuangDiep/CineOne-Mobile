package com.example.cineone.di

import com.example.cineone.domain.usecase.*

class UseCaseModule(private val repositoryModule: RepositoryModule) {
    // Auth UseCases
    val loginUseCase by lazy { LoginUseCase(repositoryModule.authRepository) }
    val registerUseCase by lazy { RegisterUseCase(repositoryModule.authRepository) }

    // Seat UseCases
    val holdSeatsUseCase by lazy { HoldSeatsUseCase(repositoryModule.seatRepository) }
    val releaseSeatsUseCase by lazy { ReleaseSeatsUseCase(repositoryModule.seatRepository) }

    // Booking & Voucher UseCases
    val createBookingUseCase by lazy { CreateBookingUseCase(repositoryModule.bookingRepository) }
    val applyVoucherUseCase by lazy { ApplyVoucherUseCase(repositoryModule.voucherRepository) }
}