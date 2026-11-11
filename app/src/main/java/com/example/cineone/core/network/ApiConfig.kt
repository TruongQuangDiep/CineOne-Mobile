package com.example.cineone.core.network

import android.os.Build

object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8080"
    private const val PHYSICAL_DEVICE_BASE_URL = "http://192.168.1.7:8080"

    fun activeBaseUrl(): String {
        val isEmulator = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
        return if (isEmulator) BASE_URL else PHYSICAL_DEVICE_BASE_URL
    }

    object Seat {
        const val HOLD = "/api/v1/seats/hold"
        const val RELEASE = "/api/v1/seats/release"
        const val HELD = "/api/v1/seats/held"
    }

    object Auth {
        const val LOGIN = "/api/v1/auth/login"
        const val REGISTER = "/api/v1/auth/register"
        const val VERIFY_OTP = "/api/v1/auth/verify-otp"
        const val FORGOT_PASSWORD = "/api/v1/auth/forgot-password"
        const val RESET_PASSWORD = "/api/v1/auth/reset-password"
    }

    object User {
        const val PROFILE = "/api/v1/users/profile"
    }

    object Booking {
        const val CREATE = "/api/v1/bookings"
    }

    object Membership {
        const val MY_TIER = "/api/v1/memberships/my-tier"
    }

    object Voucher {
        const val MY_VOUCHERS = "/api/v1/vouchers/my-vouchers"
        const val APPLY = "/api/v1/vouchers/apply"
        fun detail(code: String) = "/api/v1/vouchers/detail/$code"
    }

    object Ticket {
        const val MY_TICKETS = "/api/v1/tickets/my-tickets"
        const val DETAIL = "/api/v1/tickets"
    }

    object Movie {
        const val LIST = "/api/v1/movies"
        const val DETAIL = "/api/v1/movies"
        fun detail(id: Long) = "/api/v1/movies/$id"
    }

    object Cinema {
        const val LIST = "/api/v1/cinemas"
        const val DETAIL = "/api/v1/cinemas"
        fun detail(id: Long) = "/api/v1/cinemas/$id"
        fun fnbs(id: Long) = "/api/v1/cinemas/$id/fnbs"
    }

    object Showtime {
        const val LIST = "/api/v1/showtimes"
        fun seats(showtimeId: Long) = "/api/v1/showtimes/$showtimeId/seats"
    }

    object Fnb {
        const val BY_CINEMA = "/api/v1/cinemas"
        fun byCinema(cinemaId: Long) = "/api/v1/cinemas/$cinemaId/fnbs"
    }

    object Ai {
        const val FAQ = "/api/v1/ai/faq"
        const val SEARCH_CRITERIA = "/api/v1/ai/search-criteria"
        fun movieReviewSummary(movieId: Long) = "/api/v1/movies/$movieId/reviews/summary"
        fun aiMovieReviewSummary(movieId: Long) = "/api/v1/ai/movies/$movieId/review-summary"
    }

    object Payment {
        const val CREATE_LINK = "/api/v1/payments/create-link"
        const val STATUS = "/api/v1/payments/status"
    }

    object Review {
        fun movieReviews(movieId: Long) = "/api/v1/reviews/movie/$movieId"
        const val MY_REVIEWS = "/api/v1/reviews/my-reviews"
        fun eligibility(movieId: Long) = "/api/v1/reviews/eligibility/$movieId"
        const val CREATE = "/api/v1/reviews"
    }

    object Support {
        const val CREATE = "/api/v1/support"
        const val MY_REQUESTS = "/api/v1/support/my-requests"
        fun detail(id: Long) = "/api/v1/support/$id"
    }

    object Notification {
        const val LIST = "/api/v1/notifications"
        const val UNREAD_COUNT = "/api/v1/notifications/unread-count"
        fun markRead(id: Long) = "/api/v1/notifications/$id/read"
        const val MARK_ALL_READ = "/api/v1/notifications/read-all"
        fun detail(id: Long) = "/api/v1/notifications/$id"
    }

    object Faq {
        const val LIST = "/api/v1/faq"
    }
}
