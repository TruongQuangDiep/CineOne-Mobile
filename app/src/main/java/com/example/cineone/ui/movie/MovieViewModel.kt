package com.example.cineone.ui.movie

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
import com.example.cineone.domain.usecase.GetMovieDetailUseCase
import com.example.cineone.domain.usecase.GetMoviesUseCase
import com.example.cineone.domain.usecase.GetShowtimesUseCase
import com.example.cineone.domain.usecase.SearchMoviesUseCase
import com.example.cineone.ui.showtime.DateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class SearchCategoryTab(val label: String) {
    CINEMA("Rạp"),
    MOVIE("Phim"),
    GENRE("Thể loại"),
    SHOW_DATE("Ngày chiếu");

    companion object {
        fun fromCode(code: String?): SearchCategoryTab {
            if (code.isNullOrBlank()) return CINEMA
            return entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) } ?: CINEMA
        }
    }
}

data class CinemaWithDistance(
    val cinema: Cinema,
    val distanceKm: Double? = null
)

data class SearchCinemaShowtimeGroup(
    val cinemaId: Long,
    val cinemaName: String,
    val areaLabel: String,
    val address: String,
    val distanceKm: Double? = null,
    val showtimes: List<Showtime>
)

data class SearchDateMovieGroup(
    val movie: Movie,
    val cinemaGroups: List<SearchCinemaShowtimeGroup>
)

data class MovieDetailUiState(
    val isLoading: Boolean = false,
    val movie: Movie? = null,
    val errorMessage: String? = null
)

data class MovieListUiState(
    val isLoading: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val selectedStatus: String = "NOW_SHOWING",
    val searchQuery: String = "",
    val errorMessage: String? = null
)

data class MovieSearchUiState(
    val isLoading: Boolean = false,
    val isShowtimesLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTab: SearchCategoryTab = SearchCategoryTab.CINEMA,
    val locationMode: LocationFilterMode = LocationFilterMode.NONE,
    val selectedCity: String = "",
    val availableCities: List<String> = emptyList(),
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val homeLat: Double? = null,
    val homeLon: Double? = null,
    val homeAddress: String = "",
    val locationStatusMessage: String? = null,
    val allCinemas: List<Cinema> = emptyList(),
    val filteredCinemas: List<CinemaWithDistance> = emptyList(),
    val visibleCinemaCount: Int = 5,
    val allMovies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val visibleMovieCount: Int = 5,
    val availableGenres: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val genreFilteredMovies: List<Movie> = emptyList(),
    val visibleGenreMovieCount: Int = 5,
    val dates: List<DateItem> = emptyList(),
    val selectedDate: String = "",
    val selectedTimeSlot: TimeSlotFilter = TimeSlotFilter.ALL,
    val rawShowtimesForDate: List<Showtime> = emptyList(),
    val dateMovieGroups: List<SearchDateMovieGroup> = emptyList(),
    val visibleDateMovieCount: Int = 5,
    val errorMessage: String? = null
)

