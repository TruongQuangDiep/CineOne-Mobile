package com.example.cineone

import com.example.cineone.core.util.DateTimeUtils
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.home.HomeMovieTab
import com.example.cineone.ui.movie.formatAgeDescription
import com.example.cineone.ui.movie.formatMovieRatingBadge
import com.example.cineone.ui.movie.normalizeAgeBadge
import org.junit.Assert.assertEquals
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
    }
}
