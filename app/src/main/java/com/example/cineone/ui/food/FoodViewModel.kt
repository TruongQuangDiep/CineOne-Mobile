package com.example.cineone.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Fnb
import com.example.cineone.domain.usecase.GetCinemaFnbsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoodUiState(
    val isLoading: Boolean = false,
    val fnbs: List<Fnb> = emptyList(),
    val cartItems: Map<Long, Int> = emptyMap(), // fnbId -> số lượng
    val seatPrice: Double = 0.0,
    val foodTotalPrice: Double = 0.0,
    val grandTotalPrice: Double = 0.0,
    val errorMessage: String? = null
)

class FoodViewModel(
    private val getCinemaFnbsUseCase: GetCinemaFnbsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodUiState())
    val uiState: StateFlow<FoodUiState> = _uiState.asStateFlow()

    // 1. Tải danh sách Combo Bắp Nước theo rạp từ Backend
    fun loadCinemaFnbs(cinemaId: Long, seatPrice: Double) {
        _uiState.value = _uiState.value.copy(
            seatPrice = seatPrice,
            grandTotalPrice = seatPrice
        )

        viewModelScope.launch {
            getCinemaFnbsUseCase(cinemaId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val items = resource.data ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            fnbs = items,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải danh sách bắp nước"
                        )
                    }
                }
            }
        }
    }

    // 2. Tăng số lượng bắp nước (+)
    fun increaseQuantity(fnbId: Long) {
        val currentCart = _uiState.value.cartItems.toMutableMap()
        val currentQty = currentCart[fnbId] ?: 0
        currentCart[fnbId] = currentQty + 1
        updateCartState(currentCart)
    }

    // 3. Giảm số lượng bắp nước (-)
    fun decreaseQuantity(fnbId: Long) {
        val currentCart = _uiState.value.cartItems.toMutableMap()
        val currentQty = currentCart[fnbId] ?: 0
        if (currentQty > 1) {
            currentCart[fnbId] = currentQty - 1
        } else {
            currentCart.remove(fnbId)
        }
        updateCartState(currentCart)
    }

    // 4. Tính toán tổng tiền giỏ hàng tạm tính (SCRUM-95)
    private fun updateCartState(newCart: Map<Long, Int>) {
        var foodTotal = 0.0
        newCart.forEach { (id, qty) ->
            val item = _uiState.value.fnbs.find { it.fnbId == id }
            if (item != null) {
                foodTotal += (item.price * qty)
            }
        }

        val grandTotal = _uiState.value.seatPrice + foodTotal

        _uiState.value = _uiState.value.copy(
            cartItems = newCart,
            foodTotalPrice = foodTotal,
            grandTotalPrice = grandTotal
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                FoodViewModel(
                    getCinemaFnbsUseCase = app.useCaseModule.getCinemaFnbsUseCase
                )
            }
        }
    }
}