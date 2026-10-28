package com.example.cineone.data.repository

import com.example.cineone.core.auth.SessionManager
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.auth.LoginRequestDto
import com.example.cineone.data.dto.auth.OtpVerificationDto
import com.example.cineone.data.dto.auth.RegisterRequestDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.auth.AuthApi
import com.example.cineone.domain.model.User
import com.example.cineone.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override fun login(email: String, pass: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.login(LoginRequestDto(email, pass))
            if (response.success && response.data != null) {
                val authData = response.data
                sessionManager.saveAuthToken(authData.token, authData.user.userId, authData.user.email)
                emit(Resource.Success(authData.user.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Đăng nhập thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }

    override fun register(email: String, pass: String, name: String, phone: String?): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.register(RegisterRequestDto(email, pass, name, phone))
            if (response.success && response.data != null) {
                val authData = response.data
                sessionManager.saveAuthToken(authData.token, authData.user.userId, authData.user.email)
                emit(Resource.Success(authData.user.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Đăng ký thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }

    override fun verifyOtp(email: String, otp: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.verifyOtp(OtpVerificationDto(email, otp))
            if (response.success) emit(Resource.Success(Unit))
            else emit(Resource.Error(response.message ?: "Mã OTP không đúng"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi xác thực OTP"))
        }
    }
}