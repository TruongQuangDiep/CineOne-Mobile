package com.example.cineone.ui.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Movie
import com.example.cineone.domain.usecase.GetMovieDetailUseCase
import com.example.cineone.domain.usecase.GetMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

class MovieViewModel(
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val getMoviesUseCase: GetMoviesUseCase
) : ViewModel() {

    private val _detailUiState = MutableStateFlow(MovieDetailUiState())
    val detailUiState: StateFlow<MovieDetailUiState> = _detailUiState.asStateFlow()

    private val _listUiState = MutableStateFlow(MovieListUiState())
    val listUiState: StateFlow<MovieListUiState> = _listUiState.asStateFlow()

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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                MovieViewModel(
                    getMovieDetailUseCase = app.useCaseModule.getMovieDetailUseCase,
                    getMoviesUseCase = app.useCaseModule.getMoviesUseCase
                )
            }
        }
    }
}
