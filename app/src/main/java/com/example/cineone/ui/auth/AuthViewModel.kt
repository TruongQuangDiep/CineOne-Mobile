package com.example.cineone.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isUpdateSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun getProfile() {
        viewModelScope.launch {
            authRepository.getProfile().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = resource.data,
                        errorMessage = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resource.message
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
                        errorMessage = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUpdateSuccess = false,
                        errorMessage = resource.message
                    )
                }
            }
        }
    }

    fun resetUpdateState() {
        _uiState.value = _uiState.value.copy(isUpdateSuccess = false, errorMessage = null)
    }
}