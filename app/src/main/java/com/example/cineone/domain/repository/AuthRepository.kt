package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, pass: String): Flow<Resource<User>>
    fun register(email: String, pass: String, name: String, phone: String?): Flow<Resource<User>>
    fun verifyOtp(email: String, otp: String): Flow<Resource<Unit>>
    fun getProfile(): Flow<Resource<User>>
    fun updateProfile(fullName: String, phone: String?, address: String?): Flow<Resource<User>>
}