package com.example.cineone.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.User
import com.example.cineone.domain.repository.AuthRepository
import com.example.cineone.domain.usecase.LoginUseCase
import com.example.cineone.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isSuccess: Boolean = false,
    val isOtpSuccess: Boolean = false,
    val isForgotPasswordSuccess: Boolean = false,
    val isResetPasswordSuccess: Boolean = false,
    val isUpdateSuccess: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // 1. Đăng nhập
    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            loginUseCase(identifier, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            isSuccess = false,
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = resource.data,
                            isSuccess = true,
                            errorMessage = null,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = resource.message ?: "Tài khoản hoặc mật khẩu không chính xác",
                        )
                    }
                }
            }
        }
    }

    // 2. Đăng ký tài khoản (Gửi yêu cầu tạo user và kích hoạt gửi OTP)
    fun register(email: String, pass: String, name: String, phone: String?) {
        viewModelScope.launch {
            registerUseCase(email, pass, name, phone).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            isSuccess = false,
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = resource.data,
                            isSuccess = true,
                            errorMessage = null,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = resource.message ?: "Đăng ký thất bại",
                        )
                    }
                }
            }
        }
    }

    // 3. Xác thực mã OTP
    fun verifyOtp(email: String, otp: String, type: String = "EMAIL_VERIFY") {
        viewModelScope.launch {
            authRepository.verifyOtp(email, otp, type).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            isOtpSuccess = false,
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOtpSuccess = true,
                            errorMessage = null,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOtpSuccess = false,
                            errorMessage = resource.message ?: "Mã OTP không chính xác",
                        )
                    }
                }
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            authRepository.getProfile().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = resource.data,
                        errorMessage = null,
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resource.message,
                    )
                }
            }
        }
    }

    fun updateProfile(fullName: String, phone: String?, address: String?) {
        viewModelScope.launch {
            authRepository.updateProfile(fullName, phone, address).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, isUpdateSuccess = false)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = resource.data,
                        isUpdateSuccess = true,
                        errorMessage = null,
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUpdateSuccess = false,
                        errorMessage = resource.message,
                    )
                }
            }
        }
    }

    fun resetUpdateState() {
        _uiState.value = _uiState.value.copy(isUpdateSuccess = false, errorMessage = null)
    }

    // Gửi yêu cầu mã OTP Quên mật khẩu
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            authRepository.forgotPassword(email).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            isForgotPasswordSuccess = false,
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isForgotPasswordSuccess = true,
                            errorMessage = null,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isForgotPasswordSuccess = false,
                            errorMessage = resource.message ?: "Không thể gửi mã OTP",
                        )
                    }
                }
            }
        }
    }

    // Đặt lại mật khẩu mới
    fun resetPassword(email: String, newPassword: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email, newPassword).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            isResetPasswordSuccess = false,
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isResetPasswordSuccess = true,
                            errorMessage = null,
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isResetPasswordSuccess = false,
                            errorMessage = resource.message ?: "Đặt lại mật khẩu thất bại",
                        )
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            isOtpSuccess = false,
            isForgotPasswordSuccess = false,
            isResetPasswordSuccess = false,
            errorMessage = null,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                AuthViewModel(
                    loginUseCase = app.useCaseModule.loginUseCase,
                    registerUseCase = app.useCaseModule.registerUseCase,
                    authRepository = app.repositoryModule.authRepository,
                )
            }
        }
    }
}
