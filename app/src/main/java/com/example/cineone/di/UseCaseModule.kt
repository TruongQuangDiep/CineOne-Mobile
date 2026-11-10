package com.example.cineone.di

import com.example.cineone.domain.usecase.*

class UseCaseModule(private val repositoryModule: RepositoryModule) {
    // Auth UseCases
    val loginUseCase by lazy { LoginUseCase(repositoryModule.authRepository) }
    val registerUseCase by lazy { RegisterUseCase(repositoryModule.authRepository) }

    // Seat UseCases
    val getSeatsUseCase by lazy { GetSeatsUseCase(repositoryModule.seatRepository) }
    val holdSeatsUseCase by lazy { HoldSeatsUseCase(repositoryModule.seatRepository) }
    val releaseSeatsUseCase by lazy { ReleaseSeatsUseCase(repositoryModule.seatRepository) }

    // Booking & Voucher UseCases
    val createBookingUseCase by lazy { CreateBookingUseCase(repositoryModule.bookingRepository) }
    val applyVoucherUseCase by lazy { ApplyVoucherUseCase(repositoryModule.voucherRepository) }

    // Ticket UseCases (SCRUM-97)
    val getTicketsUseCase by lazy { GetTicketsUseCase(repositoryModule.ticketRepository) }

    // Catalog UseCases (SCRUM-23 & SCRUM-25)
    val getMoviesUseCase by lazy { GetMoviesUseCase(repositoryModule.movieRepository) }
    val searchMoviesUseCase by lazy { SearchMoviesUseCase(repositoryModule.movieRepository) }
    val getMovieDetailUseCase by lazy { GetMovieDetailUseCase(repositoryModule.movieRepository) }
    val getCinemasUseCase by lazy { GetCinemasUseCase(repositoryModule.cinemaRepository) }
    val getCinemaFnbsUseCase by lazy { GetCinemaFnbsUseCase(repositoryModule.cinemaRepository) }
    val getShowtimesUseCase by lazy { GetShowtimesUseCase(repositoryModule.showtimeRepository) }

    // AI Assistant UseCase (SCRUM-26)
    val askAssistantUseCase by lazy { AskAssistantUseCase(repositoryModule.aiRepository) }

    // Review UseCases (SCRUM-35)
    val createReviewUseCase by lazy { CreateReviewUseCase(repositoryModule.reviewRepository) }
}
