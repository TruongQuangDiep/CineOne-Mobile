package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.data.mapper.AiMessageMapper
import com.example.cineone.data.remote.ai.AiApi
import com.example.cineone.domain.model.AiClarificationOption
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiIntentType
import com.example.cineone.domain.model.AiMatchedCinemaShowtime
import com.example.cineone.domain.model.AiMessage
import com.example.cineone.domain.model.AiSender
import com.example.cineone.domain.model.AiSuggestedMovieCard
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.model.Showtime
import com.example.cineone.domain.repository.AiRepository
import com.example.cineone.domain.repository.CinemaRepository
import com.example.cineone.domain.repository.MovieRepository
import com.example.cineone.domain.repository.ShowtimeRepository
import com.example.cineone.ui.movie.MovieViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class AiRepositoryImpl(
    private val aiApi: AiApi,
    private val movieRepository: MovieRepository,
    private val cinemaRepository: CinemaRepository,
    private val showtimeRepository: ShowtimeRepository
) : AiRepository {

    override fun askAssistant(
        query: String,
        previousCriteria: AiExtractedCriteria?,
        currentLat: Double?,
        currentLon: Double?,
        homeLat: Double?,
        homeLon: Double?,
        homeAddress: String
    ): Flow<Resource<AiMessage>> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            emit(Resource.Error("Vui lòng nhập yêu cầu hoặc câu hỏi cho trợ lý CineOne."))
            return@flow
        }

        emit(Resource.Loading)

        try {
            val normalized = LocationUtils.normalizeSearchText(cleanQuery)

            // 1. Nếu là câu trả lời làm rõ khung giờ hoặc bộ lọc từ lượt hội thoại trước
            val isClarificationReply = previousCriteria?.requiresClarification == true &&
                isTimeOrFilterClarificationReply(normalized)

            if (!isClarificationReply && isFaqOrPolicyIntent(normalized)) {
                val faqMessage = handleFaqIntent(cleanQuery)
                emit(Resource.Success(faqMessage))
                return@flow
            }

            // 2. Tải danh sách phim và rạp thực tế từ Repository/Cache
            val movies = loadAvailableMovies()
            val cinemas = loadAvailableCinemas()

            if (!isClarificationReply && isReviewSummaryIntent(normalized)) {
                val reviewMessage = handleReviewSummaryIntent(cleanQuery, normalized, movies, previousCriteria)
                emit(Resource.Success(reviewMessage))
                return@flow
            }

            // 3. Xử lý tìm kiếm ngôn ngữ tự nhiên (Phim / Suất chiếu / Rạp)
            val searchMessage = handleNaturalLanguageSearch(
                rawQuery = cleanQuery,
                normalizedQuery = normalized,
                movies = movies,
                cinemas = cinemas,
                previousCriteria = previousCriteria,
                currentLat = currentLat,
                currentLon = currentLon,
                homeLat = homeLat,
                homeLon = homeLon,
                homeAddress = homeAddress
            )
            emit(Resource.Success(searchMessage))
        } catch (e: Exception) {
            emit(
                Resource.Error(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Không thể kết nối tới Trợ lý CineOne. Vui lòng thử lại sau."
                )
            )
        }
    }

    private suspend fun handleFaqIntent(question: String): AiMessage {
        val response = aiApi.askFaq(question)
        if (response.success && response.data != null) {
            val dto = response.data
            val answerText = dto.answer.trim().ifBlank {
                "Trợ lý CineOne chưa tìm thấy câu trả lời phù hợp trong cơ sở tri thức."
            }
            val sources = AiMessageMapper.mapFaqSources(dto)
            return AiMessage(
                id = UUID.randomUUID().toString(),
                sender = AiSender.ASSISTANT,
                content = answerText,
                timestamp = currentTimeFormatted(),
                intentType = AiIntentType.FAQ_POLICY,
                faqSources = sources,
                clarificationOptions = buildFollowUpFaqOptions(question)
            )
        } else {
            throw IllegalStateException(
                response.message ?: "Không thể tra cứu thông tin hỏi đáp & chính sách lúc này."
            )
        }
    }

    private suspend fun handleReviewSummaryIntent(
        rawQuery: String,
        normalizedQuery: String,
        movies: List<Movie>,
        previousCriteria: AiExtractedCriteria?
    ): AiMessage {
        var matchedMovie = findMovieInQuery(normalizedQuery, movies)

        if (matchedMovie == null) {
            val candidateTitle = extractCandidateMovieTitleFromReviewQuery(rawQuery)
            if (candidateTitle.isNotBlank()) {
                val queriedMovies = searchMoviesByKeyword(candidateTitle)
                matchedMovie = queriedMovies.firstOrNull()
            }
        }

        if (matchedMovie == null && !previousCriteria?.movieTitle.isNullOrBlank()) {
            val prevNorm = LocationUtils.normalizeSearchText(previousCriteria?.movieTitle.orEmpty())
            matchedMovie = movies.firstOrNull { m ->
                val titleNorm = LocationUtils.normalizeSearchText(m.title)
                titleNorm.contains(prevNorm) || prevNorm.contains(titleNorm)
            }
        }

        if (matchedMovie == null) {
            val candidateTitle = extractCandidateMovieTitleFromReviewQuery(rawQuery)
            val topMovies = movies
                .sortedWith(compareByDescending<Movie> { it.status == "NOW_SHOWING" }.thenByDescending { it.rating })
                .take(4)

            val options = topMovies.map { movie ->
                AiClarificationOption(
                    label = "Review: ${movie.title}",
                    queryToSend = "Tóm tắt review ${movie.title}"
                )
            }

            val explanation = if (candidateTitle.isNotBlank()) {
                "Hiện tại phim \"$candidateTitle\" chưa có trong danh mục phim đang phục vụ trên hệ thống CineOne. " +
                    if (options.isNotEmpty()) {
                        "Bạn có thể chọn một trong các bộ phim đang có trên hệ thống dưới đây để xem tóm tắt đánh giá từ khán giả:"
                    } else {
                        "Vui lòng thử lại với tên một bộ phim khác đang chiếu."
                    }
            } else {
                "Bạn muốn xem tóm tắt review cho bộ phim nào? Hãy chọn một bộ phim đang có trên hệ thống dưới đây:"
            }

            return AiMessage(
                id = UUID.randomUUID().toString(),
                sender = AiSender.ASSISTANT,
                content = explanation,
                timestamp = currentTimeFormatted(),
                intentType = AiIntentType.CLARIFICATION,
                clarificationOptions = options
            )
        }

        val response = aiApi.getMovieReviewSummary(matchedMovie.movieId)
        if (response.success && response.data != null) {
            val summaryModel = AiMessageMapper.mapReviewSummary(response.data, matchedMovie)
            val criteriaForMovie = AiExtractedCriteria(
                movieTitle = matchedMovie.title,
                date = LocalDate.now().toString(),
                dateLabel = "Hôm nay"
            )
            val suggestedCard = AiSuggestedMovieCard(
                movie = matchedMovie,
                matchedCinemas = emptyList(),
                appliedCriteria = criteriaForMovie
            )

            return AiMessage(
                id = UUID.randomUUID().toString(),
                sender = AiSender.ASSISTANT,
                content = "Dưới đây là tổng hợp đánh giá từ khán giả cho phim **${summaryModel.movieTitle}**:",
                timestamp = currentTimeFormatted(),
                intentType = AiIntentType.REVIEW_SUMMARY,
                extractedCriteria = criteriaForMovie,
                reviewSummary = summaryModel,
                suggestedMovies = listOf(suggestedCard)
            )
        } else {
            throw IllegalStateException(
                response.message ?: "Không thể tải tóm tắt đánh giá cho phim ${matchedMovie.title}."
            )
        }
    }

    private suspend fun handleNaturalLanguageSearch(
        rawQuery: String,
        normalizedQuery: String,
        movies: List<Movie>,
        cinemas: List<Cinema>,
        previousCriteria: AiExtractedCriteria?,
        currentLat: Double?,
        currentLon: Double?,
        homeLat: Double?,
        homeLon: Double?,
        homeAddress: String
    ): AiMessage {
        val localExtracted = extractCriteriaLocally(
            rawQuery = rawQuery,
            normalizedQuery = normalizedQuery,
            movies = movies,
            cinemas = cinemas,
            previousCriteria = previousCriteria
        )

        val remoteExtracted = runCatching {
            val resp = aiApi.extractSearchCriteria(rawQuery)
            if (resp.success && resp.data != null) {
                AiMessageMapper.mapSearchCriteria(resp.data)
            } else {
                null
            }
        }.getOrNull()

        var mergedCriteria = if (remoteExtracted != null) {
            remoteExtracted.mergeWith(localExtracted)
        } else {
            localExtracted
        }

        val isClearTimeSlot = normalizedQuery.contains("tat ca khung gio") ||
            normalizedQuery.contains("moi khung gio") ||
            normalizedQuery.contains("bo loc gio")
        val isClearLocation = normalizedQuery.contains("bo loc khu vuc") ||
            normalizedQuery.contains("tat ca khu vuc") ||
            normalizedQuery.contains("moi khu vuc")

        val shouldMergeContext = previousCriteria != null && (
            previousCriteria.requiresClarification ||
                isClearTimeSlot ||
                isClearLocation ||
                isFollowUpRefinement(normalizedQuery, localExtracted, previousCriteria)
            )

        if (shouldMergeContext) {
            mergedCriteria = previousCriteria
                .copy(requiresClarification = false, clarificationQuestion = null)
                .mergeWith(mergedCriteria)
            if (isClearTimeSlot) {
                mergedCriteria = mergedCriteria.copy(
                    timeSlot = TimeSlotFilter.ALL,
                    timeRangeLabel = null
                )
            }
            if (isClearLocation) {
                mergedCriteria = mergedCriteria.copy(
                    locationMode = LocationFilterMode.NONE,
                    city = "",
                    locationLabel = null
                )
            }
        }

        // Kiểm tra nếu giờ giấc mơ hồ (VD: "khoảng 8 giờ" mà chưa rõ sáng hay tối)
        if (mergedCriteria.requiresClarification && !mergedCriteria.clarificationQuestion.isNullOrBlank()) {
            return AiMessage(
                id = UUID.randomUUID().toString(),
                sender = AiSender.ASSISTANT,
                content = mergedCriteria.clarificationQuestion,
                timestamp = currentTimeFormatted(),
                intentType = AiIntentType.CLARIFICATION,
                extractedCriteria = mergedCriteria,
                clarificationOptions = listOf(
                    AiClarificationOption(
                        label = "Buổi sáng (8:00 - 12:00)",
                        queryToSend = "Khung giờ buổi sáng 8:00 - 12:00"
                    ),
                    AiClarificationOption(
                        label = "Buổi tối (17:00 - 21:00)",
                        queryToSend = "Khung giờ buổi tối 17:00 - 21:00"
                    ),
                    AiClarificationOption(
                        label = "Tất cả khung giờ",
                        queryToSend = "Xem tất cả khung giờ trong ngày"
                    )
                )
            )
        }

        // Mặc định ngày hôm nay nếu người dùng tìm suất chiếu / khung giờ / rạp mà chưa nói rõ ngày
        val targetDate = mergedCriteria.date?.takeIf { it.isNotBlank() }
            ?: LocalDate.now().toString()
        val targetDateLabel = mergedCriteria.dateLabel?.takeIf { it.isNotBlank() }
            ?: formatFriendlyDateLabel(targetDate)

        val effectiveCriteria = mergedCriteria.copy(
            date = targetDate,
            dateLabel = targetDateLabel
        )

        // 1. Lọc danh sách rạp theo vị trí (GPS / Nhà tôi / Tỉnh thành) và thương hiệu / tên rạp
        val filteredCinemaItems = MovieViewModel.filterAndSortCinemas(
            cinemas = cinemas,
            normalizedQuery = "",
            locationMode = effectiveCriteria.locationMode,
            selectedCity = effectiveCriteria.city,
            currentLat = currentLat,
            currentLon = currentLon,
            homeLat = homeLat,
            homeLon = homeLon,
            homeAddress = homeAddress
        ).filter { item ->
            val c = item.cinema
            val cNorm = LocationUtils.normalizeSearchText("${c.brandName} ${c.name} ${c.address}")
            val matchesBrand = if (effectiveCriteria.brand.isNotBlank()) {
                cNorm.contains(LocationUtils.normalizeSearchText(effectiveCriteria.brand))
            } else {
                true
            }
            val matchesCinemaName = if (effectiveCriteria.cinemaName.isNotBlank()) {
                cNorm.contains(LocationUtils.normalizeSearchText(effectiveCriteria.cinemaName))
            } else {
                true
            }
            matchesBrand && matchesCinemaName
        }

        val matchingCinemaMap = filteredCinemaItems.associateBy { it.cinema.cinemaId }

        // 2. Lọc danh sách phim theo tên phim & thể loại
        val candidateMovies = movies.filter { movie ->
            val matchesTitle = if (!effectiveCriteria.movieTitle.isNullOrBlank()) {
                val targetNorm = LocationUtils.normalizeSearchText(effectiveCriteria.movieTitle)
                val titleNorm = LocationUtils.normalizeSearchText(movie.title)
                titleNorm.contains(targetNorm) || targetNorm.contains(titleNorm)
            } else {
                true
            }

            val matchesGenre = if (!effectiveCriteria.genre.isNullOrBlank()) {
                val genreNorm = LocationUtils.normalizeSearchText(effectiveCriteria.genre)
                MovieViewModel.splitGenres(movie.genres).any { g ->
                    val gNorm = LocationUtils.normalizeSearchText(g)
                    gNorm.contains(genreNorm) || genreNorm.contains(gNorm)
                }
            } else {
                true
            }

            matchesTitle && matchesGenre
        }.sortedWith(
            compareByDescending<Movie> { it.status == "NOW_SHOWING" }
                .thenByDescending { it.status == "SNEAK_SHOW" }
                .thenByDescending { it.rating }
        )

        // 3. Tải suất chiếu thực tế trong ngày targetDate
        val showtimesForDate = loadShowtimesForDate(
            date = targetDate,
            specificMovieId = if (!effectiveCriteria.movieTitle.isNullOrBlank() && candidateMovies.size == 1) {
                candidateMovies.first().movieId
            } else {
                null
            }
        )

        // Lọc suất chiếu theo khung giờ và rạp phù hợp
        val hasCinemaConstraint = effectiveCriteria.locationMode != LocationFilterMode.NONE ||
            effectiveCriteria.city.isNotBlank() ||
            effectiveCriteria.brand.isNotBlank() ||
            effectiveCriteria.cinemaName.isNotBlank()

        val slotFilteredShowtimes = showtimesForDate.filter { st ->
            val matchesSlot = effectiveCriteria.timeSlot.matches(st.startTime)
            val matchesCinema = if (hasCinemaConstraint && cinemas.isNotEmpty()) {
                val byId = matchingCinemaMap.containsKey(st.cinemaId)
                val byBrandText = if (!byId && effectiveCriteria.brand.isNotBlank()) {
                    LocationUtils.normalizeSearchText(st.cinemaName)
                        .contains(LocationUtils.normalizeSearchText(effectiveCriteria.brand))
                } else {
                    false
                }
                byId || byBrandText
            } else if (effectiveCriteria.brand.isNotBlank()) {
                LocationUtils.normalizeSearchText(st.cinemaName)
                    .contains(LocationUtils.normalizeSearchText(effectiveCriteria.brand))
            } else {
                true
            }
            matchesSlot && matchesCinema
        }

        val showtimesByMovieId = slotFilteredShowtimes.groupBy { it.movieId }

        // 4. Ghép nối Card phim gợi ý kèm rạp và suất chiếu phù hợp
        val suggestedCards = mutableListOf<AiSuggestedMovieCard>()

        val prioritizedMovies = if (showtimesByMovieId.isNotEmpty()) {
            val moviesWithShowtimes = candidateMovies.filter { showtimesByMovieId.containsKey(it.movieId) }
            if (moviesWithShowtimes.isNotEmpty()) {
                moviesWithShowtimes
            } else {
                candidateMovies
            }
        } else {
            candidateMovies
        }

        for (movie in prioritizedMovies.take(6)) {
            val movieShowtimes = showtimesByMovieId[movie.movieId].orEmpty()
            val matchedCinemaShowtimes = movieShowtimes
                .groupBy { it.cinemaId }
                .map { (cinemaId, stList) ->
                    val distItem = matchingCinemaMap[cinemaId]
                    val cinemaObj = distItem?.cinema ?: cinemas.firstOrNull { it.cinemaId == cinemaId }
                    val cName = cinemaObj?.name?.ifBlank { null }
                        ?: stList.firstOrNull()?.cinemaName?.ifBlank { null }
                        ?: "Rạp CineOne"
                    val bName = cinemaObj?.brandName?.ifBlank { null }
                        ?: cName.split(" ").firstOrNull().orEmpty()
                    AiMatchedCinemaShowtime(
                        cinemaId = cinemaId,
                        cinemaName = cName,
                        brandName = bName,
                        address = cinemaObj?.address.orEmpty(),
                        city = cinemaObj?.city.orEmpty(),
                        distanceKm = distItem?.distanceKm,
                        showtimes = stList.sortedBy { it.startTime }
                    )
                }
                .sortedWith(
                    compareBy<AiMatchedCinemaShowtime> { it.distanceKm ?: Double.MAX_VALUE }
                        .thenBy { it.cinemaName }
                )
                .take(3)

            suggestedCards.add(
                AiSuggestedMovieCard(
                    movie = movie,
                    matchedCinemas = matchedCinemaShowtimes,
                    appliedCriteria = effectiveCriteria
                )
            )
        }

        val matchedCinemasPreview = filteredCinemaItems.take(5).map { item ->
            AiMatchedCinemaShowtime(
                cinemaId = item.cinema.cinemaId,
                cinemaName = item.cinema.name,
                brandName = item.cinema.brandName,
                address = item.cinema.address,
                city = item.cinema.city,
                distanceKm = item.distanceKm,
                showtimes = emptyList()
            )
        }

        val cinemaFocusedQuery = isCinemaCentricQuery(normalizedQuery, effectiveCriteria)
        val displayCriteria = if (cinemaFocusedQuery && mergedCriteria.date.isNullOrBlank()) {
            effectiveCriteria.copy(date = null, dateLabel = null)
        } else {
            effectiveCriteria
        }
        val moviesWithShowtimesAtMatchedCinemas = suggestedCards.filter { it.matchedCinemas.isNotEmpty() }

        val finalSuggestedCards = if (cinemaFocusedQuery) {
            moviesWithShowtimesAtMatchedCinemas
        } else {
            suggestedCards
        }

        val shouldShowCinemasList = matchedCinemasPreview.isNotEmpty() && (
            cinemaFocusedQuery ||
                finalSuggestedCards.isEmpty() ||
                moviesWithShowtimesAtMatchedCinemas.isEmpty()
            )

        val responseText = buildSearchResponseSummary(
            criteria = displayCriteria,
            cinemaFocusedQuery = cinemaFocusedQuery,
            matchedCinemas = matchedCinemasPreview,
            totalMatchedCinemasCount = filteredCinemaItems.size,
            suggestedCards = finalSuggestedCards,
            matchedShowtimesCount = slotFilteredShowtimes.size,
            allCinemas = cinemas
        )

        val followUpClarifications = when {
            cinemaFocusedQuery && matchedCinemasPreview.isEmpty() -> {
                val sampleCities = cinemas.map { it.city.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(3)
                sampleCities.map { city ->
                    AiClarificationOption(
                        label = "Rạp tại $city",
                        queryToSend = "Có rạp chiếu phim nào ở $city không"
                    )
                }
            }
            !cinemaFocusedQuery && (finalSuggestedCards.isEmpty() || slotFilteredShowtimes.isEmpty()) -> {
                buildRelaxFilterOptions(
                    criteria = effectiveCriteria,
                    hasMatchingCinemasInArea = filteredCinemaItems.isNotEmpty()
                )
            }
            else -> emptyList()
        }

        return AiMessage(
            id = UUID.randomUUID().toString(),
            sender = AiSender.ASSISTANT,
            content = responseText,
            timestamp = currentTimeFormatted(),
            intentType = AiIntentType.SEARCH_SHOWTIME_OR_MOVIE,
            extractedCriteria = displayCriteria,
            suggestedMovies = finalSuggestedCards,
            matchedCinemasOnly = if (shouldShowCinemasList) matchedCinemasPreview else emptyList(),
            clarificationOptions = followUpClarifications
        )
    }

    private fun isCinemaCentricQuery(
        normalizedQuery: String,
        criteria: AiExtractedCriteria
    ): Boolean {
        if (!criteria.movieTitle.isNullOrBlank() || !criteria.genre.isNullOrBlank()) {
            return false
        }
        val cinemaQuestionKeywords = listOf(
            "co rap",
            "rap nao",
            "tim rap",
            "rap chieu phim",
            "danh sach rap",
            "rap o",
            "rap tai",
            "nhung rap",
            "cac rap",
            "may rap",
            "bao nhieu rap"
        )
        return cinemaQuestionKeywords.any { normalizedQuery.contains(it) }
    }

    private fun buildSearchResponseSummary(
        criteria: AiExtractedCriteria,
        cinemaFocusedQuery: Boolean,
        matchedCinemas: List<AiMatchedCinemaShowtime>,
        totalMatchedCinemasCount: Int,
        suggestedCards: List<AiSuggestedMovieCard>,
        matchedShowtimesCount: Int,
        allCinemas: List<Cinema> = emptyList()
    ): String {
        val filterSummary = criteria.activeFilterTags().joinToString(" • ")

        if (cinemaFocusedQuery) {
            val areaDesc = when {
                criteria.city.isNotBlank() -> " tại **${criteria.city}**"
                criteria.brand.isNotBlank() -> " thuộc hệ thống **${criteria.brand}**"
                criteria.locationMode == LocationFilterMode.CURRENT_LOCATION -> " **gần vị trí của bạn**"
                criteria.locationMode == LocationFilterMode.HOME_LOCATION -> " **gần vị trí nhà bạn**"
                else -> ""
            }
            if (matchedCinemas.isEmpty()) {
                val availableProvinces = allCinemas.map { it.city.trim() }.filter { it.isNotBlank() }.distinct()
                val hint = if (availableProvinces.isNotEmpty()) {
                    " Bạn có thể tra cứu các tỉnh/thành đang có cụm rạp trên hệ thống như: **${availableProvinces.take(5).joinToString(", ")}**."
                } else {
                    ""
                }
                return "Hiện tại trên hệ thống CineOne chưa có rạp chiếu phim nào$areaDesc.$hint"
            }
            return buildString {
                val isDistanceSort = criteria.locationMode == LocationFilterMode.CURRENT_LOCATION ||
                    criteria.locationMode == LocationFilterMode.HOME_LOCATION
                if (isDistanceSort && criteria.city.isBlank()) {
                    append("Gợi ý **${matchedCinemas.size} rạp chiếu phim**$areaDesc nhất (sắp xếp theo khoảng cách):\n")
                } else if (totalMatchedCinemasCount > matchedCinemas.size) {
                    append(
                        "Tìm thấy **$totalMatchedCinemasCount rạp chiếu phim**$areaDesc " +
                            "(hiển thị **${matchedCinemas.size} rạp tiêu biểu** — bấm **Mở bộ lọc trên màn Tìm kiếm** để xem toàn bộ):\n"
                    )
                } else {
                    append("Tìm thấy **$totalMatchedCinemasCount rạp chiếu phim**$areaDesc:\n")
                }
                matchedCinemas.forEach { c ->
                    val distStr = LocationUtils.formatDistanceKm(c.distanceKm)
                    append("• **${c.cinemaName}**")
                    if (distStr.isNotBlank()) append(" ($distStr)")
                    if (c.address.isNotBlank()) append(" — ${c.address}")
                    append("\n")
                }
                append("Bạn có thể nhấn trực tiếp vào rạp bên dưới để xem chi tiết rạp và lịch chiếu.")
            }
        }

        if (suggestedCards.isEmpty()) {
            return if (filterSummary.isNotBlank()) {
                "Hiện chưa tìm thấy bộ phim nào khớp hoàn toàn với bộ lọc ($filterSummary). Bạn có thể nới lỏng điều kiện lọc hoặc mở màn hình Tìm kiếm để xem thêm."
            } else {
                "Hiện chưa tìm thấy phim phù hợp với yêu cầu của bạn. Vui lòng thử từ khóa hoặc tiêu chí khác."
            }
        }

        val moviesWithMatchingShowtimes = suggestedCards.count { it.matchedCinemas.isNotEmpty() }
        return buildString {
            if (moviesWithMatchingShowtimes > 0) {
                append("Tìm thấy **$moviesWithMatchingShowtimes phim** có suất chiếu phù hợp")
                if (filterSummary.isNotBlank()) {
                    append(" theo tiêu chí: $filterSummary.")
                } else {
                    append(".")
                }
                append("\nBạn có thể nhấn trực tiếp vào giờ chiếu hoặc chọn **Đặt vé** để giữ nguyên bộ lọc.")
            } else {
                append("Gợi ý **${suggestedCards.size} bộ phim** phù hợp")
                if (!criteria.genre.isNullOrBlank()) {
                    append(" thể loại **${criteria.genre}**")
                }
                if (!criteria.dateLabel.isNullOrBlank() && matchedShowtimesCount == 0) {
                    if (criteria.city.isNotBlank() && matchedCinemas.isEmpty()) {
                        append(" (hiện hệ thống chưa có cụm rạp tại **${criteria.city}**, bạn có thể chọn **Bỏ lọc khu vực ${criteria.city}** bên dưới hoặc nhấn **Đặt vé** trên thẻ phim để xem lịch chiếu tại các tỉnh/thành khác).")
                    } else {
                        append(" (hiện chưa có lịch chiếu khớp khung giờ tại ${criteria.dateLabel}")
                        if (criteria.brand.isNotBlank()) append(" ở rạp ${criteria.brand}")
                        if (matchedCinemas.isNotEmpty() &&
                            (criteria.locationMode != LocationFilterMode.NONE || criteria.city.isNotBlank())
                        ) {
                            val isDist = criteria.locationMode == LocationFilterMode.CURRENT_LOCATION ||
                                criteria.locationMode == LocationFilterMode.HOME_LOCATION
                            if (isDist && criteria.city.isBlank()) {
                                append(", kèm ${matchedCinemas.size} rạp gần bạn nhất bên dưới")
                            } else {
                                append(", đã tìm thấy $totalMatchedCinemasCount rạp phù hợp bên dưới")
                            }
                        }
                        append(" — nhấn **Đặt vé** trên thẻ phim để xem các ngày/khung giờ khác).")
                    }
                } else {
                    append(". Nhấn **Đặt vé** để chọn rạp và suất chiếu cùng bộ lọc đã trích xuất.")
                }
            }
        }
    }

    private fun buildRelaxFilterOptions(
        criteria: AiExtractedCriteria,
        hasMatchingCinemasInArea: Boolean = true
    ): List<AiClarificationOption> {
        val options = mutableListOf<AiClarificationOption>()
        if (criteria.city.isNotBlank() && !hasMatchingCinemasInArea) {
            options.add(
                AiClarificationOption(
                    label = "Bỏ lọc khu vực ${criteria.city}",
                    queryToSend = "Bỏ lọc khu vực, giữ bộ lọc phim đang chọn"
                )
            )
            options.add(
                AiClarificationOption(
                    label = "Đổi sang rạp gần tôi",
                    queryToSend = "Đổi sang tìm ở rạp gần tôi"
                )
            )
        }
        if (criteria.timeSlot != TimeSlotFilter.ALL) {
            options.add(
                AiClarificationOption(
                    label = "Xem tất cả khung giờ",
                    queryToSend = "Xem tất cả khung giờ trong ngày"
                )
            )
        }
        val todayStr = LocalDate.now().toString()
        if (criteria.date != todayStr) {
            options.add(
                AiClarificationOption(
                    label = "Đổi sang hôm nay",
                    queryToSend = "Đổi sang hôm nay"
                )
            )
        }
        val tomorrowStr = LocalDate.now().plusDays(1).toString()
        if (criteria.date != tomorrowStr) {
            options.add(
                AiClarificationOption(
                    label = "Đổi sang ngày mai",
                    queryToSend = "Đổi sang ngày mai"
                )
            )
        }
        return options.take(3)
    }

    private fun buildFollowUpFaqOptions(currentQuestion: String): List<AiClarificationOption> {
        val norm = LocationUtils.normalizeSearchText(currentQuestion)
        val allFaqPrompts = listOf(
            AiClarificationOption("Quy định đổi/ hủy vé", "Quy định đổi/ hủy vé tại CineOne như thế nào?"),
            AiClarificationOption("Chính sách thành viên VIP", "Quyền lợi và điều kiện hạng thành viên VIP tại CineOne?"),
            AiClarificationOption("Hướng dẫn check-in", "Hướng dẫn check-in lấy vé bằng mã QR tại rạp CineOne?")
        )
        return allFaqPrompts.filterNot { opt ->
            norm.contains(LocationUtils.normalizeSearchText(opt.label))
        }
    }

    private fun extractCriteriaLocally(
        rawQuery: String,
        normalizedQuery: String,
        movies: List<Movie>,
        cinemas: List<Cinema>,
        previousCriteria: AiExtractedCriteria?
    ): AiExtractedCriteria {
        val today = LocalDate.now()

        // 1. Trích xuất Ngày chiếu
        val (extractedDate, extractedDateLabel) = extractDateFromQuery(normalizedQuery, today)

        // 2. Trích xuất Khung giờ & kiểm tra giờ giấc mơ hồ
        val timeExtraction = extractTimeSlotFromQuery(normalizedQuery)

        // 3. Trích xuất Hãng rạp & Rạp cụ thể trước để hỗ trợ nhận diện vị trí rạp
        val (brand, cinemaName) = extractBrandAndCinema(normalizedQuery, cinemas)

        // 4. Trích xuất Vị trí (Gần nhà / Gần tôi / Tỉnh thành)
        val (locationMode, city, locationLabel) = extractLocationFromQuery(
            normalized = normalizedQuery,
            cinemas = cinemas,
            matchedCinemaName = cinemaName,
            matchedBrand = brand
        )

        // 5. Trích xuất Thể loại phim
        val genre = extractGenreFromQuery(normalizedQuery, movies)

        // 6. Trích xuất Tên phim
        val matchedMovie = findMovieInQuery(normalizedQuery, movies)
        val movieTitle = matchedMovie?.title

        // Nếu người dùng trả lời câu hỏi làm rõ giờ ("tất cả khung giờ")
        val resolvedSlot = if (normalizedQuery.contains("tat ca khung gio") || normalizedQuery.contains("moi khung gio")) {
            TimeSlotFilter.ALL
        } else {
            timeExtraction.slot
        }

        return AiExtractedCriteria(
            movieTitle = movieTitle,
            genre = genre,
            date = extractedDate,
            dateLabel = extractedDateLabel,
            timeSlot = resolvedSlot,
            timeRangeLabel = timeExtraction.label,
            locationMode = locationMode,
            city = city,
            locationLabel = locationLabel,
            brand = brand,
            cinemaName = cinemaName,
            requiresClarification = timeExtraction.requiresClarification && previousCriteria?.requiresClarification != true,
            clarificationQuestion = timeExtraction.clarificationQuestion
        )
    }

    private data class TimeExtractionResult(
        val slot: TimeSlotFilter = TimeSlotFilter.ALL,
        val label: String? = null,
        val requiresClarification: Boolean = false,
        val clarificationQuestion: String? = null
    )

    private fun extractTimeSlotFromQuery(normalized: String): TimeExtractionResult {
        if (normalized.contains("8:00 - 12:00") || normalized.contains("8h - 12h")) {
            return TimeExtractionResult(TimeSlotFilter.SLOT_8_12, TimeSlotFilter.SLOT_8_12.label)
        }
        if (normalized.contains("12:00 - 17:00") || normalized.contains("12h - 17h")) {
            return TimeExtractionResult(TimeSlotFilter.SLOT_12_17, TimeSlotFilter.SLOT_12_17.label)
        }
        if (normalized.contains("17:00 - 21:00") || normalized.contains("17h - 21h")) {
            return TimeExtractionResult(TimeSlotFilter.SLOT_17_21, TimeSlotFilter.SLOT_17_21.label)
        }
        if (normalized.contains("21:00 - 24:00") || normalized.contains("21h - 24h")) {
            return TimeExtractionResult(TimeSlotFilter.SLOT_21_24, TimeSlotFilter.SLOT_21_24.label)
        }

        val hasMorning = normalized.contains("buoi sang") || normalized.contains("sang nay") ||
            normalized.contains("sang mai") || Regex("""\bsang\b""").containsMatchIn(normalized) &&
            !normalized.contains("doi sang") && !normalized.contains("chuyen sang")
        val hasAfternoon = normalized.contains("buoi chieu") || normalized.contains("chieu nay") ||
            normalized.contains("chieu mai") || normalized.contains("chieu kia") ||
            normalized.contains("chieu thu") || normalized.contains("chieu chu nhat") ||
            normalized.contains("chieu cuoi tuan") || normalized.contains("vao chieu") ||
            normalized.contains("buoi trua") || normalized.contains("trua nay") ||
            normalized.contains("trua mai")
        val hasEvening = normalized.contains("toi nay") || normalized.contains("toi mai") ||
            normalized.contains("buoi toi") || Regex("""\btoi\b""").containsMatchIn(normalized) &&
            !normalized.contains("gan toi") && !normalized.contains("nha toi") && !normalized.contains("cua toi") ||
            normalized.contains("toi mai") || normalized.contains("toi nay")
        val hasNight = normalized.contains("dem") || normalized.contains("khuya")

        // Kiểm tra giờ cụ thể (VD: "lúc 8 giờ", "khoảng 20h", "19:30")
        val hourRegex = Regex("""(?:luc|khoang| tầm|tam|vao|gio|khung)?\s*(\d{1,2})\s*(?:gio|h|:(\d{2}))""")
        val match = hourRegex.find(normalized)
        if (match != null) {
            val rawHour = match.groupValues[1].toIntOrNull()
            if (rawHour != null && rawHour in 0..23) {
                val isAmbiguousHour = rawHour in 6..11 && !hasMorning && !hasAfternoon && !hasEvening && !hasNight
                if (isAmbiguousHour) {
                    return TimeExtractionResult(
                        slot = TimeSlotFilter.ALL,
                        requiresClarification = true,
                        clarificationQuestion = "Bạn muốn tìm suất chiếu khoảng $rawHour giờ sáng hay $rawHour giờ tối?"
                    )
                }

                val effectiveHour = when {
                    (hasEvening || hasNight || hasAfternoon) && rawHour in 1..11 -> rawHour + 12
                    else -> rawHour
                }
                val slot = when {
                    effectiveHour in 6..11 -> TimeSlotFilter.SLOT_8_12
                    effectiveHour in 12..16 -> TimeSlotFilter.SLOT_12_17
                    effectiveHour in 17..20 -> TimeSlotFilter.SLOT_17_21
                    effectiveHour >= 21 || effectiveHour < 6 -> TimeSlotFilter.SLOT_21_24
                    else -> TimeSlotFilter.ALL
                }
                return TimeExtractionResult(slot = slot, label = slot.label)
            }
        }

        // Kiểm tra từ khóa buổi
        return when {
            normalized.contains("toi mai") || normalized.contains("toi nay") ||
                normalized.contains("buoi toi") || hasEvening -> {
                TimeExtractionResult(TimeSlotFilter.SLOT_17_21, TimeSlotFilter.SLOT_17_21.label)
            }
            hasNight -> TimeExtractionResult(TimeSlotFilter.SLOT_21_24, TimeSlotFilter.SLOT_21_24.label)
            hasMorning -> TimeExtractionResult(TimeSlotFilter.SLOT_8_12, TimeSlotFilter.SLOT_8_12.label)
            hasAfternoon -> TimeExtractionResult(TimeSlotFilter.SLOT_12_17, TimeSlotFilter.SLOT_12_17.label)
            else -> TimeExtractionResult(TimeSlotFilter.ALL, null)
        }
    }

    private fun extractDateFromQuery(normalized: String, today: LocalDate): Pair<String?, String?> {
        val displayFormatter = DateTimeFormatter.ofPattern("dd/MM")

        if (normalized.contains("hom nay") || normalized.contains("toi nay") ||
            normalized.contains("chieu nay") || normalized.contains("sang nay") ||
            normalized.contains("dem nay") || normalized.contains("trua nay")
        ) {
            val d = today
            return d.toString() to "Hôm nay (${d.format(displayFormatter)})"
        }

        if (normalized.contains("ngay mai") || normalized.contains("toi mai") ||
            normalized.contains("sang mai") || normalized.contains("chieu mai") ||
            normalized.contains("dem mai") || Regex("""\bmai\b""").containsMatchIn(normalized)
        ) {
            val d = today.plusDays(1)
            return d.toString() to "Ngày mai (${d.format(displayFormatter)})"
        }

        if (normalized.contains("ngay kia") || normalized.contains("ngay mot")) {
            val d = today.plusDays(2)
            return d.toString() to "Ngày kia (${d.format(displayFormatter)})"
        }

        if (normalized.contains("cuoi tuan")) {
            val d = if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) {
                today
            } else {
                today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            }
            return d.toString() to "Cuối tuần (${d.format(displayFormatter)})"
        }

        val dayOfWeekMapping = listOf(
            "thu 2" to DayOfWeek.MONDAY,
            "thu hai" to DayOfWeek.MONDAY,
            "thu 3" to DayOfWeek.TUESDAY,
            "thu ba" to DayOfWeek.TUESDAY,
            "thu 4" to DayOfWeek.WEDNESDAY,
            "thu tu" to DayOfWeek.WEDNESDAY,
            "thu 5" to DayOfWeek.THURSDAY,
            "thu nam" to DayOfWeek.THURSDAY,
            "thu 6" to DayOfWeek.FRIDAY,
            "thu sau" to DayOfWeek.FRIDAY,
            "thu 7" to DayOfWeek.SATURDAY,
            "thu bay" to DayOfWeek.SATURDAY,
            "chu nhat" to DayOfWeek.SUNDAY
        )
        for ((token, dow) in dayOfWeekMapping) {
            if (normalized.contains(token)) {
                val d = today.with(TemporalAdjusters.nextOrSame(dow))
                return d.toString() to d.format(displayFormatter)
            }
        }

        val ddMmRegex = Regex("""\b(\d{1,2})[/-](\d{1,2})(?:[/-](\d{4}))?\b""")
        val match = ddMmRegex.find(normalized)
        if (match != null) {
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val year = match.groupValues[3].toIntOrNull() ?: today.year
            if (day != null && month != null && month in 1..12 && day in 1..31) {
                val parsed = runCatching { LocalDate.of(year, month, day) }.getOrNull()
                if (parsed != null) {
                    return parsed.toString() to parsed.format(displayFormatter)
                }
            }
        }

        return null to null
    }

    private fun extractLocationFromQuery(
        normalized: String,
        cinemas: List<Cinema>,
        matchedCinemaName: String = "",
        matchedBrand: String = ""
    ): Triple<LocationFilterMode, String, String?> {
        if (normalized.contains("gan nha") || normalized.contains("nha toi") ||
            normalized.contains("dia chi nha") || normalized.contains("o nha")
        ) {
            return Triple(LocationFilterMode.HOME_LOCATION, "", "Gần vị trí nhà tôi")
        }

        if (normalized.contains("gan toi") || normalized.contains("gan day") ||
            normalized.contains("quanh day") || normalized.contains("vi tri hien tai") ||
            normalized.contains("gan ban")
        ) {
            return Triple(LocationFilterMode.CURRENT_LOCATION, "", "Gần vị trí hiện tại")
        }

        // Nếu đã khớp chính xác tên 1 rạp cụ thể trong hệ thống thì ưu tiên lọc theo rạp đó (không ép thêm khu vực giả)
        if (matchedCinemaName.isNotBlank()) {
            return Triple(LocationFilterMode.NONE, "", null)
        }

        val catalogCities = cinemas.map { it.city.trim() }.filter { it.isNotBlank() }.distinct()
        val cleanNormalized = normalized
            .replace(Regex("[.,\\-_/]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        for (city in catalogCities.sortedByDescending { it.length }) {
            val cityNorm = LocationUtils.normalizeSearchText(city)
            val cityKey = LocationUtils.normalizeCityKey(city)
            if (cityNorm.isNotBlank() && Regex("""\b${Regex.escape(cityNorm)}\b""").containsMatchIn(normalized)) {
                return Triple(LocationFilterMode.PROVINCE, city, "Khu vực: $city")
            }
            if (cityKey.isNotBlank() && Regex("""\b${Regex.escape(cityKey)}\b""").containsMatchIn(cleanNormalized)) {
                return Triple(LocationFilterMode.PROVINCE, city, "Khu vực: $city")
            }
        }

        val cityAliases = mapOf(
            "ha noi" to "Hà Nội",
            "hanoi" to "Hà Nội",
            "hn" to "Hà Nội",
            "ho chi minh" to "Hồ Chí Minh",
            "tp hcm" to "Hồ Chí Minh",
            "tphcm" to "Hồ Chí Minh",
            "hcm" to "Hồ Chí Minh",
            "sai gon" to "Hồ Chí Minh",
            "saigon" to "Hồ Chí Minh",
            "da nang" to "Đà Nẵng",
            "hai phong" to "Hải Phòng",
            "can tho" to "Cần Thơ",
            "an giang" to "An Giang",
            "long xuyen" to "An Giang",
            "chau doc" to "An Giang",
            "kien giang" to "Kiên Giang",
            "rach gia" to "Kiên Giang",
            "gach gia" to "Kiên Giang",
            "phu quoc" to "Kiên Giang",
            "dong thap" to "Đồng Tháp",
            "cao lanh" to "Đồng Tháp",
            "sa dec" to "Đồng Tháp",
            "long an" to "Long An",
            "tan an" to "Long An",
            "tien giang" to "Tiền Giang",
            "my tho" to "Tiền Giang",
            "ben tre" to "Bến Tre",
            "vinh long" to "Vĩnh Long",
            "tra vinh" to "Trà Vinh",
            "hau giang" to "Hậu Giang",
            "vi thanh" to "Hậu Giang",
            "soc trang" to "Sóc Trăng",
            "bac lieu" to "Bạc Liêu",
            "ca mau" to "Cà Mau",
            "tay ninh" to "Tây Ninh",
            "binh phuoc" to "Bình Phước",
            "dong xoai" to "Bình Phước",
            "bien hoa" to "Đồng Nai",
            "dong nai" to "Đồng Nai",
            "binh duong" to "Bình Dương",
            "thu dau mot" to "Bình Dương",
            "di an" to "Bình Dương",
            "thuan an" to "Bình Dương",
            "vung tau" to "Bà Rịa - Vũng Tàu",
            "ba ria" to "Bà Rịa - Vũng Tàu",
            "brvt" to "Bà Rịa - Vũng Tàu",
            "da lat" to "Lâm Đồng",
            "lam dong" to "Lâm Đồng",
            "bao loc" to "Lâm Đồng",
            "dak lak" to "Đắk Lắk",
            "buon ma thuot" to "Đắk Lắk",
            "dak nong" to "Đắk Nông",
            "gia nghia" to "Đắk Nông",
            "gia lai" to "Gia Lai",
            "pleiku" to "Gia Lai",
            "kon tum" to "Kon Tum",
            "binh thuan" to "Bình Thuận",
            "phan thiet" to "Bình Thuận",
            "ninh thuan" to "Ninh Thuận",
            "phan rang" to "Ninh Thuận",
            "nha trang" to "Khánh Hòa",
            "khanh hoa" to "Khánh Hòa",
            "cam ranh" to "Khánh Hòa",
            "phu yen" to "Phú Yên",
            "tuy hoa" to "Phú Yên",
            "binh dinh" to "Bình Định",
            "quy nhon" to "Bình Định",
            "quang ngai" to "Quảng Ngãi",
            "quang nam" to "Quảng Nam",
            "tam ky" to "Quảng Nam",
            "hoi an" to "Quảng Nam",
            "hue" to "Thừa Thiên Huế",
            "thua thien hue" to "Thừa Thiên Huế",
            "quang tri" to "Quảng Trị",
            "dong ha" to "Quảng Trị",
            "quang binh" to "Quảng Bình",
            "dong hoi" to "Quảng Bình",
            "ha tinh" to "Hà Tĩnh",
            "nghe an" to "Nghệ An",
            "tp vinh" to "Nghệ An",
            "thanh hoa" to "Thanh Hóa",
            "ninh binh" to "Ninh Bình",
            "nam dinh" to "Nam Định",
            "thai binh" to "Thái Bình",
            "ha nam" to "Hà Nam",
            "phu ly" to "Hà Nam",
            "hung yen" to "Hưng Yên",
            "hai duong" to "Hải Dương",
            "bac giang" to "Bắc Giang",
            "bac ninh" to "Bắc Ninh",
            "quang ninh" to "Quảng Ninh",
            "ha long" to "Quảng Ninh",
            "cam pha" to "Quảng Ninh",
            "uong bi" to "Quảng Ninh",
            "mong cai" to "Quảng Ninh",
            "vinh phuc" to "Vĩnh Phúc",
            "vinh yen" to "Vĩnh Phúc",
            "phu tho" to "Phú Thọ",
            "viet tri" to "Phú Thọ",
            "thai nguyen" to "Thái Nguyên",
            "tuyen quang" to "Tuyên Quang",
            "yen bai" to "Yên Bái",
            "lao cai" to "Lào Cai",
            "sapa" to "Lào Cai",
            "sa pa" to "Lào Cai",
            "dien bien" to "Điện Biên",
            "lai chau" to "Lai Châu",
            "son la" to "Sơn La",
            "hoa binh" to "Hòa Bình",
            "lang son" to "Lạng Sơn",
            "cao bang" to "Cao Bằng",
            "bac kan" to "Bắc Kạn",
            "ha giang" to "Hà Giang"
        )
        for ((alias, canonical) in cityAliases) {
            if (Regex("""\b$alias\b""").containsMatchIn(cleanNormalized)) {
                val canonicalKey = LocationUtils.normalizeCityKey(canonical)
                val matchedCatalogCity = catalogCities.firstOrNull { c ->
                    LocationUtils.normalizeCityKey(c) == canonicalKey
                } ?: canonical
                return Triple(LocationFilterMode.PROVINCE, matchedCatalogCity, "Khu vực: $matchedCatalogCity")
            }
        }

        // Bắt cụm địa danh sau từ khóa "o / tai / khu vuc / tinh / thanh pho" nếu người dùng hỏi địa điểm bất kỳ
        if (matchedBrand.isBlank()) {
            val placeRegex = Regex("""\b(?:o|tai|khu vuc|tinh|thanh pho)\s+(?:rap\s+)?([a-z0-9\s]{3,25}?)(?:\s+(?:khong|ko|chua|vay|nhi|ha|the|nao|di|nhe|nha|voi|giup|dum|xem|coi|a|ak|luon)|$)""")
            val placeMatch = placeRegex.find(cleanNormalized)
            if (placeMatch != null) {
                val rawPlace = placeMatch.groupValues[1]
                    .replace(Regex("""\s+(?:khong|ko|chua|vay|nhi|ha|the|nao|di|nhe|nha|voi|giup|dum|xem|coi|a|ak|luon)$"""), "")
                    .trim()
                val ignoredTokens = setOf(
                    "gan day", "gan toi", "nha toi", "dau", "day", "rap", "he thong", "cineone",
                    "cgv", "beta", "lotte", "galaxy", "bhd", "cinestar", "megags"
                )
                val hasBrandWord = ignoredTokens.any { token ->
                    Regex("""\b$token\b""").containsMatchIn(rawPlace)
                }
                if (rawPlace.isNotBlank() && !ignoredTokens.contains(rawPlace) && !hasBrandWord) {
                    val displayPlace = rawPlace.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
                    return Triple(LocationFilterMode.PROVINCE, displayPlace, "Khu vực: $displayPlace")
                }
            }
        }

        return Triple(LocationFilterMode.NONE, "", null)
    }

    private fun extractBrandAndCinema(
        normalized: String,
        cinemas: List<Cinema>
    ): Pair<String, String> {
        val phoneticQuery = normalized.replace("gach gia", "rach gia")
        val sortedCinemas = cinemas.sortedByDescending { it.name.length }
        for (cinema in sortedCinemas) {
            val nameNorm = LocationUtils.normalizeSearchText(cinema.name)
            if (nameNorm.length >= 5 && phoneticQuery.contains(nameNorm)) {
                val brand = cinema.brandName.ifBlank {
                    cinema.name.split(" ").firstOrNull().orEmpty()
                }
                return brand to cinema.name
            }
            // Hỗ trợ người dùng gõ tắt tên rạp bỏ qua chữ "Vincom / Plaza / Center / Mall / TTTM" (VD: "cgv rạch giá" -> "CGV Vincom Rạch Giá")
            val shortCinemaNorm = nameNorm
                .replace(Regex("""\b(vincom|mega mall|plaza|center|sense city|tttm|mall)\b"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (shortCinemaNorm.length >= 6 && shortCinemaNorm != nameNorm && phoneticQuery.contains(shortCinemaNorm)) {
                val brand = cinema.brandName.ifBlank {
                    cinema.name.split(" ").firstOrNull().orEmpty()
                }
                return brand to cinema.name
            }
        }

        val knownBrands = listOf(
            "Beta" to listOf("beta", "rap beta", "beta cinemas"),
            "CGV" to listOf("cgv", "rap cgv"),
            "Lotte" to listOf("lotte", "rap lotte", "lotte cinema"),
            "Galaxy" to listOf("galaxy", "rap galaxy", "galaxy cinema"),
            "BHD" to listOf("bhd", "rap bhd", "bhd star"),
            "Cinestar" to listOf("cinestar", "rap cinestar"),
            "Mega GS" to listOf("mega gs", "megags")
        )
        for ((brandLabel, tokens) in knownBrands) {
            if (tokens.any { token -> Regex("""\b$token\b""").containsMatchIn(normalized) }) {
                return brandLabel to ""
            }
        }

        val catalogBrands = cinemas.map { it.brandName.trim() }.filter { it.isNotBlank() }.distinct()
        for (brand in catalogBrands) {
            val bNorm = LocationUtils.normalizeSearchText(brand)
            if (bNorm.length >= 2 && Regex("""\b$bNorm\b""").containsMatchIn(normalized)) {
                return brand to ""
            }
        }

        return "" to ""
    }

    private fun extractGenreFromQuery(
        normalized: String,
        movies: List<Movie>
    ): String? {
        val catalogGenres = MovieViewModel.extractGenresFromMovies(movies)
        for (g in catalogGenres.sortedByDescending { it.length }) {
            val gNorm = LocationUtils.normalizeSearchText(g)
            if (gNorm.length >= 3 && normalized.contains(gNorm)) {
                return g
            }
        }

        val commonGenres = listOf(
            "kinh di" to "Kinh dị",
            "hanh dong" to "Hành động",
            "tinh cam" to "Tình cảm",
            "lang man" to "Lãng mạn",
            "hoat hinh" to "Hoạt hình",
            "vien tuong" to "Khoa học viễn tưởng",
            "khoa hoc vien tuong" to "Khoa học viễn tưởng",
            "tam ly" to "Tâm lý",
            "phieu luu" to "Phiêu lưu",
            "gia dinh" to "Gia đình",
            "bi an" to "Bí ẩn",
            "hinh su" to "Hình sự",
            "chien tranh" to "Chiến tranh",
            "am nhac" to "Âm nhạc",
            "tai lieu" to "Tài liệu",
            "giat gan" to "Giật gân",
            "than thoai" to "Thần thoại",
            "lich su" to "Lịch sử",
            "chinh kich" to "Chính kịch",
            "hai huoc" to "Hài",
            "phim hai" to "Hài"
        )
        for ((token, label) in commonGenres) {
            if (normalized.contains(token)) {
                val matchedCatalog = catalogGenres.firstOrNull { cg ->
                    val cgNorm = LocationUtils.normalizeSearchText(cg)
                    cgNorm.contains(token) || LocationUtils.normalizeSearchText(label).contains(cgNorm)
                } ?: label
                return matchedCatalog
            }
        }
        return null
    }

    private fun findMovieInQuery(
        normalizedQuery: String,
        movies: List<Movie>
    ): Movie? {
        val sortedByLength = movies.sortedByDescending { it.title.length }
        for (movie in sortedByLength) {
            val titleNorm = LocationUtils.normalizeSearchText(movie.title)
            if (titleNorm.length >= 3 && normalizedQuery.contains(titleNorm)) {
                return movie
            }
            val subPart = movie.title.substringBefore(":").substringBefore("-").trim()
            val subNorm = LocationUtils.normalizeSearchText(subPart)
            if (subNorm.length >= 5 && normalizedQuery.contains(subNorm)) {
                return movie
            }
        }
        return null
    }

    private fun extractCandidateMovieTitleFromReviewQuery(rawQuery: String): String {
        val cleaned = rawQuery
            .replace(Regex("(?i)tóm tắt review phim|tóm tắt review|tom tat review|review phim|review|đánh giá phim|đánh giá|nhận xét phim|nhận xét"), "")
            .trim()
            .trim('"', '\'', '?', '.', ',', ':', '-')
            .trim()
        return cleaned
    }

    private fun isFaqOrPolicyIntent(normalized: String): Boolean {
        val faqKeywords = listOf(
            "quy dinh",
            "doi ve",
            "huy ve",
            "doi/ huy ve",
            "doi / huy ve",
            "hoan tien",
            "hoan ve",
            "check-in",
            "checkin",
            "check in",
            "huong dan",
            "chinh sach",
            "hoi dap",
            "faq",
            "hang thanh vien",
            "the thanh vien",
            "quyen loi vip",
            "uu dai vip",
            "diem thuong",
            "mang do an",
            "do tuoi",
            "ma qr",
            "soat ve"
        )
        if (faqKeywords.any { normalized.contains(it) }) return true
        if (normalized == "vip" || normalized.startsWith("vip ") || normalized.endsWith(" vip")) return true
        return false
    }

    private fun isReviewSummaryIntent(normalized: String): Boolean {
        val reviewKeywords = listOf(
            "review",
            "tom tat review",
            "danh gia phim",
            "nhan xet phim",
            "khen che",
            "co hay khong",
            "binh luan phim"
        )
        return reviewKeywords.any { normalized.contains(it) }
    }

    private fun isTimeOrFilterClarificationReply(normalized: String): Boolean {
        return normalized.contains("buoi sang") ||
            normalized.contains("buoi chieu") ||
            normalized.contains("buoi toi") ||
            normalized.contains("tat ca khung gio") ||
            normalized.contains("8:00") ||
            normalized.contains("12:00") ||
            normalized.contains("17:00") ||
            normalized.contains("21:00") ||
            normalized.contains("sang") ||
            normalized.contains("toi") ||
            normalized.contains("chieu")
    }

    private fun isFollowUpRefinement(
        normalizedQuery: String,
        extracted: AiExtractedCriteria,
        previousCriteria: AiExtractedCriteria
    ): Boolean {
        if (!extracted.hasAnyFilter()) return false
        val followUpPrefixes = listOf(
            "doi sang",
            "chon khung gio",
            "khung gio",
            "the con",
            "vay con",
            "con o",
            "con tai",
            "neu o",
            "thay bang",
            "o rap",
            "tai rap",
            "buoi sang",
            "buoi chieu",
            "buoi toi",
            "tat ca khung gio",
            "bo loc"
        )
        if (followUpPrefixes.any { normalizedQuery.contains(it) }) return true

        // Nếu câu trước người dùng đang tìm một bộ phim hoặc thể loại cụ thể,
        // và câu tiếp theo chỉ bổ sung/đổi ngày, giờ hoặc địa điểm (mà không phải câu hỏi danh sách rạp độc lập)
        val prevHasMovieOrGenre = !previousCriteria.movieTitle.isNullOrBlank() || !previousCriteria.genre.isNullOrBlank()
        val newHasMovieOrGenre = !extracted.movieTitle.isNullOrBlank() || !extracted.genre.isNullOrBlank()
        if (prevHasMovieOrGenre && !newHasMovieOrGenre && !isCinemaCentricQuery(normalizedQuery, extracted)) {
            return true
        }
        return false
    }

    private suspend fun loadAvailableMovies(): List<Movie> {
        val cached = MovieRepositoryImpl.getCachedMoviesSnapshot()
        if (cached.isNotEmpty()) return cached

        var result: List<Movie> = emptyList()
        movieRepository.getMovies(q = null, status = null, page = 0, size = 50).collect { res ->
            if (res is Resource.Success) {
                result = res.data.orEmpty()
            }
        }
        return result
    }

    private suspend fun searchMoviesByKeyword(keyword: String): List<Movie> {
        var result: List<Movie> = emptyList()
        movieRepository.getMovies(q = keyword, status = null, page = 0, size = 10).collect { res ->
            if (res is Resource.Success) {
                result = res.data.orEmpty()
            }
        }
        return result
    }

    private suspend fun loadAvailableCinemas(): List<Cinema> {
        val cached = CinemaRepositoryImpl.getCachedCinemasSnapshot()
        if (cached.isNotEmpty() && CinemaRepositoryImpl.isFullListFresh()) {
            return cached
        }

        val accumulated = cached.toMutableList()
        var page = 0
        while (page < 15) {
            var pageItems: List<Cinema> = emptyList()
            var hadError = false
            cinemaRepository.getCinemas(q = null, city = null, status = "ACTIVE", page = page).collect { res ->
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
            if (pageItems.size < 10) {
                CinemaRepositoryImpl.markFullCatalogLoaded()
                break
            }
            page++
        }
        return accumulated.distinctBy { it.cinemaId }
    }

    private suspend fun loadShowtimesForDate(
        date: String,
        specificMovieId: Long? = null
    ): List<Showtime> {
        val accumulated = mutableListOf<Showtime>()
        val maxPages = if (specificMovieId != null) 4 else 6
        var page = 0
        while (page < maxPages) {
            var pageItems: List<Showtime> = emptyList()
            var hadError = false
            showtimeRepository.getShowtimes(
                movieId = specificMovieId,
                cinemaId = null,
                date = date,
                page = page
            ).collect { res ->
                when (res) {
                    is Resource.Success -> pageItems = res.data.orEmpty()
                    is Resource.Error -> hadError = true
                    is Resource.Loading -> Unit
                }
            }
            if (hadError || pageItems.isEmpty()) break
            accumulated.addAll(pageItems)
            if (pageItems.size < 10) break
            page++
        }
        return accumulated.distinctBy { it.showtimeId }
    }

    private fun formatFriendlyDateLabel(dateStr: String): String {
        val today = LocalDate.now()
        val parsed = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return dateStr
        val ddMm = parsed.format(DateTimeFormatter.ofPattern("dd/MM"))
        return when (parsed) {
            today -> "Hôm nay ($ddMm)"
            today.plusDays(1) -> "Ngày mai ($ddMm)"
            today.plusDays(2) -> "Ngày kia ($ddMm)"
            else -> ddMm
        }
    }

    private fun currentTimeFormatted(): String {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
