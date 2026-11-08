package com.example.cineone.data.repository

import com.example.cineone.core.auth.SessionManager
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.auth.LoginRequestDto
import com.example.cineone.data.dto.auth.OtpVerificationDto
import com.example.cineone.data.dto.auth.RegisterRequestDto
import com.example.cineone.data.dto.auth.UserDto
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
                authData.token?.let { token ->
                    sessionManager.saveAuthToken(token, authData.user.userId, authData.user.email)
                }
                val domainUser = authData.user.toDomain().let { u ->
                    u.copy(
                        dateOfBirth = u.dateOfBirth ?: sessionManager.fetchUserDateOfBirth(),
                        isVerified = true
                    )
                }
                sessionManager.saveUserProfile(domainUser)
                emit(Resource.Success(domainUser))
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
                authData.token?.let { token ->
                    sessionManager.saveAuthToken(token, authData.user.userId, authData.user.email)
                }
                val domainUser = authData.user.toDomain()
                sessionManager.saveUserProfile(domainUser)
                emit(Resource.Success(domainUser))
            } else {
                emit(Resource.Error(response.message ?: "Đăng ký thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }

    override fun verifyOtp(email: String, otp: String, type: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.verifyOtp(OtpVerificationDto(email = email, otp = otp, type = type))
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Mã OTP không đúng hoặc đã hết hạn"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi xác thực OTP"))
        }
    }

    override fun getProfile(): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.getProfile()
            if (response.success && response.data != null) {
                val remoteUser = response.data.toDomain()
                val mergedUser = remoteUser.copy(
                    dateOfBirth = remoteUser.dateOfBirth?.takeIf { it.isNotBlank() }
                        ?: sessionManager.fetchUserDateOfBirth(),
                    isVerified = response.data.isVerified ?: true
                )
                sessionManager.saveUserProfile(mergedUser)
                emit(Resource.Success(mergedUser))
            } else {
                val cached = sessionManager.fetchCachedUser()
                if (cached != null) {
                    emit(Resource.Success(cached))
                } else {
                    emit(Resource.Error(response.message ?: "Không thể lấy thông tin cá nhân"))
                }
            }
        } catch (e: Exception) {
            val cached = sessionManager.fetchCachedUser()
            if (cached != null) {
                emit(Resource.Success(cached))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
            }
        }
    }

    override fun updateProfile(
        fullName: String,
        phone: String?,
        address: String?,
        dateOfBirth: String?,
        phoneVerified: Boolean?
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            if (dateOfBirth != null) {
                sessionManager.saveUserDateOfBirth(dateOfBirth)
            }
            val requestDto = UserDto(
                fullName = fullName,
                phone = phone,
                dateOfBirth = dateOfBirth ?: sessionManager.fetchUserDateOfBirth(),
                address = address,
                phoneVerified = phoneVerified
            )
            val response = authApi.updateProfile(requestDto)
            if (response.success && response.data != null) {
                val remoteUser = response.data.toDomain()
                val updatedUser = remoteUser.copy(
                    dateOfBirth = remoteUser.dateOfBirth?.takeIf { it.isNotBlank() }
                        ?: dateOfBirth?.takeIf { it.isNotBlank() }
                        ?: sessionManager.fetchUserDateOfBirth(),
                    isVerified = response.data.isVerified ?: true
                )
                sessionManager.saveUserProfile(updatedUser)
                emit(Resource.Success(updatedUser))
            } else {
                emit(Resource.Error(response.message ?: "Cập nhật thông tin thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }

    override fun forgotPassword(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.forgotPassword(email)
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Gửi mã OTP thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }

    override fun resetPassword(email: String, newPassword: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.resetPassword(email, newPassword)
            if (response.success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Đặt lại mật khẩu thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng"))
        }
    }
}
