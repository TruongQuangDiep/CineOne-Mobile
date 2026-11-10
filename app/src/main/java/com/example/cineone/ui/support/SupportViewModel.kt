package com.example.cineone.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.model.SupportRequest
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.repository.SupportRepository
import com.example.cineone.domain.usecase.CreateSupportUseCase
import com.example.cineone.domain.usecase.GetFaqListUseCase
import com.example.cineone.domain.usecase.GetMySupportRequestsUseCase
import com.example.cineone.domain.usecase.GetTicketsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FaqFilterChip(val label: String, val categoryQuery: String?) {
    ALL("Tất cả", null),
    FAQ("FAQ", "FAQ"),
    GUIDE("Hướng dẫn", "HƯỚNG DẪN"),
    POLICY("Chính sách", "CHÍNH SÁCH")
}

data class SupportUiState(
    val myRequests: List<SupportRequest> = emptyList(),
    val isRequestsLoading: Boolean = false,
    val requestsError: String? = null,
    val selectedStatusFilter: String = "ALL",
    val selectedRequestDetail: SupportRequest? = null,
    val formType: String = "HỖ TRỢ",
    val formCategory: String = "THANH TOÁN",
    val formBookingId: Long? = null,
    val formBookingCode: String = "",
    val formContent: String = "",
    val userTickets: List<Ticket> = emptyList(),
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitErrorMessage: String? = null,
    val faqItems: List<FaqItem> = emptyList(),
    val isFaqLoading: Boolean = false,
    val faqError: String? = null,
    val faqSearchQuery: String = "",
    val selectedFaqChip: FaqFilterChip = FaqFilterChip.ALL
)

