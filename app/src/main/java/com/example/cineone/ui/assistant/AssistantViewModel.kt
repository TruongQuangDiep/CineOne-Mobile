package com.example.cineone.ui.assistant

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.data.repository.CinemaRepositoryImpl
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiMessage
import com.example.cineone.domain.model.AiSender
import com.example.cineone.domain.repository.AuthRepository
import com.example.cineone.domain.usecase.AskAssistantUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class DiscoveryBannerSlide(
    val index: Int,
    val total: Int,
    val badgeText: String,
    val promptDisplay: String,
    val promptToSend: String
)

data class QuickSuggestionItem(
    val id: String,
    val title: String,
    val promptToSend: String
)

data class AssistantUiState(
    val userGreetingName: String = "bạn",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val messages: List<AiMessage> = emptyList(),
    val activeCriteria: AiExtractedCriteria? = null,
    val discoverySlides: List<DiscoveryBannerSlide> = defaultDiscoverySlides(),
    val quickSuggestions: List<QuickSuggestionItem> = defaultQuickSuggestions(),
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val homeLat: Double? = null,
    val homeLon: Double? = null,
    val homeAddress: String = "",
    val errorMessage: String? = null
)

private fun defaultDiscoverySlides(): List<DiscoveryBannerSlide> = listOf(
    DiscoveryBannerSlide(
        index = 1,
        total = 3,
        badgeText = "Khám phá cùng trợ lý CineOne (1/3)",
        promptDisplay = "Tìm suất chiếu\ntối mai gần nhà tôi",
        promptToSend = "Tìm suất chiếu tối mai gần nhà tôi"
    ),
    DiscoveryBannerSlide(
        index = 2,
        total = 3,
        badgeText = "Khám phá cùng trợ lý CineOne (2/3)",
        promptDisplay = "Phim hành động\ncuối tuần gần tôi",
        promptToSend = "Phim hành động cuối tuần gần tôi"
    ),
    DiscoveryBannerSlide(
        index = 3,
        total = 3,
        badgeText = "Khám phá cùng trợ lý CineOne (3/3)",
        promptDisplay = "Quy định đổi vé\nvà quyền lợi VIP",
        promptToSend = "Quy định đổi/ hủy vé và quyền lợi VIP tại CineOne"
    )
)

private fun defaultQuickSuggestions(): List<QuickSuggestionItem> = listOf(
    QuickSuggestionItem(
        id = "quick_horror_beta",
        title = "Phim kinh dị tối mai rạp Beta",
        promptToSend = "Phim kinh dị tối mai rạp Beta"
    ),
    QuickSuggestionItem(
        id = "quick_review_summary",
        title = "Tóm tắt review Quỷ ăn tạng",
        promptToSend = "Tóm tắt review Quỷ ăn tạng"
    )
)

class AssistantViewModel(
    private val askAssistantUseCase: AskAssistantUseCase,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        loadUserProfileGreeting()
    }

    private fun loadUserProfileGreeting() {
        val repo = authRepository ?: return
        val isLoggedIn = runCatching {
            CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        }.getOrDefault(false)

        if (!isLoggedIn) return

        viewModelScope.launch {
            repo.getProfile().collect { res ->
                if (res is Resource.Success) {
                    val profile = res.data ?: return@collect
                    val fullName = profile.fullName.trim()
                    val shortName = if (fullName.isNotBlank()) {
                        fullName.split(" ").lastOrNull()?.ifBlank { fullName } ?: fullName
                    } else {
                        "bạn"
                    }
                    val addr = profile.address?.trim().orEmpty()
                    _uiState.value = _uiState.value.copy(
                        userGreetingName = shortName,
                        homeAddress = addr
                    )
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(
            inputText = newText,
            errorMessage = null
        )
    }

    fun clearConversation() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            activeCriteria = null,
            errorMessage = null
        )
    }

    fun sendPrompt(prompt: String, context: Context? = null) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() || _uiState.value.isLoading) return

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            sender = AiSender.USER,
            content = cleanPrompt,
            timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        )

        val previousCriteria = _uiState.value.activeCriteria
        _uiState.value = _uiState.value.copy(
            inputText = "",
            isLoading = true,
            messages = _uiState.value.messages + userMessage,
            errorMessage = null
        )

        viewModelScope.launch {
            resolveLocationCoordinatesIfNeeded(cleanPrompt, context)

            val stateSnap = _uiState.value
            askAssistantUseCase(
                query = cleanPrompt,
                previousCriteria = previousCriteria,
                currentLat = stateSnap.currentLat,
                currentLon = stateSnap.currentLon,
                homeLat = stateSnap.homeLat,
                homeLon = stateSnap.homeLon,
                homeAddress = stateSnap.homeAddress
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        val reply = resource.data
                        val updatedCriteria = reply.extractedCriteria ?: _uiState.value.activeCriteria
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = _uiState.value.messages + reply,
                            activeCriteria = updatedCriteria,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        val errText = resource.message
                            ?: "Không thể kết nối tới Trợ lý CineOne lúc này. Vui lòng thử lại."
                        val errorBubble = AiMessage(
                            id = UUID.randomUUID().toString(),
                            sender = AiSender.ASSISTANT,
                            content = errText,
                            timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = _uiState.value.messages + errorBubble,
                            errorMessage = errText
                        )
                    }
                }
            }
        }
    }

    private suspend fun resolveLocationCoordinatesIfNeeded(query: String, context: Context?) {
        if (context == null) return
        val normalized = LocationUtils.normalizeSearchText(query)

        val needsGps = normalized.contains("gan toi") || normalized.contains("gan day") ||
            normalized.contains("quanh day") || normalized.contains("hien tai")
        if (needsGps && (_uiState.value.currentLat == null || _uiState.value.currentLon == null)) {
            val coords = withContext(Dispatchers.IO) {
                LocationUtils.fetchDeviceLocation(context)
            }
            if (coords != null) {
                _uiState.value = _uiState.value.copy(
                    currentLat = coords.first,
                    currentLon = coords.second
                )
            }
        }

        val needsHome = normalized.contains("gan nha") || normalized.contains("nha toi")
        if (needsHome && (_uiState.value.homeLat == null || _uiState.value.homeLon == null)) {
            val addr = _uiState.value.homeAddress
            if (addr.isNotBlank()) {
                val cinemas = CinemaRepositoryImpl.getCachedCinemasSnapshot()
                val coords = withContext(Dispatchers.IO) {
                    LocationUtils.resolveAddressCoordinates(context, addr, cinemas)
                }
                if (coords != null) {
                    _uiState.value = _uiState.value.copy(
                        homeLat = coords.first,
                        homeLon = coords.second
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                AssistantViewModel(
                    askAssistantUseCase = app.useCaseModule.askAssistantUseCase,
                    authRepository = app.repositoryModule.authRepository
                )
            }
        }
    }
}
