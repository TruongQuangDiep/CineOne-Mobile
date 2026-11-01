package com.example.cineone.ui.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.model.TicketDetail
import com.example.cineone.domain.repository.TicketRepository
import com.example.cineone.domain.usecase.GetTicketsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TicketUiState(
    val isLoading: Boolean = false,
    val tickets: List<Ticket> = emptyList(),
    val currentTab: String = "UPCOMING",
    val selectedTicketDetail: TicketDetail? = null,
    val errorMessage: String? = null
)

class TicketViewModel(
    private val getTicketsUseCase: GetTicketsUseCase,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    init {
        loadTickets("UPCOMING")
    }

    fun switchTab(tab: String) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        loadTickets(tab)
    }

    fun loadTickets(tab: String = _uiState.value.currentTab) {
        viewModelScope.launch {
            getTicketsUseCase(tab).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            tickets = resource.data,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                }
            }
        }
    }

    fun getTicketDetail(ticketId: Long) {
        viewModelScope.launch {
            ticketRepository.getTicketDetail(ticketId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedTicketDetail = resource.data
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resource.message
                    )
                }
            }
        }
    }
}