class SupportViewModel(
    private val supportRepository: SupportRepository,
    private val createSupportUseCase: CreateSupportUseCase,
    private val getMySupportRequestsUseCase: GetMySupportRequestsUseCase,
    private val getFaqListUseCase: GetFaqListUseCase,
    private val getTicketsUseCase: GetTicketsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    private var faqFetchJob: Job? = null

    init {
        loadFaqList()
        viewModelScope.launch {
            supportUpdatedEvent.collect {
                loadMyRequests()
            }
        }
    }

    fun loadMyRequests() {
        val isLoggedIn = CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        if (!isLoggedIn) {
            _uiState.update {
                it.copy(
                    isRequestsLoading = false,
                    myRequests = emptyList(),
                    requestsError = null
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRequestsLoading = true, requestsError = null) }
            getMySupportRequestsUseCase().collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isRequestsLoading = false,
                                myRequests = res.data ?: emptyList(),
                                requestsError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isRequestsLoading = false,
                                requestsError = res.message ?: "Không thể tải danh sách yêu cầu hỗ trợ."
                            )
                        }
                    }
                }
            }
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun selectRequestDetail(request: SupportRequest?) {
        _uiState.update { it.copy(selectedRequestDetail = request) }
        if (request != null && request.requestId > 0L) {
            viewModelScope.launch {
                supportRepository.getSupportDetail(request.requestId).collect { res ->
                    if (res is Resource.Success) {
                        _uiState.update { it.copy(selectedRequestDetail = res.data) }
                    }
                }
            }
        }
    }

    fun loadUserTicketsForPicker() {
        val isLoggedIn = CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        if (!isLoggedIn) return
        viewModelScope.launch {
            val combined = mutableListOf<Ticket>()
            getTicketsUseCase("UPCOMING").collect { res ->
                if (res is Resource.Success) {
                    combined.addAll(res.data)
                }
            }
            getTicketsUseCase("HISTORY").collect { res ->
                if (res is Resource.Success) {
                    combined.addAll(res.data)
                }
            }
            val unique = combined.distinctBy { it.ticketCode.ifBlank { it.ticketId.toString() } }
            _uiState.update { it.copy(userTickets = unique) }
        }
    }

    fun setFormType(type: String) {
        _uiState.update { it.copy(formType = type, submitErrorMessage = null) }
    }

    fun setFormCategory(category: String) {
        _uiState.update { it.copy(formCategory = category, submitErrorMessage = null) }
    }

    fun setFormBookingCode(code: String, bookingId: Long? = null) {
        _uiState.update {
            it.copy(
                formBookingCode = code,
                formBookingId = bookingId,
                submitErrorMessage = null
            )
        }
    }

    fun setFormContent(content: String) {
        if (content.length <= 1000) {
            _uiState.update { it.copy(formContent = content, submitErrorMessage = null) }
        }
    }

    fun resetForm(initialBookingCode: String = "") {
        _uiState.update {
            it.copy(
                formType = "HỖ TRỢ",
                formCategory = "THANH TOÁN",
                formBookingId = null,
                formBookingCode = initialBookingCode,
                formContent = "",
                isSubmitting = false,
                submitSuccess = false,
                submitErrorMessage = null
            )
        }
    }

    fun submitSupportRequest(onSuccess: (SupportRequest) -> Unit = {}) {
        val state = _uiState.value
        val content = state.formContent.trim()
        if (content.isBlank()) {
            _uiState.update { it.copy(submitErrorMessage = "Vui lòng nhập nội dung chi tiết cần hỗ trợ / khiếu nại.") }
            return
        }
        if (content.length < 10) {
            _uiState.update { it.copy(submitErrorMessage = "Nội dung quá ngắn, vui lòng mô tả chi tiết ít nhất 10 ký tự.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null, submitSuccess = false) }
            createSupportUseCase(
                type = state.formType,
                category = state.formCategory,
                content = content,
                bookingId = state.formBookingId,
                bookingCode = state.formBookingCode.trim().takeIf { it.isNotBlank() }
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val created = res.data
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitSuccess = true,
                                submitErrorMessage = null,
                                myRequests = listOf(created) + it.myRequests.filterNot { item -> item.requestId == created.requestId }
                            )
                        }
                        supportUpdatedEvent.tryEmit(Unit)
                        onSuccess(created)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitSuccess = false,
                                submitErrorMessage = res.message ?: "Gửi yêu cầu thất bại, vui lòng thử lại."
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissSubmitSuccess() {
        _uiState.update { it.copy(submitSuccess = false) }
    }

    fun onFaqSearchQueryChange(query: String) {
        _uiState.update { it.copy(faqSearchQuery = query) }
        faqFetchJob?.cancel()
        faqFetchJob = viewModelScope.launch {
            delay(250L)
            fetchFaqsFromServer()
        }
    }

    fun onFaqChipSelected(chip: FaqFilterChip) {
        _uiState.update { it.copy(selectedFaqChip = chip) }
        faqFetchJob?.cancel()
        faqFetchJob = viewModelScope.launch {
            fetchFaqsFromServer()
        }
    }

    fun loadFaqList() {
        faqFetchJob?.cancel()
        faqFetchJob = viewModelScope.launch {
            fetchFaqsFromServer()
        }
    }

    private suspend fun fetchFaqsFromServer() {
        val state = _uiState.value
        val categoryParam = state.selectedFaqChip.categoryQuery
        val queryParam = state.faqSearchQuery.trim().takeIf { it.isNotBlank() }

        _uiState.update { it.copy(isFaqLoading = true, faqError = null) }
        getFaqListUseCase(category = categoryParam, q = queryParam).collect { res ->
            when (res) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isFaqLoading = false,
                            faqItems = res.data,
                            faqError = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isFaqLoading = false,
                            faqItems = emptyList(),
                            faqError = res.message ?: "Không thể tải danh sách câu hỏi thường gặp."
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val supportUpdatedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        val TYPE_OPTIONS = listOf("HỖ TRỢ", "KHIẾU NẠI")
        val CATEGORY_OPTIONS = listOf("THANH TOÁN", "VÉ", "ĐÁNH GIÁ", "TÀI KHOẢN", "KHÁC")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                SupportViewModel(
                    supportRepository = app.repositoryModule.supportRepository,
                    createSupportUseCase = app.useCaseModule.createSupportUseCase,
                    getMySupportRequestsUseCase = app.useCaseModule.getMySupportRequestsUseCase,
                    getFaqListUseCase = app.useCaseModule.getFaqListUseCase,
                    getTicketsUseCase = app.useCaseModule.getTicketsUseCase
                )
            }
        }
    }
}
