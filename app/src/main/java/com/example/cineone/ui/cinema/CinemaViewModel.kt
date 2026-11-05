package com.example.cineone.ui.cinema

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.data.repository.CinemaRepositoryImpl
import com.example.cineone.data.repository.MovieRepositoryImpl
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.AuthRepository
import com.example.cineone.domain.usecase.GetCinemasUseCase
import com.example.cineone.domain.usecase.GetMoviesUseCase
import com.example.cineone.domain.usecase.GetShowtimesUseCase
import com.example.cineone.ui.movie.CinemaWithDistance
import com.example.cineone.ui.movie.MovieViewModel
import com.example.cineone.ui.showtime.DateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CinemaMovieShowtimeItem(
    val movie: Movie,
    val showtimes: List<Showtime>
)

data class CinemaListUiState(
    val isLoading: Boolean = false,
    val allCinemas: List<Cinema> = emptyList(),
    val availableBrands: List<String> = emptyList(),
    val selectedBrand: String = "Đề xuất",
    val availableCities: List<String> = emptyList(),
    val locationMode: LocationFilterMode = LocationFilterMode.NONE,
    val selectedCity: String = "",
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val homeLat: Double? = null,
    val homeLon: Double? = null,
    val homeAddress: String = "",
    val locationStatusMessage: String? = null,
    val filteredCinemas: List<CinemaWithDistance> = emptyList(),
    val errorMessage: String? = null
)

data class CinemaDetailUiState(
    val isLoading: Boolean = false,
    val cinema: Cinema? = null,
    val dates: List<DateItem> = emptyList(),
    val selectedDate: String = "",
    val selectedTimeSlot: TimeSlotFilter = TimeSlotFilter.ALL,
    val movieShowtimes: List<CinemaMovieShowtimeItem> = emptyList(),
    val errorMessage: String? = null
)

