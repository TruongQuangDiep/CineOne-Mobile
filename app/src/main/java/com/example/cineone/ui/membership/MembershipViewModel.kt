package com.example.cineone.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.membership.MyMembershipDto
import com.example.cineone.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MembershipUiState(
    val isLoading: Boolean = false,
    val membershipData: MyMembershipDto? = null,
    val showTiersBottomSheet: Boolean = false,
    val errorMessage: String? = null
)

class MembershipViewModel(
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembershipUiState())
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    init {
        loadMembership()
    }

    fun loadMembership() {
        viewModelScope.launch {
            membershipRepository.getMyMembership().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(isLoading = false, membershipData = resource.data)
                    is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = resource.message)
                }
            }
        }
    }

    fun toggleTiersBottomSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTiersBottomSheet = show)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                MembershipViewModel(
                    membershipRepository = app.repositoryModule.membershipRepository
                )
            }
        }
    }
}