package com.example.cineone.ui.showtime

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
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.AuthRepository
import com.example.cineone.domain.usecase.GetCinemasUseCase
import com.example.cineone.domain.usecase.GetShowtimesUseCase
import com.example.cineone.ui.movie.MovieViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DateItem(
    val dateString: String,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val isSelected: Boolean = false
)

data class CinemaShowtimeGroupItem(
    val cinemaId: Long,
    val cinemaName: String,
    val brandName: String,
    val address: String,
    val city: String,
    val distanceKm: Double? = null,
    val showtimes: List<Showtime>
)

data class ShowtimeUiState(
    val isLoading: Boolean = false,
    val dates: List<DateItem> = emptyList(),
    val selectedDate: String = "",
    val selectedTimeSlot: TimeSlotFilter = TimeSlotFilter.ALL,
    val availableBrands: List<String> = listOf("Đề xuất"),
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
    val cinemaGroups: List<CinemaShowtimeGroupItem> = emptyList(),
    val showtimesByCinema: Map<String, List<Showtime>> = emptyMap(),
    val errorMessage: String? = null
)

class ShowtimeViewModel(
    private val getShowtimesUseCase: GetShowtimesUseCase,
    private val getCinemasUseCase: GetCinemasUseCase? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowtimeUiState())
    val uiState: StateFlow<ShowtimeUiState> = _uiState.asStateFlow()

    private var rawShowtimes: List<Showtime> = emptyList()
    private var allCinemas: List<Cinema> = emptyList()
    private var initializedCriteria = false

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

    fun initWithPreservedCriteria(
        movieId: Long,
        initialDate: String = "",
        initialTimeSlot: String = "ALL",
        initialLocationMode: String = "NONE",
        initialCity: String = "",
        context: Context? = null
    ) {
        val currentDates = _uiState.value.dates
        val targetDate = if (initialDate.isNotBlank() && currentDates.any { it.dateString == initialDate }) {
            initialDate
        } else {
            _uiState.value.selectedDate
        }
        val updatedDates = currentDates.map { it.copy(isSelected = it.dateString == targetDate) }
        val targetSlot = TimeSlotFilter.fromCode(initialTimeSlot)
        val targetLoc = LocationFilterMode.fromCode(initialLocationMode)

        if (!initializedCriteria) {
            initializedCriteria = true
            _uiState.value = _uiState.value.copy(
                dates = updatedDates,
                selectedDate = targetDate,
                selectedTimeSlot = targetSlot,
                locationMode = targetLoc,
                selectedCity = initialCity
            )
            if (targetLoc == LocationFilterMode.CURRENT_LOCATION && context != null) {
                activateCurrentLocation(context)
            } else if (targetLoc == LocationFilterMode.HOME_LOCATION && context != null) {
                activateHomeLocation(context)
            }
        }
        loadShowtimes(movieId = movieId, date = targetDate)
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

    fun onTimeSlotSelected(slot: TimeSlotFilter) {
        _uiState.value = _uiState.value.copy(selectedTimeSlot = slot)
        recomputeFilteredGroups()
    }

    fun onBrandSelected(brand: String) {
        _uiState.value = _uiState.value.copy(selectedBrand = brand)
        recomputeFilteredGroups()
    }

    fun clearLocationFilter() {
        _uiState.value = _uiState.value.copy(
            locationMode = LocationFilterMode.NONE,
            selectedCity = "",
            locationStatusMessage = null
        )
        recomputeFilteredGroups()
    }

    fun selectCityFilter(city: String) {
        val clean = city.trim()
        if (clean.isBlank()) {
            clearLocationFilter()
            return
        }
        _uiState.value = _uiState.value.copy(
            locationMode = LocationFilterMode.PROVINCE,
            selectedCity = clean,
            locationStatusMessage = "Tỉnh/TP: $clean"
        )
        recomputeFilteredGroups()
    }

    fun activateCurrentLocation(context: Context) {
        viewModelScope.launch {
            val coords = withContext(Dispatchers.IO) {
                LocationUtils.fetchDeviceLocation(context)
            }
            val fallback = allCinemas.firstOrNull { it.latitude != null && it.longitude != null }
            val lat = coords?.first ?: fallback?.latitude
            val lon = coords?.second ?: fallback?.longitude
            if (lat != null && lon != null) {
                _uiState.value = _uiState.value.copy(
                    locationMode = LocationFilterMode.CURRENT_LOCATION,
                    selectedCity = "",
                    currentLat = lat,
                    currentLon = lon,
                    locationStatusMessage = "Đang sắp xếp theo Vị trí gần tôi"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    locationMode = LocationFilterMode.NONE,
                    locationStatusMessage = "Không thể xác định tọa độ GPS"
                )
            }
            recomputeFilteredGroups()
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
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
            _uiState.value = _uiState.value.copy(
                locationMode = LocationFilterMode.NONE,
                locationStatusMessage = "Vui lòng đăng nhập để lọc theo Vị trí gần nhà"
            )
            return
        }

        viewModelScope.launch {
            repo.getProfile().collect { res ->
                if (res is Resource.Success) {
                    val addr = res.data?.address?.trim().orEmpty()
                    if (addr.isNotBlank()) {
                        val coords = withContext(Dispatchers.IO) {
                            LocationUtils.resolveAddressCoordinates(context, addr, allCinemas)
                        }
                        _uiState.value = _uiState.value.copy(
                            locationMode = LocationFilterMode.HOME_LOCATION,
                            selectedCity = "",
                            homeAddress = addr,
                            homeLat = coords?.first,
                            homeLon = coords?.second,
                            locationStatusMessage = "Vị trí nhà: $addr"
                        )
                        recomputeFilteredGroups()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            locationMode = LocationFilterMode.NONE,
                            locationStatusMessage = "Chưa có địa chỉ nhà trong hồ sơ cá nhân"
                        )
                    }
                }
            }
        }
    }

    fun loadShowtimes(movieId: Long, date: String? = null) {
        val queryDate = date ?: _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            // 1. Lấy danh sách rạp từ RAM/Disk cache trước (0ms), nếu chưa có thì tải ngầm song song
            if (allCinemas.isEmpty()) {
                allCinemas = CinemaRepositoryImpl.getCachedCinemasSnapshot()
            }
            if (allCinemas.isEmpty() && getCinemasUseCase != null) {
                launch {
                    val loadedCinemas = mutableListOf<Cinema>()
                    var p = 0
                    while (p < 10) {
                        var pageItems: List<Cinema> = emptyList()
                        getCinemasUseCase.invoke(q = null, city = null, status = "ACTIVE", page = p).collect { r ->
                            if (r is Resource.Success) {
                                pageItems = r.data.orEmpty()
                            }
                        }
                        if (pageItems.isEmpty()) break
                        loadedCinemas.addAll(pageItems)
                        allCinemas = loadedCinemas.distinctBy { it.cinemaId }
                        recomputeFilteredGroups()
                        if (pageItems.size < 10) break
                        p++
                    }
                }
            }

            // 2. Tải suất chiếu của phim trong ngày (hiển thị ngay khi có trang 0)
            val combinedShowtimes = mutableListOf<Showtime>()
            var lastError: String? = null
            var page = 0
            while (page < 8) {
                var pageItems: List<Showtime> = emptyList()
                var hadError = false
                getShowtimesUseCase(movieId = movieId, date = queryDate, page = page).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> {
                            pageItems = resource.data.orEmpty()
                        }
                        is Resource.Error -> {
                            lastError = resource.message
                            hadError = true
                        }
                    }
                }
                if (hadError && page == 0) break
                if (pageItems.isEmpty()) break
                combinedShowtimes.addAll(pageItems)
                rawShowtimes = combinedShowtimes.distinctBy { it.showtimeId }

                val cinemaMap = allCinemas.associateBy { it.cinemaId }
                val brandsFromShowtimes = rawShowtimes.mapNotNull { st ->
                    val c = cinemaMap[st.cinemaId]
                    val b = c?.brandName?.trim().orEmpty()
                    if (b.isNotBlank()) b else st.cinemaName.trim().split(" ").firstOrNull()?.ifBlank { null }
                }.distinct()

                val cities = allCinemas.map { it.city.trim() }.filter { it.isNotBlank() }.distinct().sorted()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableBrands = (listOf("Đề xuất") + brandsFromShowtimes).distinct(),
                    availableCities = cities,
                    errorMessage = null
                )
                recomputeFilteredGroups()

                if (pageItems.size < 10) break
                page++
            }

            if (rawShowtimes.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = lastError
                )
                recomputeFilteredGroups()
            }
        }
    }

    private fun recomputeFilteredGroups() {
        val state = _uiState.value
        val cinemaMap = allCinemas.associateBy { it.cinemaId }
        val filteredByDistance = MovieViewModel.filterAndSortCinemas(
            cinemas = allCinemas,
            normalizedQuery = "",
            locationMode = state.locationMode,
            selectedCity = state.selectedCity,
            currentLat = state.currentLat,
            currentLon = state.currentLon,
            homeLat = state.homeLat,
            homeLon = state.homeLon,
            homeAddress = state.homeAddress
        ).associateBy { it.cinema.cinemaId }

        val slotFiltered = rawShowtimes.filter { st ->
            state.selectedTimeSlot.matches(st.startTime)
        }

        val groups = slotFiltered
            .groupBy { it.cinemaId }
            .mapNotNull { (cinemaId, list) ->
                val cinemaObj = cinemaMap[cinemaId]
                val distItem = filteredByDistance[cinemaId]

                if (state.locationMode == LocationFilterMode.PROVINCE &&
                    state.selectedCity.isNotBlank() &&
                    allCinemas.isNotEmpty() &&
                    distItem == null
                ) {
                    return@mapNotNull null
                }

                val cName = cinemaObj?.name?.ifBlank { null }
                    ?: list.firstOrNull()?.cinemaName?.ifBlank { null }
                    ?: "Rạp CineOne"
                val bName = cinemaObj?.brandName?.ifBlank { null }
                    ?: cName.split(" ").firstOrNull().orEmpty()

                if (state.selectedBrand != "Đề xuất" && state.selectedBrand.isNotBlank()) {
                    val normBrand = LocationUtils.normalizeSearchText(state.selectedBrand)
                    val normTarget = LocationUtils.normalizeSearchText("$bName $cName")
                    if (!normTarget.contains(normBrand)) return@mapNotNull null
                }

                CinemaShowtimeGroupItem(
                    cinemaId = cinemaId,
                    cinemaName = cName,
                    brandName = bName,
                    address = cinemaObj?.address.orEmpty(),
                    city = cinemaObj?.city.orEmpty(),
                    distanceKm = distItem?.distanceKm,
                    showtimes = list.sortedBy { it.startTime }
                )
            }
            .sortedWith(
                compareBy<CinemaShowtimeGroupItem> { it.distanceKm ?: Double.MAX_VALUE }
                    .thenBy { it.cinemaName }
            )

        val groupedMap = groups.associate { it.cinemaName to it.showtimes }

        _uiState.value = state.copy(
            cinemaGroups = groups,
            showtimesByCinema = groupedMap
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                ShowtimeViewModel(
                    getShowtimesUseCase = app.useCaseModule.getShowtimesUseCase,
                    getCinemasUseCase = app.useCaseModule.getCinemasUseCase,
                    authRepository = app.repositoryModule.authRepository
                )
            }
        }
    }
}