class CinemaViewModel(
    private val getCinemasUseCase: GetCinemasUseCase,
    private val getShowtimesUseCase: GetShowtimesUseCase,
    private val getMoviesUseCase: GetMoviesUseCase,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _listUiState = MutableStateFlow(CinemaListUiState())
    val listUiState: StateFlow<CinemaListUiState> = _listUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(CinemaDetailUiState())
    val detailUiState: StateFlow<CinemaDetailUiState> = _detailUiState.asStateFlow()

    private var rawCinemaShowtimes: List<Showtime> = emptyList()
    private var cachedMoviesById: Map<Long, Movie> = emptyMap()
    private var detailJob: Job? = null

    fun loadCinemasIfNeeded() {
        if (_listUiState.value.allCinemas.isNotEmpty() || _listUiState.value.isLoading) return

        // 1. Lấy ngay từ RAM / Disk cache để hiển thị tức thì 0ms (không phải đợi loading)
        val cachedSnapshot = CinemaRepositoryImpl.getCachedCinemasSnapshot()
        if (cachedSnapshot.isNotEmpty()) {
            applyCinemasToState(cachedSnapshot)
            if (CinemaRepositoryImpl.isFullListFresh()) {
                return
            }
        }
        loadAllCinemas()
    }

    fun loadAllCinemas() {
        viewModelScope.launch {
            if (_listUiState.value.allCinemas.isEmpty()) {
                _listUiState.value = _listUiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            val combined = _listUiState.value.allCinemas.toMutableList()
            var lastError: String? = null
            var page = 0
            while (page < 10) {
                var pageItems: List<Cinema> = emptyList()
                var hadError = false
                getCinemasUseCase(q = null, city = null, status = "ACTIVE", page = page).collect { res ->
                    when (res) {
                        is Resource.Success -> pageItems = res.data.orEmpty()
                        is Resource.Error -> {
                            lastError = res.message
                            hadError = true
                        }
                        is Resource.Loading -> Unit
                    }
                }
                if (hadError) break
                if (pageItems.isEmpty()) {
                    CinemaRepositoryImpl.markFullCatalogLoaded()
                    break
                }
                combined.addAll(pageItems)
                val uniqueCinemas = combined.distinctBy { it.cinemaId }

                // Hiển thị ngay lập tức khi có trang 0, các trang sau cập nhật nối tiếp mượt mà
                applyCinemasToState(uniqueCinemas)

                if (pageItems.size < 10) {
                    CinemaRepositoryImpl.markFullCatalogLoaded()
                    break
                }
                page++
            }

            if (_listUiState.value.allCinemas.isEmpty()) {
                _listUiState.value = _listUiState.value.copy(
                    isLoading = false,
                    errorMessage = lastError ?: "Không tải được danh sách rạp"
                )
            }
        }
    }

    private fun applyCinemasToState(uniqueCinemas: List<Cinema>) {
        val brands = buildBrandList(uniqueCinemas)
        val cities = uniqueCinemas
            .map { it.city.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        _listUiState.value = _listUiState.value.copy(
            isLoading = false,
            allCinemas = uniqueCinemas,
            availableBrands = brands,
            availableCities = cities,
            errorMessage = null
        )
        recomputeCinemaList()
    }

    fun selectBrand(brand: String) {
        _listUiState.value = _listUiState.value.copy(selectedBrand = brand)
        recomputeCinemaList()
    }

    fun clearLocationFilter() {
        _listUiState.value = _listUiState.value.copy(
            locationMode = LocationFilterMode.NONE,
            selectedCity = "",
            locationStatusMessage = null
        )
        recomputeCinemaList()
    }

    fun selectCityFilter(city: String) {
        val clean = city.trim()
        if (clean.isBlank()) {
            clearLocationFilter()
            return
        }
        _listUiState.value = _listUiState.value.copy(
            locationMode = LocationFilterMode.PROVINCE,
            selectedCity = clean,
            locationStatusMessage = "Tỉnh thành: $clean"
        )
        recomputeCinemaList()
    }

    fun activateCurrentLocation(context: Context) {
        viewModelScope.launch {
            val coords = withContext(Dispatchers.IO) {
                LocationUtils.fetchDeviceLocation(context)
            }
            val fallback = _listUiState.value.allCinemas.firstOrNull {
                it.latitude != null && it.longitude != null
            }
            val lat = coords?.first ?: fallback?.latitude
            val lon = coords?.second ?: fallback?.longitude

            if (lat != null && lon != null) {
                _listUiState.value = _listUiState.value.copy(
                    locationMode = LocationFilterMode.CURRENT_LOCATION,
                    selectedCity = "",
                    currentLat = lat,
                    currentLon = lon,
                    locationStatusMessage = "Đang sắp xếp theo Vị trí gần tôi"
                )
            } else {
                _listUiState.value = _listUiState.value.copy(
                    locationMode = LocationFilterMode.NONE,
                    locationStatusMessage = "Không thể xác định tọa độ GPS hiện tại"
                )
            }
            recomputeCinemaList()
        }
    }

    fun onLocationPermissionDenied() {
        _listUiState.value = _listUiState.value.copy(
            locationMode = LocationFilterMode.NONE,
            locationStatusMessage = "Vui lòng cấp quyền GPS để lọc rạp gần bạn"
        )
    }

    fun activateHomeLocation(context: Context) {
        val repo = authRepository
        val isLoggedIn = runCatching {
            CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        }.getOrDefault(false)

        if (repo == null || !isLoggedIn) {
            _listUiState.value = _listUiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                locationStatusMessage = "Vui lòng đăng nhập để sử dụng Vị trí gần nhà"
            )
            return
        }

        viewModelScope.launch {
            repo.getProfile().collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val addr = res.data?.address?.trim().orEmpty()
                        if (addr.isBlank()) {
                            _listUiState.value = _listUiState.value.copy(
                                locationMode = LocationFilterMode.NONE,
                                locationStatusMessage = "Chưa có địa chỉ nhà trong hồ sơ cá nhân"
                            )
                        } else {
                            val coords = withContext(Dispatchers.IO) {
                                LocationUtils.resolveAddressCoordinates(
                                    context = context,
                                    address = addr,
                                    cinemas = _listUiState.value.allCinemas
                                )
                            }
                            _listUiState.value = _listUiState.value.copy(
                                locationMode = LocationFilterMode.HOME_LOCATION,
                                selectedCity = "",
                                homeAddress = addr,
                                homeLat = coords?.first,
                                homeLon = coords?.second,
                                locationStatusMessage = "Vị trí nhà: $addr"
                            )
                        }
                        recomputeCinemaList()
                    }
                    is Resource.Error -> {
                        _listUiState.value = _listUiState.value.copy(
                            locationMode = LocationFilterMode.NONE,
                            locationStatusMessage = res.message ?: "Không tải được địa chỉ nhà"
                        )
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private fun recomputeCinemaList() {
        val state = _listUiState.value
        val brandFiltered = if (state.selectedBrand == "Đề xuất" || state.selectedBrand.isBlank()) {
            state.allCinemas
        } else {
            val normBrand = LocationUtils.normalizeSearchText(state.selectedBrand)
            state.allCinemas.filter { cinema ->
                val text = LocationUtils.normalizeSearchText("${cinema.brandName} ${cinema.name}")
                text.contains(normBrand)
            }
        }

        val withDistance = MovieViewModel.filterAndSortCinemas(
            cinemas = brandFiltered,
            normalizedQuery = "",
            locationMode = state.locationMode,
            selectedCity = state.selectedCity,
            currentLat = state.currentLat,
            currentLon = state.currentLon,
            homeLat = state.homeLat,
            homeLon = state.homeLon,
            homeAddress = state.homeAddress
        )

        _listUiState.value = state.copy(filteredCinemas = withDistance)
    }

    fun initCinemaDetail(
        cinemaId: Long,
        initialDate: String = "",
        initialTimeSlotCode: String = "ALL"
    ) {
        val baseDates = MovieViewModel.buildUpcomingDates()
        val chosenDate = if (initialDate.isNotBlank() && baseDates.any { it.dateString == initialDate }) {
            initialDate
        } else {
            baseDates.firstOrNull()?.dateString.orEmpty()
        }
        val dates = baseDates.map { it.copy(isSelected = it.dateString == chosenDate) }
        val slot = TimeSlotFilter.fromCode(initialTimeSlotCode)

        val cachedCinema = CinemaRepositoryImpl.getCachedCinemasSnapshot().firstOrNull { it.cinemaId == cinemaId }
        _detailUiState.value = _detailUiState.value.copy(
            cinema = cachedCinema ?: _detailUiState.value.cinema,
            dates = dates,
            selectedDate = chosenDate,
            selectedTimeSlot = slot
        )
        loadCinemaDetailData(cinemaId = cinemaId, dateStr = chosenDate)
    }

    fun selectDetailDate(cinemaId: Long, dateStr: String) {
        if (dateStr == _detailUiState.value.selectedDate) return
        val updatedDates = _detailUiState.value.dates.map {
            it.copy(isSelected = it.dateString == dateStr)
        }
        _detailUiState.value = _detailUiState.value.copy(
            dates = updatedDates,
            selectedDate = dateStr
        )
        loadCinemaDetailData(cinemaId = cinemaId, dateStr = dateStr)
    }

    fun selectDetailTimeSlot(slot: TimeSlotFilter) {
        _detailUiState.value = _detailUiState.value.copy(selectedTimeSlot = slot)
        recomputeDetailMovies()
    }

    fun loadCinemaDetailData(cinemaId: Long, dateStr: String = _detailUiState.value.selectedDate) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailUiState.value = _detailUiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            if (cachedMoviesById.isEmpty()) {
                val snap = MovieRepositoryImpl.getCachedMoviesSnapshot()
                if (snap.isNotEmpty()) {
                    cachedMoviesById = snap.associateBy { it.movieId }
                } else {
                    getMoviesUseCase(q = null, status = null, page = 0, size = 50).collect { res ->
                        if (res is Resource.Success) {
                            cachedMoviesById = res.data.orEmpty().associateBy { it.movieId }
                        }
                    }
                }
            }

            val showtimes = mutableListOf<Showtime>()
            var lastErr: String? = null
            var page = 0
            while (page < 8) {
                var pageData: List<Showtime> = emptyList()
                var hadError = false
                getShowtimesUseCase(
                    movieId = null,
                    cinemaId = cinemaId,
                    date = dateStr.ifBlank { null },
                    page = page
                ).collect { res ->
                    when (res) {
                        is Resource.Success -> pageData = res.data.orEmpty()
                        is Resource.Error -> {
                            lastErr = res.message
                            hadError = true
                        }
                        is Resource.Loading -> Unit
                    }
                }
                if (hadError || pageData.isEmpty()) break
                showtimes.addAll(pageData)
                rawCinemaShowtimes = showtimes.distinctBy { it.showtimeId }
                _detailUiState.value = _detailUiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                recomputeDetailMovies()

                if (pageData.size < 10) break
                page++
            }

            _detailUiState.value = _detailUiState.value.copy(
                isLoading = false,
                errorMessage = if (rawCinemaShowtimes.isEmpty()) lastErr else null
            )
            recomputeDetailMovies()
        }
    }

    private fun recomputeDetailMovies() {
        val slot = _detailUiState.value.selectedTimeSlot
        val filtered = rawCinemaShowtimes.filter { slot.matches(it.startTime) }

        val grouped = filtered
            .groupBy { it.movieId }
            .mapNotNull { (movieId, list) ->
                val sample = list.firstOrNull() ?: return@mapNotNull null
                val movie = cachedMoviesById[movieId] ?: Movie(
                    movieId = movieId,
                    title = sample.movieTitle.ifBlank { "Phim #$movieId" },
                    poster = sample.moviePosterUrl,
                    trailer = "",
                    duration = 0,
                    releaseDate = "",
                    genres = "",
                    rating = 0.0,
                    description = ""
                )
                CinemaMovieShowtimeItem(
                    movie = movie,
                    showtimes = list.sortedBy { it.startTime }
                )
            }

        _detailUiState.value = _detailUiState.value.copy(movieShowtimes = grouped)
    }

    private fun buildBrandList(cinemas: List<Cinema>): List<String> {
        val extracted = cinemas.mapNotNull { cinema ->
            val brand = cinema.brandName.trim()
            if (brand.isNotBlank()) {
                brand
            } else {
                val firstToken = cinema.name.trim().split(" ").firstOrNull().orEmpty()
                firstToken.ifBlank { null }
            }
        }.distinct()

        return listOf("Đề xuất") + extracted
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                CinemaViewModel(
                    getCinemasUseCase = app.useCaseModule.getCinemasUseCase,
                    getShowtimesUseCase = app.useCaseModule.getShowtimesUseCase,
                    getMoviesUseCase = app.useCaseModule.getMoviesUseCase,
                    authRepository = app.repositoryModule.authRepository
                )
            }
        }
    }
}