class MovieViewModel(
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getMoviesUseCase: GetMoviesUseCase,
    private val searchMoviesUseCase: SearchMoviesUseCase? = null,
    private val getCinemasUseCase: GetCinemasUseCase? = null,
    private val getShowtimesUseCase: GetShowtimesUseCase? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _detailUiState = MutableStateFlow(MovieDetailUiState())
    val detailUiState: StateFlow<MovieDetailUiState> = _detailUiState.asStateFlow()

    private val _listUiState = MutableStateFlow(MovieListUiState())
    val listUiState: StateFlow<MovieListUiState> = _listUiState.asStateFlow()

    private val _searchUiState = MutableStateFlow(MovieSearchUiState())
    val searchUiState: StateFlow<MovieSearchUiState> = _searchUiState.asStateFlow()

    private var showtimesLoadJob: Job? = null
    private var searchInitialized = false

    fun loadMovieDetail(movieId: Long) {
        if (movieId <= 0L) {
            _detailUiState.value = MovieDetailUiState(
                isLoading = false,
                movie = null,
                errorMessage = "Mã phim không hợp lệ"
            )
            return
        }
        viewModelScope.launch {
            getMovieDetailUseCase(movieId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _detailUiState.value = _detailUiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _detailUiState.value = _detailUiState.value.copy(
                            isLoading = false,
                            movie = resource.data,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _detailUiState.value = _detailUiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải thông tin chi tiết phim"
                        )
                    }
                }
            }
        }
    }

    fun loadMovies(
        query: String = _listUiState.value.searchQuery,
        status: String = _listUiState.value.selectedStatus
    ) {
        _listUiState.value = _listUiState.value.copy(
            searchQuery = query,
            selectedStatus = status
        )
        viewModelScope.launch {
            getMoviesUseCase(
                q = query.trim().ifBlank { null },
                status = status.ifBlank { null },
                page = 0,
                size = 30
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _listUiState.value = _listUiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _listUiState.value = _listUiState.value.copy(
                            isLoading = false,
                            movies = resource.data ?: emptyList(),
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _listUiState.value = _listUiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải danh sách phim"
                        )
                    }
                }
            }
        }
    }

    fun initSearchScreen(
        initialQuery: String = "",
        initialTabCode: String = "CINEMA",
        initialLocationCode: String = "NONE"
    ) {
        val dates = buildUpcomingDates()
        val defaultDate = dates.firstOrNull()?.dateString.orEmpty()
        val targetTab = SearchCategoryTab.fromCode(initialTabCode)
        val targetLoc = LocationFilterMode.fromCode(initialLocationCode)

        if (!searchInitialized) {
            searchInitialized = true
            val cachedCinemas = CinemaRepositoryImpl.getCachedCinemasSnapshot()
            val cachedMovies = MovieRepositoryImpl.getCachedMoviesSnapshot()

            _searchUiState.value = _searchUiState.value.copy(
                searchQuery = initialQuery,
                selectedTab = targetTab,
                locationMode = if (targetLoc == LocationFilterMode.PROVINCE) LocationFilterMode.NONE else targetLoc,
                dates = dates,
                selectedDate = defaultDate,
                allCinemas = cachedCinemas,
                availableCities = cachedCinemas.map { it.city.trim() }.filter { it.isNotBlank() }.distinct().sorted(),
                allMovies = cachedMovies,
                availableGenres = extractGenresFromMovies(cachedMovies)
            )
            if (cachedCinemas.isNotEmpty() || cachedMovies.isNotEmpty()) {
                recomputeSearchResults()
            }

            refreshSearchCatalog()

            // Chỉ tải suất chiếu khi người dùng thực sự mở tab Ngày chiếu (tránh nghẽn mạng khi vừa mở màn hình Tìm kiếm)
            if (targetTab == SearchCategoryTab.SHOW_DATE && defaultDate.isNotBlank()) {
                loadShowtimesForSearchDate(defaultDate)
            }
        } else if (initialQuery.isNotBlank() && initialQuery != _searchUiState.value.searchQuery) {
            onSearchQueryChange(initialQuery)
        }
    }

    fun refreshSearchCatalog() {
        val hasInitialData = _searchUiState.value.allCinemas.isNotEmpty() ||
            _searchUiState.value.allMovies.isNotEmpty()
        if (!hasInitialData) {
            _searchUiState.value = _searchUiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        // 1. Tải danh sách phim song song
        viewModelScope.launch {
            val movieFlow = searchMoviesUseCase?.invoke(q = null, status = null, page = 0, size = 50)
                ?: getMoviesUseCase(q = null, status = null, page = 0, size = 50)
            movieFlow.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val movies = res.data.orEmpty()
                        _searchUiState.value = _searchUiState.value.copy(
                            isLoading = false,
                            allMovies = movies,
                            availableGenres = extractGenresFromMovies(movies),
                            errorMessage = null
                        )
                        recomputeSearchResults()
                    }
                    is Resource.Error -> {
                        if (_searchUiState.value.allMovies.isEmpty() && _searchUiState.value.allCinemas.isEmpty()) {
                            _searchUiState.value = _searchUiState.value.copy(
                                isLoading = false,
                                errorMessage = res.message
                            )
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }

        // 2. Tải danh sách rạp song song (hiển thị ngay trang đầu tiên, tải ngầm các trang tiếp theo nếu chưa đầy đủ trong cache)
        viewModelScope.launch {
            if (CinemaRepositoryImpl.isFullListFresh()) {
                val cached = CinemaRepositoryImpl.getCachedCinemasSnapshot()
                updateCinemasInState(cached)
                return@launch
            }

            val cinemasUseCase = getCinemasUseCase ?: return@launch
            val accumulated = _searchUiState.value.allCinemas.toMutableList()
            var page = 0
            while (page < 10) {
                var pageItems: List<Cinema> = emptyList()
                var hadError = false
                cinemasUseCase(q = null, city = null, status = "ACTIVE", page = page).collect { res ->
                    when (res) {
                        is Resource.Success -> pageItems = res.data.orEmpty()
                        is Resource.Error -> hadError = true
                        is Resource.Loading -> Unit
                    }
                }
                if (hadError) break
                if (pageItems.isEmpty()) {
                    CinemaRepositoryImpl.markFullCatalogLoaded()
                    break
                }
                accumulated.addAll(pageItems)
                val distinctList = accumulated.distinctBy { it.cinemaId }
                // Cập nhật UI ngay lập tức sau mỗi trang (đặc biệt là trang 0) để người dùng không phải đợi!
                updateCinemasInState(distinctList)

                if (pageItems.size < 10) {
                    CinemaRepositoryImpl.markFullCatalogLoaded()
                    break
                }
                page++
            }
        }
    }

    private fun updateCinemasInState(cinemas: List<Cinema>) {
        val cities = cinemas
            .map { it.city.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        _searchUiState.value = _searchUiState.value.copy(
            isLoading = false,
            allCinemas = cinemas,
            availableCities = cities,
            errorMessage = null
        )
        recomputeSearchResults()
    }

    fun loadShowtimesForSearchDate(dateStr: String) {
        val showtimesUseCase = getShowtimesUseCase ?: return
        showtimesLoadJob?.cancel()
        showtimesLoadJob = viewModelScope.launch {
            _searchUiState.value = _searchUiState.value.copy(
                isShowtimesLoading = true
            )
            val allShowtimes = mutableListOf<Showtime>()
            var page = 0
            while (page < 8) {
                var pageData: List<Showtime> = emptyList()
                var hadError = false
                showtimesUseCase(movieId = null, cinemaId = null, date = dateStr, page = page).collect { res ->
                    when (res) {
                        is Resource.Success -> pageData = res.data.orEmpty()
                        is Resource.Error -> hadError = true
                        is Resource.Loading -> Unit
                    }
                }
                if (hadError || pageData.isEmpty()) break
                allShowtimes.addAll(pageData)
                // Hiển thị ngay trang suất chiếu đầu tiên cho người dùng, các trang sau nối thêm mượt mà
                _searchUiState.value = _searchUiState.value.copy(
                    isShowtimesLoading = false,
                    rawShowtimesForDate = allShowtimes.distinctBy { it.showtimeId }
                )
                recomputeSearchResults()

                if (pageData.size < 10) break
                page++
            }
            _searchUiState.value = _searchUiState.value.copy(
                isShowtimesLoading = false
            )
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchUiState.value = _searchUiState.value.copy(
            searchQuery = newQuery,
            visibleCinemaCount = 5,
            visibleMovieCount = 5,
            visibleGenreMovieCount = 5,
            visibleDateMovieCount = 5
        )
        recomputeSearchResults()
    }

    fun onSearchTabSelected(tab: SearchCategoryTab) {
        _searchUiState.value = _searchUiState.value.copy(selectedTab = tab)
        if (tab == SearchCategoryTab.SHOW_DATE &&
            _searchUiState.value.rawShowtimesForDate.isEmpty() &&
            !_searchUiState.value.isShowtimesLoading &&
            _searchUiState.value.selectedDate.isNotBlank()
        ) {
            loadShowtimesForSearchDate(_searchUiState.value.selectedDate)
        }
    }

    fun selectProvince(city: String) {
        val cleanCity = city.trim()
        if (cleanCity.isBlank()) {
            _searchUiState.value = _searchUiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                selectedCity = "",
                locationStatusMessage = null,
                visibleCinemaCount = 5,
                visibleDateMovieCount = 5
            )
        } else {
            _searchUiState.value = _searchUiState.value.copy(
                locationMode = LocationFilterMode.PROVINCE,
                selectedCity = cleanCity,
                locationStatusMessage = "Đang lọc theo tỉnh thành: $cleanCity",
                visibleCinemaCount = 5,
                visibleDateMovieCount = 5
            )
        }
        recomputeSearchResults()
    }

    fun activateCurrentLocation(context: Context) {
        if (_searchUiState.value.locationMode == LocationFilterMode.CURRENT_LOCATION) {
            _searchUiState.value = _searchUiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                locationStatusMessage = null,
                visibleCinemaCount = 5,
                visibleDateMovieCount = 5
            )
            recomputeSearchResults()
            return
        }

        viewModelScope.launch {
            val coords = withContext(Dispatchers.IO) {
                LocationUtils.fetchDeviceLocation(context)
            }
            if (coords != null) {
                _searchUiState.value = _searchUiState.value.copy(
                    locationMode = LocationFilterMode.CURRENT_LOCATION,
                    selectedCity = "",
                    currentLat = coords.first,
                    currentLon = coords.second,
                    locationStatusMessage = "Đang sắp xếp rạp gần vị trí hiện tại (GPS)",
                    visibleCinemaCount = 5,
                    visibleDateMovieCount = 5
                )
            } else {
                val fallbackCinema = _searchUiState.value.allCinemas.firstOrNull {
                    it.latitude != null && it.longitude != null
                }
                if (fallbackCinema != null) {
                    _searchUiState.value = _searchUiState.value.copy(
                        locationMode = LocationFilterMode.CURRENT_LOCATION,
                        selectedCity = "",
                        currentLat = fallbackCinema.latitude,
                        currentLon = fallbackCinema.longitude,
                        locationStatusMessage = "Đang sắp xếp khoảng cách theo vị trí thiết bị",
                        visibleCinemaCount = 5,
                        visibleDateMovieCount = 5
                    )
                } else {
                    _searchUiState.value = _searchUiState.value.copy(
                        locationMode = LocationFilterMode.NONE,
                        locationStatusMessage = "Không thể xác định tọa độ GPS hiện tại của thiết bị"
                    )
                }
            }
            recomputeSearchResults()
        }
    }

    fun onLocationPermissionDenied() {
        _searchUiState.value = _searchUiState.value.copy(
            locationMode = LocationFilterMode.NONE,
            locationStatusMessage = "Vui lòng cấp quyền truy cập vị trí (GPS) để tìm rạp gần bạn"
        )
    }

    fun activateHomeLocation(context: Context) {
        if (_searchUiState.value.locationMode == LocationFilterMode.HOME_LOCATION) {
            _searchUiState.value = _searchUiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                locationStatusMessage = null,
                visibleCinemaCount = 5,
                visibleDateMovieCount = 5
            )
            recomputeSearchResults()
            return
        }

        val repo = authRepository
        val isLoggedIn = runCatching {
            CineOneApp.instance.networkModule.sessionManager.isLoggedIn()
        }.getOrDefault(false)

        if (repo == null || !isLoggedIn) {
            _searchUiState.value = _searchUiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                locationStatusMessage = "Vui lòng đăng nhập tài khoản để sử dụng Vị trí nhà tôi"
            )
            return
        }

        viewModelScope.launch {
            repo.getProfile().collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _searchUiState.value = _searchUiState.value.copy(
                            locationStatusMessage = "Đang lấy địa chỉ nhà từ hồ sơ cá nhân..."
                        )
                    }
                    is Resource.Success -> {
                        val homeAddr = res.data?.address?.trim().orEmpty()
                        if (homeAddr.isBlank()) {
                            _searchUiState.value = _searchUiState.value.copy(
                                locationMode = LocationFilterMode.NONE,
                                locationStatusMessage = "Bạn chưa cập nhật địa chỉ nhà trong hồ sơ cá nhân"
                            )
                        } else {
                            val coords = withContext(Dispatchers.IO) {
                                LocationUtils.resolveAddressCoordinates(
                                    context = context,
                                    address = homeAddr,
                                    cinemas = _searchUiState.value.allCinemas
                                )
                            }
                            _searchUiState.value = _searchUiState.value.copy(
                                locationMode = LocationFilterMode.HOME_LOCATION,
                                selectedCity = "",
                                homeAddress = homeAddr,
                                homeLat = coords?.first,
                                homeLon = coords?.second,
                                locationStatusMessage = "Vị trí nhà: $homeAddr",
                                visibleCinemaCount = 5,
                                visibleDateMovieCount = 5
                            )
                        }
                        recomputeSearchResults()
                    }
                    is Resource.Error -> {
                        _searchUiState.value = _searchUiState.value.copy(
                            locationMode = LocationFilterMode.NONE,
                            locationStatusMessage = res.message ?: "Không thể tải địa chỉ nhà từ hồ sơ"
                        )
                    }
                }
            }
        }
    }

    fun toggleGenre(genre: String) {
        val clean = genre.trim()
        if (clean.isBlank()) return
        val current = _searchUiState.value.selectedGenres.toMutableSet()
        if (current.contains(clean)) {
            current.remove(clean)
        } else {
            current.add(clean)
        }
        _searchUiState.value = _searchUiState.value.copy(
            selectedGenres = current,
            visibleGenreMovieCount = 5
        )
        recomputeSearchResults()
    }

    fun clearGenres() {
        _searchUiState.value = _searchUiState.value.copy(
            selectedGenres = emptySet(),
            visibleGenreMovieCount = 5
        )
        recomputeSearchResults()
    }

    fun selectSearchDate(dateStr: String) {
        if (dateStr == _searchUiState.value.selectedDate) return
        val updatedDates = _searchUiState.value.dates.map {
            it.copy(isSelected = (it.dateString == dateStr))
        }
        _searchUiState.value = _searchUiState.value.copy(
            dates = updatedDates,
            selectedDate = dateStr,
            visibleDateMovieCount = 5
        )
        loadShowtimesForSearchDate(dateStr)
    }

    fun selectSearchTimeSlot(slot: TimeSlotFilter) {
        _searchUiState.value = _searchUiState.value.copy(
            selectedTimeSlot = slot,
            visibleDateMovieCount = 5
        )
        recomputeSearchResults()
    }

    fun loadMoreCinemas() {
        _searchUiState.value = _searchUiState.value.copy(
            visibleCinemaCount = _searchUiState.value.visibleCinemaCount + 5
        )
    }

    fun loadMoreMovies() {
        _searchUiState.value = _searchUiState.value.copy(
            visibleMovieCount = _searchUiState.value.visibleMovieCount + 5
        )
    }

    fun loadMoreGenreMovies() {
        _searchUiState.value = _searchUiState.value.copy(
            visibleGenreMovieCount = _searchUiState.value.visibleGenreMovieCount + 5
        )
    }

    fun loadMoreDateMovies() {
        _searchUiState.value = _searchUiState.value.copy(
            visibleDateMovieCount = _searchUiState.value.visibleDateMovieCount + 5
        )
    }

    private fun recomputeSearchResults() {
        val state = _searchUiState.value
        val normalizedQuery = LocationUtils.normalizeSearchText(state.searchQuery)

        val filteredCinemas = filterAndSortCinemas(
            cinemas = state.allCinemas,
            normalizedQuery = normalizedQuery,
            locationMode = state.locationMode,
            selectedCity = state.selectedCity,
            currentLat = state.currentLat,
            currentLon = state.currentLon,
            homeLat = state.homeLat,
            homeLon = state.homeLon,
            homeAddress = state.homeAddress
        )

        val filteredMovies = state.allMovies.filter { movie ->
            if (normalizedQuery.isBlank()) {
                true
            } else {
                val searchable = LocationUtils.normalizeSearchText(
                    "${movie.title} ${movie.genres} ${movie.director} ${movie.castInfo}"
                )
                searchable.contains(normalizedQuery)
            }
        }

        val selectedGenresNorm = state.selectedGenres.map { LocationUtils.normalizeSearchText(it) }.toSet()
        val genreFiltered = filteredMovies.filter { movie ->
            if (selectedGenresNorm.isEmpty()) {
                true
            } else {
                val movieGenresNorm = splitGenres(movie.genres).map { LocationUtils.normalizeSearchText(it) }
                selectedGenresNorm.any { sel -> movieGenresNorm.any { it == sel || it.contains(sel) } }
            }
        }

        val allowedCinemaMap = filteredCinemas.associateBy { it.cinema.cinemaId }
        val allCinemaMap = state.allCinemas.associateBy { it.cinemaId }
        val movieMap = state.allMovies.associateBy { it.movieId }

        val matchingShowtimes = state.rawShowtimesForDate.filter { st ->
            val slotMatch = state.selectedTimeSlot.matches(st.startTime)
            if (!slotMatch) return@filter false

            val locationMatch = when (state.locationMode) {
                LocationFilterMode.NONE -> true
                LocationFilterMode.PROVINCE,
                LocationFilterMode.CURRENT_LOCATION,
                LocationFilterMode.HOME_LOCATION -> {
                    if (state.allCinemas.isEmpty()) true else allowedCinemaMap.containsKey(st.cinemaId)
                }
            }
            if (!locationMatch) return@filter false

            if (normalizedQuery.isBlank()) {
                true
            } else {
                val movieObj = movieMap[st.movieId]
                val cinemaObj = allCinemaMap[st.cinemaId]
                val combinedText = LocationUtils.normalizeSearchText(
                    "${st.movieTitle} ${movieObj?.title.orEmpty()} ${movieObj?.genres.orEmpty()} " +
                        "${st.cinemaName} ${cinemaObj?.name.orEmpty()} ${cinemaObj?.address.orEmpty()}"
                )
                combinedText.contains(normalizedQuery)
            }
        }

        val dateGroups = matchingShowtimes
            .groupBy { it.movieId }
            .mapNotNull { (movieId, movieShowtimes) ->
                val sample = movieShowtimes.firstOrNull() ?: return@mapNotNull null
                val movie = movieMap[movieId] ?: Movie(
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

                val cinemaGroups = movieShowtimes
                    .groupBy { it.cinemaId }
                    .map { (cinemaId, cShowtimes) ->
                        val cinemaWithDist = allowedCinemaMap[cinemaId]
                        val cinema = cinemaWithDist?.cinema ?: allCinemaMap[cinemaId]
                        val cName = cinema?.name?.ifBlank { null }
                            ?: cShowtimes.firstOrNull()?.cinemaName?.ifBlank { null }
                            ?: "Rạp #$cinemaId"
                        val areaLabel = cinema?.let { LocationUtils.extractAreaBracket(it) }.orEmpty()
                        val sortedTimes = cShowtimes.sortedBy { it.startTime }
                        SearchCinemaShowtimeGroup(
                            cinemaId = cinemaId,
                            cinemaName = cName,
                            areaLabel = areaLabel,
                            address = cinema?.address.orEmpty(),
                            distanceKm = cinemaWithDist?.distanceKm,
                            showtimes = sortedTimes
                        )
                    }
                    .sortedWith(
                        compareBy<SearchCinemaShowtimeGroup> { it.distanceKm ?: Double.MAX_VALUE }
                            .thenBy { it.cinemaName }
                    )

                SearchDateMovieGroup(
                    movie = movie,
                    cinemaGroups = cinemaGroups
                )
            }

        _searchUiState.value = state.copy(
            filteredCinemas = filteredCinemas,
            filteredMovies = filteredMovies,
            genreFilteredMovies = genreFiltered,
            dateMovieGroups = dateGroups
        )
    }

    companion object {
        fun splitGenres(rawGenres: String): List<String> {
            if (rawGenres.isBlank()) return emptyList()
            return rawGenres
                .split(Regex("[,;/|•]+"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        fun extractGenresFromMovies(movies: List<Movie>): List<String> {
            val seenNormalized = mutableSetOf<String>()
            val result = mutableListOf<String>()
            for (movie in movies) {
                for (g in splitGenres(movie.genres)) {
                    val norm = LocationUtils.normalizeSearchText(g)
                    if (norm.isNotBlank() && seenNormalized.add(norm)) {
                        result.add(g)
                    }
                }
            }
            return result
        }

        fun filterAndSortCinemas(
            cinemas: List<Cinema>,
            normalizedQuery: String,
            locationMode: LocationFilterMode,
            selectedCity: String,
            currentLat: Double?,
            currentLon: Double?,
            homeLat: Double?,
            homeLon: Double?,
            homeAddress: String
        ): List<CinemaWithDistance> {
            val queryFiltered = cinemas.filter { cinema ->
                if (normalizedQuery.isBlank()) {
                    true
                } else {
                    val text = LocationUtils.normalizeSearchText(
                        "${cinema.name} ${cinema.address} ${cinema.city} ${cinema.brandName}"
                    )
                    text.contains(normalizedQuery)
                }
            }

            return when (locationMode) {
                LocationFilterMode.NONE -> {
                    queryFiltered.map { CinemaWithDistance(cinema = it, distanceKm = null) }
                }

                LocationFilterMode.PROVINCE -> {
                    val normCity = LocationUtils.normalizeSearchText(selectedCity)
                    queryFiltered
                        .filter {
                            normCity.isBlank() ||
                                LocationUtils.normalizeSearchText(it.city) == normCity ||
                                LocationUtils.normalizeSearchText(it.address).contains(normCity)
                        }
                        .map { CinemaWithDistance(cinema = it, distanceKm = null) }
                }

                LocationFilterMode.CURRENT_LOCATION -> {
                    queryFiltered
                        .map { cinema ->
                            val dist = if (
                                currentLat != null && currentLon != null &&
                                cinema.latitude != null && cinema.longitude != null
                            ) {
                                LocationUtils.calculateDistanceKm(
                                    currentLat,
                                    currentLon,
                                    cinema.latitude,
                                    cinema.longitude
                                )
                            } else {
                                null
                            }
                            CinemaWithDistance(cinema = cinema, distanceKm = dist)
                        }
                        .sortedWith(
                            compareBy<CinemaWithDistance> { it.distanceKm ?: Double.MAX_VALUE }
                                .thenBy { it.cinema.name }
                        )
                }

                LocationFilterMode.HOME_LOCATION -> {
                    val normHome = LocationUtils.normalizeSearchText(homeAddress)
                    queryFiltered
                        .map { cinema ->
                            val dist = if (
                                homeLat != null && homeLon != null &&
                                cinema.latitude != null && cinema.longitude != null
                            ) {
                                LocationUtils.calculateDistanceKm(
                                    homeLat,
                                    homeLon,
                                    cinema.latitude,
                                    cinema.longitude
                                )
                            } else {
                                null
                            }
                            CinemaWithDistance(cinema = cinema, distanceKm = dist)
                        }
                        .sortedWith(
                            compareBy<CinemaWithDistance> { item ->
                                item.distanceKm ?: if (
                                    normHome.isNotBlank() &&
                                    normHome.contains(LocationUtils.normalizeSearchText(item.cinema.city))
                                ) {
                                    10.0
                                } else {
                                    Double.MAX_VALUE
                                }
                            }.thenBy { it.cinema.name }
                        )
                }
            }
        }

        fun buildUpcomingDates(): List<DateItem> {
            val today = LocalDate.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val displayFormatter = DateTimeFormatter.ofPattern("dd/MM")

            return (0..6).map { offset ->
                val d = today.plusDays(offset.toLong())
                val dayLabel = when (offset) {
                    0 -> "Hôm nay"
                    else -> when (d.dayOfWeek.value) {
                        1 -> "Thứ 2"
                        2 -> "Thứ 3"
                        3 -> "Thứ 4"
                        4 -> "Thứ 5"
                        5 -> "Thứ 6"
                        6 -> "Thứ 7"
                        else -> "CN"
                    }
                }
                DateItem(
                    dateString = d.format(dateFormatter),
                    dayOfWeek = dayLabel,
                    dayOfMonth = d.format(displayFormatter),
                    isSelected = (offset == 0)
                )
            }
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                MovieViewModel(
                    getMovieDetailUseCase = app.useCaseModule.getMovieDetailUseCase,
                    getMoviesUseCase = app.useCaseModule.getMoviesUseCase,
                    searchMoviesUseCase = app.useCaseModule.searchMoviesUseCase,
                    getCinemasUseCase = app.useCaseModule.getCinemasUseCase,
                    getShowtimesUseCase = app.useCaseModule.getShowtimesUseCase,
                    authRepository = app.repositoryModule.authRepository
                )
            }
        }
    }
}
