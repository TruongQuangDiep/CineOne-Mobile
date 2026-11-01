package com.example.cineone.data.remote.auth

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.auth.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto): ApiResponseDto<AuthResponseDto> {
        return client.post(ApiConfig.Auth.LOGIN) {
            setBody(request)
        }.body()
    }

    suspend fun register(request: RegisterRequestDto): ApiResponseDto<AuthResponseDto> {
        return client.post(ApiConfig.Auth.REGISTER) {
            setBody(request)
        }.body()
    }

    suspend fun verifyOtp(request: OtpVerificationDto): ApiResponseDto<Unit> {
        return client.post(ApiConfig.Auth.VERIFY_OTP) {
            setBody(request)
        }.body()
    }

    // Dùng trực tiếp UserDto cho Profile, không cần tạo thêm DTO mới
    suspend fun getProfile(): ApiResponseDto<UserDto> {
        return client.get(ApiConfig.User.PROFILE).body()
    }

    suspend fun updateProfile(request: UserDto): ApiResponseDto<UserDto> {
        return client.put(ApiConfig.User.PROFILE) {
            setBody(request)
        }.body()
    }

    suspend fun forgotPassword(email: String): ApiResponseDto<Unit> {
        return client.post(ApiConfig.Auth.FORGOT_PASSWORD) {
            setBody(ForgotPasswordRequestDto(email))
        }.body()
    }
    suspend fun resetPassword(email: String, newPassword: String): ApiResponseDto<Unit> {
        return client.post(ApiConfig.Auth.RESET_PASSWORD) {
            setBody(ResetPasswordRequestDto(email, newPassword))
        }.body()
    }
}