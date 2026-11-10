package com.example.cineone.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.ai.ReviewSummaryResponseDto
import com.example.cineone.data.remote.ai.AiApi
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.MovieReviewsData
import com.example.cineone.domain.model.Review
import com.example.cineone.domain.model.ReviewEligibility
import com.example.cineone.domain.repository.ReviewRepository
import com.example.cineone.domain.usecase.CreateReviewUseCase
import com.example.cineone.domain.usecase.GetMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReviewTab(val label: String) {
    COMMUNITY("Cộng đồng"),
    MY_REVIEWS("Đánh giá của tôi")
}

data class ReviewUiState(
    val activeTab: ReviewTab = ReviewTab.COMMUNITY,
    val searchQuery: String = "",
    val movies: List<Movie> = emptyList(),
    val isMoviesLoading: Boolean = false,
    val moviesError: String? = null,
    val myReviews: List<Review> = emptyList(),
    val isMyReviewsLoading: Boolean = false,
    val myReviewsError: String? = null,
    val selectedMovie: Movie? = null,
    val movieReviewsData: MovieReviewsData? = null,
    val isMovieReviewsLoading: Boolean = false,
    val movieReviewsError: String? = null,
    val aiSummary: ReviewSummaryResponseDto? = null,
    val isAiSummaryLoading: Boolean = false,
    val aiSummaryError: String? = null,
    val eligibility: ReviewEligibility? = null,
    val isCheckingEligibility: Boolean = false,
    val formRating: Int = 5,
    val formContent: String = "",
    val isSubmittingReview: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitErrorMessage: String? = null
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val createReviewUseCase: CreateReviewUseCase,
    private val getMoviesUseCase: GetMoviesUseCase,
    private val aiApi: AiApi? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
        loadMyReviews()
        viewModelScope.launch {
            reviewUpdateEvent.collect { updatedMovieId ->
                aiSummaryCache.remove(updatedMovieId)
                loadMovies(_uiState.value.searchQuery)
                loadMyReviews()
                if (_uiState.value.selectedMovie?.movieId == updatedMovieId) {
                    _uiState.update { it.copy(isAiSummaryLoading = true, aiSummary = null) }
                    loadMovieReviews(updatedMovieId)
                    loadAiSummary(updatedMovieId, forceRefresh = true)
                    checkEligibility(updatedMovieId)
                }
            }
        }
    }

    fun setTab(tab: ReviewTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == ReviewTab.MY_REVIEWS) {
            loadMyReviews()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadMovies(query)
    }

    fun loadMovies(query: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMoviesLoading = true, moviesError = null) }
            getMoviesUseCase(
                q = query?.trim()?.ifBlank { null },
                status = null,
                page = 0,
                size = 50
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val list = res.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                isMoviesLoading = false,
                                movies = list,
                                moviesError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isMoviesLoading = false,
                                moviesError = res.message ?: "Không thể tải danh sách phim"
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMyReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMyReviewsLoading = true, myReviewsError = null) }
            reviewRepository.getMyReviews().collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isMyReviewsLoading = false,
                                myReviews = res.data ?: emptyList(),
                                myReviewsError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isMyReviewsLoading = false,
                                myReviewsError = res.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectMovie(movie: Movie) {
        val cachedSummary = aiSummaryCache[movie.movieId]
        _uiState.update {
            it.copy(
                selectedMovie = movie,
                movieReviewsData = null,
                aiSummary = cachedSummary,
                eligibility = null
            )
        }
        loadMovieReviews(movie.movieId)
        if (cachedSummary == null) {
            loadAiSummary(movie.movieId)
        }
        checkEligibility(movie.movieId)
    }

    fun selectMovieById(movieId: Long) {
        val found = _uiState.value.movies.find { it.movieId == movieId }
        if (found != null) {
            selectMovie(found)
        } else {
            val cachedSummary = aiSummaryCache[movieId]
            _uiState.update { it.copy(aiSummary = cachedSummary) }
            loadMovieReviews(movieId)
            if (cachedSummary == null) {
                loadAiSummary(movieId)
            }
            checkEligibility(movieId)
        }
    }

    fun clearSelectedMovie() {
        _uiState.update {
            it.copy(
                selectedMovie = null,
                movieReviewsData = null,
                aiSummary = null,
                aiSummaryError = null,
                eligibility = null
            )
        }
    }

    fun loadMovieReviews(movieId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMovieReviewsLoading = true, movieReviewsError = null) }
            reviewRepository.getMovieReviews(movieId).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isMovieReviewsLoading = false,
                                movieReviewsData = res.data,
                                movieReviewsError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isMovieReviewsLoading = false,
                                movieReviewsError = res.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadAiSummary(movieId: Long, forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            val cached = aiSummaryCache[movieId]
            if (cached != null) {
                _uiState.update { it.copy(isAiSummaryLoading = false, aiSummary = cached, aiSummaryError = null) }
                return
            }
        }
        val ai = aiApi ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiSummaryLoading = true, aiSummaryError = null) }
            try {
                val response = ai.getMovieReviewSummary(movieId)
                if (response.isSuccess && response.data != null) {
                    val summaryData = response.data
                    aiSummaryCache[movieId] = summaryData
                    _uiState.update {
                        it.copy(
                            isAiSummaryLoading = false,
                            aiSummary = summaryData,
                            aiSummaryError = null
                        )
                    }
                } else {
                    val err = response.message?.ifBlank { null }
                        ?: "Dịch vụ AI đang bận hoặc đạt giới hạn lượt gọi (Rate Limit). Vui lòng bấm 🔄 để thử lại."
                    _uiState.update { it.copy(isAiSummaryLoading = false, aiSummaryError = err) }
                }
            } catch (e: Exception) {
                val err = e.message?.ifBlank { null }
                    ?: "Không thể kết nối đến dịch vụ AI. Vui lòng bấm 🔄 để thử lại."
                _uiState.update { it.copy(isAiSummaryLoading = false, aiSummaryError = err) }
            }
        }
    }

    fun refreshAiSummary(movieId: Long) {
        aiSummaryCache.remove(movieId)
        _uiState.update { it.copy(isAiSummaryLoading = true) }
        loadAiSummary(movieId, forceRefresh = true)
    }

    fun checkEligibility(movieId: Long, onResult: ((ReviewEligibility) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingEligibility = true) }
            reviewRepository.checkEligibility(movieId).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val elig = res.data ?: ReviewEligibility(eligible = false, message = "Không xác định")
                        _uiState.update {
                            it.copy(
                                isCheckingEligibility = false,
                                eligibility = elig
                            )
                        }
                        if (elig.existingRating != null) {
                            _uiState.update {
                                it.copy(
                                    formRating = elig.existingRating,
                                    formContent = elig.existingContent ?: ""
                                )
                            }
                        }
                        onResult?.invoke(elig)
                    }
                    is Resource.Error -> {
                        val fallback = ReviewEligibility(
                            eligible = false,
                            message = res.message ?: "Chưa thể kiểm tra điều kiện đánh giá"
                        )
                        _uiState.update {
                            it.copy(
                                isCheckingEligibility = false,
                                eligibility = fallback
                            )
                        }
                        onResult?.invoke(fallback)
                    }
                }
            }
        }
    }

    fun setFormRating(rating: Int) {
        _uiState.update { it.copy(formRating = rating.coerceIn(1, 5)) }
    }

    fun setFormContent(content: String) {
        if (content.length <= 500) {
            _uiState.update { it.copy(formContent = content) }
        }
    }

    fun prefillForm(rating: Int?, content: String?) {
        _uiState.update {
            it.copy(
                formRating = rating?.coerceIn(1, 5) ?: 5,
                formContent = content ?: "",
                submitSuccess = false,
                submitErrorMessage = null
            )
        }
    }

    fun submitReview(movieId: Long, onSuccess: () -> Unit) {
        val state = _uiState.value
        val rating = state.formRating
        val content = state.formContent.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReview = true, submitErrorMessage = null, submitSuccess = false) }
            createReviewUseCase(movieId, rating, content).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSubmittingReview = false,
                                submitSuccess = true,
                                submitErrorMessage = null
                            )
                        }
                        // Xóa cache dùng chung và phát sự kiện cho mọi màn hình đang mở
                        aiSummaryCache.remove(movieId)
                        reviewUpdateEvent.tryEmit(movieId)
                        onSuccess()
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSubmittingReview = false,
                                submitSuccess = false,
                                submitErrorMessage = res.message ?: "Gửi đánh giá thất bại"
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearSubmitStatus() {
        _uiState.update { it.copy(submitSuccess = false, submitErrorMessage = null) }
    }

    companion object {
        // Bộ nhớ đệm và sự kiện dùng chung toàn ứng dụng để đồng bộ giữa ReviewScreen và ReviewFormScreen
        private val aiSummaryCache = java.util.concurrent.ConcurrentHashMap<Long, ReviewSummaryResponseDto>()
        private val reviewUpdateEvent = kotlinx.coroutines.flow.MutableSharedFlow<Long>(extraBufferCapacity = 1)

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                ReviewViewModel(
                    reviewRepository = app.repositoryModule.reviewRepository,
                    createReviewUseCase = app.useCaseModule.createReviewUseCase,
                    getMoviesUseCase = app.useCaseModule.getMoviesUseCase,
                    aiApi = app.networkModule.aiApi
                )
            }
        }
    }
}
