package com.example.cineone.core.network

object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:8080"

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

    object Voucher {
        const val MY_VOUCHERS = "/api/v1/vouchers/my-vouchers"
        const val APPLY = "/api/v1/vouchers/apply"
    }

    object Ticket {
        const val MY_TICKETS = "/api/v1/tickets/my-tickets"
        const val DETAIL = "/api/v1/tickets"
    }
}