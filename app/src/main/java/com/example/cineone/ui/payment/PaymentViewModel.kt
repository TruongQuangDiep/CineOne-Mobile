package com.example.cineone.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.repository.PaymentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isLoading: Boolean = false,
    val bookingId: Long = 0L,
    val bookingCode: String = "",
    val amount: Long = 0L,
    val movieTitle: String = "",
    val qrCode: String = "",
    val checkoutUrl: String = "",
    val remainingSeconds: Int = 600, // 10 phút giữ ghế
    val isExpired: Boolean = false,
    val isPaid: Boolean = false,
    val errorMessage: String? = null
)

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var pollingJob: Job? = null

    fun initPayment(bookingId: Long, bookingCode: String, amount: Long, movieTitle: String) {
        _uiState.value = _uiState.value.copy(
            bookingId = bookingId,
            bookingCode = bookingCode,
            amount = amount,
            movieTitle = movieTitle,
            isLoading = true
        )

        loadPaymentLink(bookingId)
        startCountdownTimer()
        startPaymentStatusPolling(bookingId)
    }

    private fun loadPaymentLink(bookingId: Long) {
        viewModelScope.launch {
            paymentRepository.createPaymentLink(bookingId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val data = res.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            qrCode = data?.qrCode ?: "",
                            checkoutUrl = data?.checkoutUrl ?: ""
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = res.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _uiState.value.remainingSeconds > 0 && !_uiState.value.isPaid) {
                delay(1000L)
                val newSeconds = _uiState.value.remainingSeconds - 1
                if (newSeconds <= 0) {
                    _uiState.value = _uiState.value.copy(remainingSeconds = 0, isExpired = true)
                    pollingJob?.cancel()
                    break
                } else {
                    _uiState.value = _uiState.value.copy(remainingSeconds = newSeconds)
                }
            }
        }
    }

    private fun startPaymentStatusPolling(bookingId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && !_uiState.value.isPaid && !_uiState.value.isExpired) {
                delay(2000L) // Polling mỗi 2 giây
                paymentRepository.getPaymentStatus(bookingId).collect { res ->
                    if (res is Resource.Success) {
                        val status = res.data
                        if (status?.isPaid == true || "PAID".equals(status?.status, true) || "CONFIRMED".equals(status?.bookingStatus, true)) {
                            _uiState.value = _uiState.value.copy(isPaid = true)
                            countdownJob?.cancel()
                        }
                    }
                }
            }
        }
    }

    fun checkStatusManually(onResult: ((Boolean, String) -> Unit)? = null) {
        val bookingId = _uiState.value.bookingId
        if (bookingId <= 0) {
            onResult?.invoke(false, "Không tìm thấy thông tin đơn hàng")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            paymentRepository.getPaymentStatus(bookingId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        val isPaid = res.data?.isPaid == true || "PAID".equals(res.data?.status, true) || "CONFIRMED".equals(res.data?.bookingStatus, true)
                        if (isPaid) {
                            _uiState.value = _uiState.value.copy(isPaid = true)
                            countdownJob?.cancel()
                            onResult?.invoke(true, "Thanh toán thành công")
                        } else {
                            onResult?.invoke(false, "Hệ thống chưa nhận được tiền thanh toán. Vui lòng thử lại sau ít phút.")
                        }
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onResult?.invoke(false, res.message ?: "Chưa nhận được giao dịch")
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        pollingJob?.cancel()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                PaymentViewModel(
                    paymentRepository = app.repositoryModule.paymentRepository
                )
            }
        }
    }
}
