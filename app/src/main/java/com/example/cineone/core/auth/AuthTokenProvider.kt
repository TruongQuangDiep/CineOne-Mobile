package com.example.cineone.core.auth

class AuthTokenProvider(private val sessionManager: SessionManager) {
    fun getToken(): String? = sessionManager.fetchAuthToken()
    fun getUserId(): Long = sessionManager.fetchUserId()
}