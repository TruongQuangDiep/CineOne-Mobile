package com.example.cineone.ui.booking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Booking
import com.example.cineone.domain.model.Voucher
import com.example.cineone.domain.repository.BookingRepository
import com.example.cineone.domain.repository.VoucherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingUiState(
    val isLoading: Boolean = false,
    val showtimeId: Long = 0L,
    val movieTitle: String = "",
    val cinemaName: String = "",
    val startTime: String = "",
    val seatNames: String = "",
    val seatPrice: Long = 0L,
    val foodPrice: Long = 0L,
    val foodSummary: String = "",
    val availableVouchers: List<Voucher> = emptyList(),
    val selectedVoucher: Voucher? = null,
    val voucherCodeInput: String = "",
    val discountAmount: Long = 0L,
    val createdBooking: Booking? = null,
    val errorMessage: String? = null,
    val voucherMessage: String? = null
) {
    val subtotal: Long get() = seatPrice + foodPrice
    val finalAmount: Long get() = (subtotal - discountAmount).coerceAtLeast(0L)
}

class BookingViewModel(
    private val bookingRepository: BookingRepository,
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun initData(
        showtimeId: Long,
        movieTitle: String,
        cinemaName: String,
        startTime: String,
        seatNames: String,
        seatPrice: Long,
        foodPrice: Long,
        foodSummary: String
    ) {
        _uiState.value = _uiState.value.copy(
            showtimeId = showtimeId,
            movieTitle = movieTitle,
            cinemaName = cinemaName,
            startTime = startTime,
            seatNames = seatNames,
            seatPrice = seatPrice,
            foodPrice = foodPrice,
            foodSummary = foodSummary
        )
        loadVouchers()

        // Tự động nạp mã voucher nếu trước đó người dùng đã bấm "Sử dụng" từ Ví Voucher
        val savedVoucher = CineOneApp.instance
            .getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
            .getString("selected_voucher_code", null)

        if (!savedVoucher.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(voucherCodeInput = savedVoucher)
            applyVoucher(savedVoucher)
            // Xóa mã tạm sau khi đã tự động điền để không áp dụng cho các lần sau
            CineOneApp.instance
                .getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                .edit()
                .remove("selected_voucher_code")
                .apply()
        }
    }

    fun onVoucherCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(voucherCodeInput = code, voucherMessage = null)
    }

    private fun loadVouchers() {
        viewModelScope.launch {
            voucherRepository.getMyVouchers("AVAILABLE").collect { res ->
                if (res is Resource.Success) {
                    _uiState.value = _uiState.value.copy(availableVouchers = res.data ?: emptyList())
                }
            }
        }
    }

    fun applyVoucher(code: String) {
        if (code.isBlank()) return
        val currentSubtotal = _uiState.value.subtotal

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, voucherMessage = null)
            voucherRepository.applyVoucher(code.trim().uppercase(), currentSubtotal).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val discount = res.data ?: 0L
                        val matched = _uiState.value.availableVouchers.find { it.code.equals(code, true) }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            selectedVoucher = matched,
                            discountAmount = discount,
                            voucherMessage = "Áp dụng thành công: -${String.format("%,d", discount)} đ"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = res.message,
                            voucherMessage = res.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun removeVoucher() {
        _uiState.value = _uiState.value.copy(
            selectedVoucher = null,
            voucherCodeInput = "",
            discountAmount = 0L,
            voucherMessage = null
        )
    }

    fun confirmBooking(onSuccess: (Booking) -> Unit) {
        val state = _uiState.value
        val context = CineOneApp.instance
        val seatIdsString = context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
            .getString("selected_seat_ids", "") ?: ""
        val realSeatIds = seatIdsString.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            bookingRepository.createBooking(
                showtimeId = state.showtimeId,
                seatIds = realSeatIds, // <-- GỬI SEAT IDS THẬT LÊN SERVER
                subtotal = state.subtotal,
                discountAmount = state.discountAmount,
                totalAmount = state.finalAmount,
                voucherCode = state.selectedVoucher?.code ?: state.voucherCodeInput.ifBlank { null }
            ).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, createdBooking = res.data)
                        res.data?.let { onSuccess(it) }
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                BookingViewModel(
                    bookingRepository = app.repositoryModule.bookingRepository,
                    voucherRepository = app.repositoryModule.voucherRepository
                )
            }
        }
    }
}