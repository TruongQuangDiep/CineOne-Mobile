package com.example.cineone.data.repository

import android.content.Context
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.movie.MovieDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.movie.MovieApi
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class MovieRepositoryImpl(private val movieApi: MovieApi) : MovieRepository {

    override fun getMovies(
        q: String?,
        status: String?,
        genreId: Long?,
        page: Int,
        size: Int
    ): Flow<Resource<List<Movie>>> = flow {
        val cacheKey = "${q.orEmpty().trim().lowercase()}|${status.orEmpty().trim().uppercase()}|${genreId ?: -1L}|$page|$size"
        val now = System.currentTimeMillis()

        // 1. Kiểm tra RAM cache
        val memEntry = queryMemoryCache[cacheKey]
        if (memEntry != null && (now - memEntry.first) < CACHE_TTL_MS) {
            emit(Resource.Success(memEntry.second))
            return@flow
        }

        // 2. Nếu lấy toàn bộ danh sách phim (q=null, status=null), kiểm tra Disk cache
        val isAllMoviesQuery = q.isNullOrBlank() && status.isNullOrBlank() && genreId == null && page == 0
        if (isAllMoviesQuery && allMoviesMemoryCache.isEmpty()) {
            loadMoviesFromDiskIfAvailable()
        }
        if (isAllMoviesQuery && allMoviesMemoryCache.isNotEmpty() && (now - allMoviesTimestamp) < CACHE_TTL_MS) {
            emit(Resource.Success(allMoviesMemoryCache))
            return@flow
        }

        // Nếu có sẵn dữ liệu cũ trên đĩa khi mới mở app, phát ngay để UI hiển thị tức thì trước khi cập nhật ngầm
        val hasStaleAllMovies = isAllMoviesQuery && allMoviesMemoryCache.isNotEmpty()
        if (hasStaleAllMovies) {
            emit(Resource.Success(allMoviesMemoryCache))
        } else {
            emit(Resource.Loading)
        }

        try {
            val response = movieApi.getMovies(q = q, status = status, genreId = genreId, page = page, size = size)
            if (response.success && response.data != null) {
                val domainList = response.data.map { it.toDomain() }
                val ts = System.currentTimeMillis()
                queryMemoryCache[cacheKey] = ts to domainList
                domainList.forEach { detailMemoryCache[it.movieId] = ts to it }

                if (isAllMoviesQuery) {
                    allMoviesMemoryCache = domainList
                    allMoviesTimestamp = ts
                    saveMoviesToDisk(response.data, ts)
                }

                emit(Resource.Success(domainList))
            } else if (!hasStaleAllMovies) {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách phim"))
            }
        } catch (e: Exception) {
            if (!hasStaleAllMovies) {
                emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
            }
        }
    }

    override fun getMovieDetail(movieId: Long): Flow<Resource<Movie>> = flow {
        val now = System.currentTimeMillis()
        val cached = detailMemoryCache[movieId]
        if (cached != null && (now - cached.first) < CACHE_TTL_MS) {
            emit(Resource.Success(cached.second))
            return@flow
        }

        emit(Resource.Loading)
        try {
            val response = movieApi.getMovieDetail(movieId)
            if (response.success && response.data != null) {
                val domain = response.data.toDomain()
                detailMemoryCache[movieId] = System.currentTimeMillis() to domain
                emit(Resource.Success(domain))
            } else {
                emit(Resource.Error(response.message ?: "Không tìm thấy chi tiết phim"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 phút
        private const val PREFS_NAME = "cineone_catalog_cache"
        private const val KEY_MOVIES_JSON = "cached_all_movies_json"
        private const val KEY_MOVIES_TS = "cached_all_movies_ts"

        private val json = Json { ignoreUnknownKeys = true }
        private val queryMemoryCache = ConcurrentHashMap<String, Pair<Long, List<Movie>>>()
        private val detailMemoryCache = ConcurrentHashMap<Long, Pair<Long, Movie>>()

        @Volatile
        private var allMoviesMemoryCache: List<Movie> = emptyList()

        @Volatile
        private var allMoviesTimestamp: Long = 0L

        fun getCachedMoviesSnapshot(): List<Movie> {
            if (allMoviesMemoryCache.isEmpty()) {
                loadMoviesFromDiskIfAvailable()
            }
            return allMoviesMemoryCache
        }

        private fun loadMoviesFromDiskIfAvailable() {
            runCatching {
                val prefs = CineOneApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val rawJson = prefs.getString(KEY_MOVIES_JSON, null)
                val savedTs = prefs.getLong(KEY_MOVIES_TS, 0L)
                if (!rawJson.isNullOrBlank()) {
                    val dtos = json.decodeFromString<List<MovieDto>>(rawJson)
                    if (dtos.isNotEmpty()) {
                        allMoviesMemoryCache = dtos.map { it.toDomain() }
                        allMoviesTimestamp = savedTs
                    }
                }
            }
        }

        private fun saveMoviesToDisk(dtos: List<MovieDto>, timestamp: Long) {
            if (dtos.isEmpty()) return
            runCatching {
                val prefs = CineOneApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_MOVIES_JSON, json.encodeToString(dtos))
                    .putLong(KEY_MOVIES_TS, timestamp)
                    .apply()
            }
        }
    }
}
