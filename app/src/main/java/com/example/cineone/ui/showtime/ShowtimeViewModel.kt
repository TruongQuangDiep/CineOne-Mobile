package com.example.cineone.ui.showtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.usecase.GetShowtimesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DateItem(
    val dateString: String,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val isSelected: Boolean = false
)

data class ShowtimeUiState(
    val isLoading: Boolean = false,
    val dates: List<DateItem> = emptyList(),
    val selectedDate: String = "",
    val showtimesByCinema: Map<String, List<Showtime>> = emptyMap(),
    val errorMessage: String? = null
)

class ShowtimeViewModel(
    private val getShowtimesUseCase: GetShowtimesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowtimeUiState())
    val uiState: StateFlow<ShowtimeUiState> = _uiState.asStateFlow()

    init {
        generateUpcomingDates()
    }

    private fun generateUpcomingDates() {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val displayFormatter = DateTimeFormatter.ofPattern("dd/MM")

        val dateList = (0..6).map { offset ->
            val d = today.plusDays(offset.toLong())
            val dateStr = d.format(dateFormatter)
            val dayOfWeekStr = when (offset) {
                0 -> "Hôm nay"
                else -> when (d.dayOfWeek.value) {
                    1 -> "T.2"
                    2 -> "T.3"
                    3 -> "T.4"
                    4 -> "T.5"
                    5 -> "T.6"
                    6 -> "T.7"
                    else -> "CN"
                }
            }
            DateItem(
                dateString = dateStr,
                dayOfWeek = dayOfWeekStr,
                dayOfMonth = d.format(displayFormatter),
                isSelected = (offset == 0)
            )
        }

        val firstDate = dateList.firstOrNull()?.dateString ?: ""
        _uiState.value = _uiState.value.copy(
            dates = dateList,
            selectedDate = firstDate
        )
    }

    fun onDateSelected(movieId: Long, selectedDateStr: String) {
        val updatedDates = _uiState.value.dates.map {
            it.copy(isSelected = (it.dateString == selectedDateStr))
        }
        _uiState.value = _uiState.value.copy(
            dates = updatedDates,
            selectedDate = selectedDateStr
        )
        loadShowtimes(movieId = movieId, date = selectedDateStr)
    }

    fun loadShowtimes(movieId: Long, date: String? = null) {
        val queryDate = date ?: _uiState.value.selectedDate
        viewModelScope.launch {
            getShowtimesUseCase(movieId = movieId, date = queryDate).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        val showtimes = resource.data ?: emptyList()

                        val grouped = showtimes.groupBy { it.cinemaName.ifBlank { "Rạp CineOne" } }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showtimesByCinema = grouped,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải danh sách suất chiếu"
                        )
                    }
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                ShowtimeViewModel(
                    getShowtimesUseCase = app.useCaseModule.getShowtimesUseCase
                )
            }
        }
    }
}