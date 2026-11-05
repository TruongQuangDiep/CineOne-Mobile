package com.example.cineone

import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.core.util.LocationFilterMode
import com.example.cineone.core.util.LocationUtils
import com.example.cineone.core.util.TimeSlotFilter
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Movie
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.home.HomeMovieTab
import com.example.cineone.ui.movie.MovieViewModel
import com.example.cineone.ui.movie.SearchCategoryTab
import com.example.cineone.ui.movie.formatAgeDescription
import com.example.cineone.ui.movie.formatMovieRatingBadge
import com.example.cineone.ui.movie.normalizeAgeBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun homeMovieTab_mapsCorrectStatuses() {
        assertEquals("NOW_SHOWING", HomeMovieTab.NOW_SHOWING.statusCode)
        assertEquals("SNEAK_SHOW", HomeMovieTab.SNEAK_SHOW.statusCode)
        assertEquals("COMING_SOON", HomeMovieTab.COMING_SOON.statusCode)
    }

    @Test
    fun ageRatingFormatting_matchesDesignSpec() {
        assertEquals("", normalizeAgeBadge(""))
        assertEquals("18+", normalizeAgeBadge("T18"))
        assertEquals("13+", normalizeAgeBadge("C13"))
        assertEquals("P", normalizeAgeBadge("P"))
        assertEquals(
            "Phân loại độ tuổi: Chưa cập nhật",
            formatAgeDescription("")
        )
        assertEquals(
            "Phim phổ biến cho người trên 18 tuổi",
            formatAgeDescription("18+")
        )
    }

    @Test
    fun ratingAndReleaseDateFormatting_matchesRealDatabaseCounts() {
        assertEquals("Chưa có đánh giá", formatMovieRatingBadge(0.0))
        assertEquals("9/10", formatMovieRatingBadge(4.5))
        assertEquals("8.5/10", formatMovieRatingBadge(8.5))
        assertEquals("18/09/2026", DateTimeUtils.formatReleaseDate("2026-09-18"))
    }

    @Test
    fun navigationRoutes_createExpectedPaths() {
        assertEquals("movie_detail/42", Screen.MovieDetail.createRoute(42L))
        assertEquals("movie_list?status=SNEAK_SHOW", Screen.MovieList.createRoute("SNEAK_SHOW"))
        assertEquals(
            "movie_search?query=Mai&tab=MOVIE&locationMode=CURRENT_LOCATION",
            Screen.MovieSearch.createRoute(
                query = "Mai",
                tab = "MOVIE",
                locationMode = "CURRENT_LOCATION"
            )
        )
        assertEquals(
            "cinema_detail/5?cinemaName=Beta%20Quang%20Trung&date=2026-04-05&timeSlot=SLOT_12_17",
            Screen.CinemaDetail.createRoute(
                cinemaId = 5L,
                cinemaName = "Beta Quang Trung",
                date = "2026-04-05",
                timeSlot = "SLOT_12_17"
            )
        )
    }

    @Test
    fun timeSlotFilter_matchesShowtimeHoursCorrectly() {
        assertTrue(TimeSlotFilter.ALL.matches("2026-04-05T09:15:00"))
        assertTrue(TimeSlotFilter.SLOT_8_12.matches("2026-04-05T09:15:00"))
        assertFalse(TimeSlotFilter.SLOT_8_12.matches("2026-04-05T13:30:00"))
        assertTrue(TimeSlotFilter.SLOT_12_17.matches("2026-04-05T13:30:00"))
        assertTrue(TimeSlotFilter.SLOT_17_21.matches("2026-04-05T19:45:00"))
        assertTrue(TimeSlotFilter.SLOT_21_24.matches("2026-04-05T22:15:00"))
    }

    @Test
    fun locationDistanceFilter_calculatesHaversineAndSortsNearestFirst() {
        val c1 = Cinema(
            cinemaId = 1L,
            name = "CGV Vincom Đồng Khởi",
            address = "72 Lê Thánh Tôn, Quận 1, TP.HCM",
            city = "TP. Hồ Chí Minh",
            latitude = 10.7781,
            longitude = 106.7019
        )
        val c2 = Cinema(
            cinemaId = 2L,
            name = "Beta Quang Trung",
            address = "645 Quang Trung, Quận Gò Vấp, TP.HCM",
            city = "TP. Hồ Chí Minh",
            latitude = 10.8355,
            longitude = 106.6626
        )

        // Người dùng đang ở Quận Gò Vấp (gần Beta Quang Trung hơn CGV Quận 1)
        val userLat = 10.8360
        val userLon = 106.6630

        val sorted = MovieViewModel.filterAndSortCinemas(
            cinemas = listOf(c1, c2),
            normalizedQuery = "",
            locationMode = LocationFilterMode.CURRENT_LOCATION,
            selectedCity = "",
            currentLat = userLat,
            currentLon = userLon,
            homeLat = null,
            homeLon = null,
            homeAddress = ""
        )

        assertEquals(2, sorted.size)
        assertEquals(2L, sorted[0].cinema.cinemaId)
        assertEquals(1L, sorted[1].cinema.cinemaId)
        assertTrue((sorted[0].distanceKm ?: 99.0) < (sorted[1].distanceKm ?: 0.0))
        assertEquals("Gò Vấp", LocationUtils.extractAreaBracket(c2))
    }

    @Test
    fun multiCriteriaSearch_extractsGenresAndCategoriesCorrectly() {
        val movies = listOf(
            Movie(
                movieId = 1L,
                title = "Quỷ Nhập Tràng",
                poster = "",
                trailer = "",
                duration = 122,
                releaseDate = "2026-03-07",
                genres = "Kinh dị, Tâm lý",
                rating = 8.4,
                description = ""
            ),
            Movie(
                movieId = 2L,
                title = "Bộ Tứ Báo Thủ",
                poster = "",
                trailer = "",
                duration = 130,
                releaseDate = "2026-01-29",
                genres = "Hài hước, Tâm lý",
                rating = 8.1,
                description = ""
            )
        )

        val genres = MovieViewModel.extractGenresFromMovies(movies)
        assertEquals(listOf("Kinh dị", "Tâm lý", "Hài hước"), genres)
        assertEquals(SearchCategoryTab.GENRE, SearchCategoryTab.fromCode("GENRE"))
    }
}
