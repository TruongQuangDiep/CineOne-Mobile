package com.example.cineone.data.repository

import android.content.Context
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.cinema.CinemaDto
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.cinema.CinemaApi
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Fnb
import com.example.cineone.domain.repository.CinemaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class CinemaRepositoryImpl(private val cinemaApi: CinemaApi) : CinemaRepository {

    override fun getCinemas(
        q: String?,
        city: String?,
        status: String?,
        page: Int
    ): Flow<Resource<List<Cinema>>> = flow {
        val cacheKey = "${q.orEmpty().trim().lowercase()}|${city.orEmpty().trim().lowercase()}|${status.orEmpty().trim().uppercase()}|$page"
        val now = System.currentTimeMillis()

        // 1. Kiểm tra RAM cache cho trang hiện tại
        val memEntry = pageMemoryCache[cacheKey]
        if (memEntry != null && (now - memEntry.first) < CACHE_TTL_MS) {
            emit(Resource.Success(memEntry.second))
            return@flow
        }

        // 2. Nếu là truy vấn danh sách rạp mặc định (q=null, city=null), kiểm tra Disk cache khi vừa mở lại app
        val isDefaultQuery = q.isNullOrBlank() && city.isNullOrBlank() && (status.isNullOrBlank() || status == "ACTIVE")
        if (isDefaultQuery && allCinemasMemoryCache.isEmpty()) {
            loadFromDiskCacheIfAvailable()
        }

        if (isDefaultQuery && allCinemasMemoryCache.isNotEmpty()) {
            val fromSlice = page * PAGE_SIZE
            if (fromSlice < allCinemasMemoryCache.size && (now - allCinemasTimestamp) < CACHE_TTL_MS) {
                val slice = allCinemasMemoryCache.subList(
                    fromSlice,
                    (fromSlice + PAGE_SIZE).coerceAtMost(allCinemasMemoryCache.size)
                )
                emit(Resource.Success(slice))
                return@flow
            }
        }

        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemas(q = q, city = city, status = status, page = page)
            if (response.success && response.data != null) {
                val domainList = response.data.map { it.toDomain() }
                val fetchTime = System.currentTimeMillis()
                pageMemoryCache[cacheKey] = fetchTime to domainList

                if (isDefaultQuery) {
                    mergeIntoAllCinemasCache(page, domainList, response.data)
                }

                emit(Resource.Success(domainList))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách rạp"))
            }
        } catch (e: Exception) {
            // Fallback về dữ liệu đã lưu trên máy nếu mất mạng tạm thời
            if (isDefaultQuery && allCinemasMemoryCache.isNotEmpty() && page == 0) {
                emit(Resource.Success(allCinemasMemoryCache))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
            }
        }
    }

    override fun getCinemaDetail(cinemaId: Long): Flow<Resource<Cinema>> = flow {
        val cached = allCinemasMemoryCache.firstOrNull { it.cinemaId == cinemaId }
        if (cached != null) {
            emit(Resource.Success(cached))
            return@flow
        }
        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemaDetail(cinemaId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không tìm thấy chi tiết rạp"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getCinemaFnbs(cinemaId: Long): Flow<Resource<List<Fnb>>> = flow {
        val cachedFnb = fnbMemoryCache[cinemaId]
        val now = System.currentTimeMillis()
        if (cachedFnb != null && (now - cachedFnb.first) < CACHE_TTL_MS) {
            emit(Resource.Success(cachedFnb.second))
            return@flow
        }
        emit(Resource.Loading)
        try {
            val response = cinemaApi.getCinemaFnbs(cinemaId)
            if (response.success && response.data != null) {
                val mapped = response.data.map { it.toDomain() }
                fnbMemoryCache[cinemaId] = System.currentTimeMillis() to mapped
                emit(Resource.Success(mapped))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách bắp nước"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 phút
        private const val PREFS_NAME = "cineone_catalog_cache"
        private const val KEY_CINEMAS_JSON = "cached_active_cinemas_json"
        private const val KEY_CINEMAS_TS = "cached_active_cinemas_ts"
        private const val KEY_CINEMAS_COMPLETE = "cached_active_cinemas_complete"

        private val json = Json { ignoreUnknownKeys = true }
        private val pageMemoryCache = ConcurrentHashMap<String, Pair<Long, List<Cinema>>>()
        private val fnbMemoryCache = ConcurrentHashMap<Long, Pair<Long, List<Fnb>>>()

        @Volatile
        private var allCinemasMemoryCache: List<Cinema> = emptyList()

        @Volatile
        private var allCinemaDtosCache: List<CinemaDto> = emptyList()

        @Volatile
        private var allCinemasTimestamp: Long = 0L

        @Volatile
        private var isFullCatalogCached: Boolean = false

        fun getCachedCinemasSnapshot(): List<Cinema> {
            if (allCinemasMemoryCache.isEmpty()) {
                loadFromDiskCacheIfAvailable()
            }
            return allCinemasMemoryCache
        }

        fun isFullListFresh(): Boolean {
            if (allCinemasMemoryCache.isEmpty()) {
                loadFromDiskCacheIfAvailable()
            }
            val now = System.currentTimeMillis()
            return isFullCatalogCached &&
                allCinemasMemoryCache.isNotEmpty() &&
                (now - allCinemasTimestamp) < CACHE_TTL_MS
        }

        fun markFullCatalogLoaded() {
            isFullCatalogCached = true
            saveToDiskCache()
        }

        @Synchronized
        private fun mergeIntoAllCinemasCache(
            page: Int,
            domainItems: List<Cinema>,
            dtoItems: List<CinemaDto>
        ) {
            val mergedDomain = (if (page == 0 && !isFullCatalogCached) domainItems else allCinemasMemoryCache + domainItems)
                .distinctBy { it.cinemaId }
            val mergedDtos = (if (page == 0 && !isFullCatalogCached) dtoItems else allCinemaDtosCache + dtoItems)
                .distinctBy { it.cinemaId }

            allCinemasMemoryCache = mergedDomain
            allCinemaDtosCache = mergedDtos
            allCinemasTimestamp = System.currentTimeMillis()
            if (domainItems.size < PAGE_SIZE) {
                isFullCatalogCached = true
            }
            saveToDiskCache()
        }

        private fun loadFromDiskCacheIfAvailable() {
            runCatching {
                val prefs = CineOneApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val rawJson = prefs.getString(KEY_CINEMAS_JSON, null)
                val savedTs = prefs.getLong(KEY_CINEMAS_TS, 0L)
                val complete = prefs.getBoolean(KEY_CINEMAS_COMPLETE, false)
                if (!rawJson.isNullOrBlank()) {
                    val dtos = json.decodeFromString<List<CinemaDto>>(rawJson)
                    if (dtos.isNotEmpty()) {
                        allCinemaDtosCache = dtos
                        allCinemasMemoryCache = dtos.map { it.toDomain() }
                        allCinemasTimestamp = savedTs
                        isFullCatalogCached = complete
                    }
                }
            }
        }

        private fun saveToDiskCache() {
            if (allCinemaDtosCache.isEmpty()) return
            runCatching {
                val prefs = CineOneApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val rawJson = json.encodeToString(allCinemaDtosCache)
                prefs.edit()
                    .putString(KEY_CINEMAS_JSON, rawJson)
                    .putLong(KEY_CINEMAS_TS, allCinemasTimestamp)
                    .putBoolean(KEY_CINEMAS_COMPLETE, isFullCatalogCached)
                    .apply()
            }
        }
    }
}
