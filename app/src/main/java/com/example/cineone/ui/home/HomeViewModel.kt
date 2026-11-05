package com.example.cineone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.core.util.CurrencyUtils
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.data.repository.CinemaRepositoryImpl
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.Voucher
import com.example.cineone.domain.repository.VoucherRepository
import com.example.cineone.domain.usecase.GetCinemasUseCase
import com.example.cineone.domain.usecase.GetMoviesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeMovieTab(val label: String, val statusCode: String) {
    NOW_SHOWING("Đang chiếu", "NOW_SHOWING"),
    SNEAK_SHOW("Đặc biệt", "SNEAK_SHOW"),
    COMING_SOON("Sắp chiếu", "COMING_SOON")
}

data class HomePromotionItem(
    val id: Long,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val validPeriod: String
)

data class HomeFeaturedReviewItem(
    val reviewId: Long,
    val movieId: Long,
    val movieTitle: String,
    val reviewerName: String,
    val reviewDate: String,
    val rating: Int,
    val maxRating: Int = 5,
    val content: String
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val isTabLoading: Boolean = false,
    val selectedTab: HomeMovieTab = HomeMovieTab.NOW_SHOWING,
    val searchQuery: String = "",
    val tabMovies: List<Movie> = emptyList(),
    val featuredMovies: List<Movie> = emptyList(),
    val promotions: List<HomePromotionItem> = emptyList(),
    val featuredReviews: List<HomeFeaturedReviewItem> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val getMoviesUseCase: GetMoviesUseCase,
    private val voucherRepository: VoucherRepository? = null,
    private val getCinemasUseCase: GetCinemasUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var tabLoadJob: Job? = null

    init {
        loadHomeData()
        prewarmCinemasInBackground()
    }

    fun loadHomeData() {
        loadFeaturedMovies(_uiState.value.searchQuery.ifBlank { null })
        loadMoviesByTab(_uiState.value.selectedTab, _uiState.value.searchQuery)
        loadPromotions()
    }

    private fun prewarmCinemasInBackground() {
        val cinemasUseCase = getCinemasUseCase ?: return
        if (CinemaRepositoryImpl.isFullListFresh()) return
        viewModelScope.launch(Dispatchers.IO) {
            var page = 0
            while (page < 10) {
                var count = 0
                var hadError = false
                cinemasUseCase(q = null, city = null, status = "ACTIVE", page = page).collect { res ->
                    when (res) {
                        is Resource.Success -> count = res.data?.size ?: 0
                        is Resource.Error -> hadError = true
                        is Resource.Loading -> Unit
                    }
                }
                if (hadError) break
                if (count < 10) {
                    CinemaRepositoryImpl.markFullCatalogLoaded()
                    break
                }
                page++
            }
        }
    }

    fun onTabSelected(tab: HomeMovieTab) {
        if (_uiState.value.selectedTab == tab && _uiState.value.tabMovies.isNotEmpty()) return
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadMoviesByTab(tab, _uiState.value.searchQuery)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadMoviesByTab(_uiState.value.selectedTab, query)
    }

    fun submitSearch() {
        loadMoviesByTab(_uiState.value.selectedTab, _uiState.value.searchQuery)
        loadFeaturedMovies(_uiState.value.searchQuery)
    }

    private fun loadMoviesByTab(tab: HomeMovieTab, query: String) {
        tabLoadJob?.cancel()
        tabLoadJob = viewModelScope.launch {
            getMoviesUseCase(
                q = query.trim().ifBlank { null },
                status = tab.statusCode,
                page = 0,
                size = 20
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isTabLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        val movies = resource.data ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            isTabLoading = false,
                            isLoading = false,
                            tabMovies = movies,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isTabLoading = false,
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải danh sách phim"
                        )
                    }
                }
            }
        }
    }

    private fun loadFeaturedMovies(query: String? = null) {
        viewModelScope.launch {
            getMoviesUseCase(
                q = query?.trim()?.ifBlank { null },
                status = null,
                page = 0,
                size = 20
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_uiState.value.featuredMovies.isEmpty()) {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                    }
                    is Resource.Success -> {
                        val allMovies = resource.data ?: emptyList()
                        val sortedFeatured = allMovies.sortedWith(
                            compareByDescending<Movie> { it.rating }
                                .thenBy { it.status != "NOW_SHOWING" }
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            featuredMovies = sortedFeatured,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải phim nổi bật"
                        )
                    }
                }
            }
        }
    }

    private fun loadPromotions() {
        val repo = voucherRepository ?: return
        viewModelScope.launch {
            repo.getMyVouchers("AVAILABLE").collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val vouchers: List<Voucher> = resource.data ?: emptyList()
                        val mapped = vouchers.map { v -> mapVoucherToPromotion(v) }
                        _uiState.value = _uiState.value.copy(promotions = mapped)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(promotions = emptyList())
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private fun mapVoucherToPromotion(v: Voucher): HomePromotionItem {
        val isPercent = v.discountType.equals("PERCENT", ignoreCase = true)
        val badgeText = when {
            isPercent && v.discountAmount > 0 -> "GIẢM ${v.discountAmount}%"
            v.discountAmount >= 1000 -> "GIẢM ${v.discountAmount / 1000}K"
            v.discountAmount > 0 -> "GIẢM ${v.discountAmount}Đ"
            else -> v.code
        }
        val subtitleParts = mutableListOf<String>()
        if (v.minOrderAmount > 0) {
            subtitleParts.add("Đơn tối thiểu ${CurrencyUtils.formatVnd(v.minOrderAmount)}")
        } else {
            subtitleParts.add("Áp dụng cho mọi đơn đặt vé")
        }
        if (isPercent && v.maxDiscount != null && v.maxDiscount > 0) {
            subtitleParts.add("Tối đa ${CurrencyUtils.formatVnd(v.maxDiscount)}")
        }
        val endFormatted = v.expiresAt?.take(10)?.let { DateTimeUtils.formatReleaseDate(it) }
        val validPeriod = if (!endFormatted.isNullOrBlank() && endFormatted != "Đang cập nhật") {
            "HSD: $endFormatted"
        } else {
            "Đang áp dụng"
        }
        return HomePromotionItem(
            id = v.voucherId,
            code = v.code,
            title = v.title.ifBlank { "Mã ưu đãi ${v.code}" },
            subtitle = subtitleParts.joinToString(" • "),
            badge = badgeText,
            validPeriod = validPeriod
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                HomeViewModel(
                    getMoviesUseCase = app.useCaseModule.getMoviesUseCase,
                    voucherRepository = app.repositoryModule.voucherRepository,
                    getCinemasUseCase = app.useCaseModule.getCinemasUseCase
                )
            }
        }
    }
}
