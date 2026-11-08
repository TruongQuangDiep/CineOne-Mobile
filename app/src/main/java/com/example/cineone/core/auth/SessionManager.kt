package com.example.cineone.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.cineone.domain.model.User

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_DOB = "user_dob"
        private const val KEY_USER_ADDRESS = "user_address"
        private const val KEY_USER_TIER = "user_tier"
        private const val KEY_USER_VERIFIED = "user_verified"
        private const val KEY_USER_PHONE_VERIFIED = "user_phone_verified"
        private const val KEY_USER_AVATAR_URI = "user_avatar_uri"
        const val KEY_HOME_PINNED_LAT = "home_pinned_lat"
        const val KEY_HOME_PINNED_LON = "home_pinned_lon"
        const val KEY_HOME_PINNED_ADDRESS = "home_pinned_address"
    }

    fun saveAuthToken(token: String, userId: Long, email: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun saveUserProfile(user: User) {
        prefs.edit().apply {
            if (user.userId > 0L) putLong(KEY_USER_ID, user.userId)
            if (user.email.isNotBlank()) putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_FULL_NAME, user.fullName)
            putString(KEY_USER_PHONE, user.phone ?: "")
            if (!user.dateOfBirth.isNullOrBlank()) {
                putString(KEY_USER_DOB, user.dateOfBirth)
            }
            putString(KEY_USER_ADDRESS, user.address ?: "")
            if (user.membershipTier.isNotBlank()) {
                putString(KEY_USER_TIER, user.membershipTier)
            }
            putBoolean(KEY_USER_VERIFIED, user.isEmailVerified || user.isVerified)
            putBoolean(KEY_USER_PHONE_VERIFIED, user.isPhoneVerified)
            apply()
        }
    }

    fun saveUserHomeCoordinates(lat: Double, lon: Double, address: String) {
        prefs.edit().apply {
            putString(KEY_HOME_PINNED_LAT, lat.toString())
            putString(KEY_HOME_PINNED_LON, lon.toString())
            putString(KEY_HOME_PINNED_ADDRESS, address.trim())
            apply()
        }
    }

    fun fetchUserHomeCoordinates(forAddress: String? = null): Pair<Double, Double>? {
        val lat = prefs.getString(KEY_HOME_PINNED_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_HOME_PINNED_LON, null)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        if (!forAddress.isNullOrBlank()) {
            val savedAddr = prefs.getString(KEY_HOME_PINNED_ADDRESS, null)?.trim().orEmpty()
            if (savedAddr.isNotBlank() && !savedAddr.equals(forAddress.trim(), ignoreCase = true)) {
                return null
            }
        }
        return Pair(lat, lon)
    }

    fun saveUserDateOfBirth(dateOfBirth: String?) {
        prefs.edit().putString(KEY_USER_DOB, dateOfBirth?.trim().orEmpty()).apply()
    }

    fun fetchUserDateOfBirth(): String? =
        prefs.getString(KEY_USER_DOB, null)?.takeIf { it.isNotBlank() }

    fun saveUserAvatarUri(uri: String?) {
        prefs.edit().putString(KEY_USER_AVATAR_URI, uri?.trim().orEmpty()).apply()
    }

    fun fetchUserAvatarUri(): String? =
        prefs.getString(KEY_USER_AVATAR_URI, null)?.takeIf { it.isNotBlank() }

    fun fetchCachedUser(): User? {
        if (!isLoggedIn()) return null
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val fullName = prefs.getString(KEY_USER_FULL_NAME, "") ?: ""
        val phone = prefs.getString(KEY_USER_PHONE, null)?.takeIf { it.isNotBlank() }
        val dob = prefs.getString(KEY_USER_DOB, null)?.takeIf { it.isNotBlank() }
        val address = prefs.getString(KEY_USER_ADDRESS, null)?.takeIf { it.isNotBlank() }
        val tier = prefs.getString(KEY_USER_TIER, "NORMAL") ?: "NORMAL"
        val verified = prefs.getBoolean(KEY_USER_VERIFIED, true)
        return User(
            userId = fetchUserId().coerceAtLeast(0L),
            email = email,
            fullName = fullName,
            phone = phone,
            dateOfBirth = dob,
            address = address,
            membershipTier = tier,
            isVerified = verified
        )
    }

    fun fetchAuthToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun fetchUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun fetchUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = !fetchAuthToken().isNullOrBlank()
}
