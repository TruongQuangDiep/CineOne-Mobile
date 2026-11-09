package com.example.cineone.ui.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Voucher
import com.example.cineone.domain.repository.VoucherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoucherUiState(
    val isLoading: Boolean = false,
    val currentTab: String = "AVAILABLE", // AVAILABLE, USED, EXPIRED
    val vouchers: List<Voucher> = emptyList(),
    val selectedVoucher: Voucher? = null,
    val showDetailBottomSheet: Boolean = false,
    val errorMessage: String? = null
)

class VoucherViewModel(
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoucherUiState())
    val uiState: StateFlow<VoucherUiState> = _uiState.asStateFlow()

    init {
        loadVouchers("AVAILABLE")
    }

    fun switchTab(tab: String) {
        if (_uiState.value.currentTab == tab) return
        _uiState.value = _uiState.value.copy(currentTab = tab)
        loadVouchers(tab)
    }

    fun loadVouchers(tab: String = _uiState.value.currentTab) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            voucherRepository.getMyVouchers(tab).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            vouchers = resource.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectVoucherForDetail(voucher: Voucher?) {
        _uiState.value = _uiState.value.copy(
            selectedVoucher = voucher,
            showDetailBottomSheet = voucher != null
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                VoucherViewModel(
                    voucherRepository = app.repositoryModule.voucherRepository
                )
            }
        }
    }
}