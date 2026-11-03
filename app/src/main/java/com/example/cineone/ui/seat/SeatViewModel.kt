package com.example.cineone.ui.seat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cineone.CineOneApp
import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Seat
import com.example.cineone.domain.usecase.GetSeatsUseCase
import com.example.cineone.domain.usecase.HoldSeatsUseCase
import com.example.cineone.domain.usecase.ReleaseSeatsUseCase
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeatUiState(
    val isLoading: Boolean = false,
    val seatsByRow: Map<String, List<Seat>> = emptyMap(),
    val selectedSeatIds: Set<Long> = emptySet(),     // Ghế tôi đang chọn
    val heldByOtherSeatIds: Set<Long> = emptySet(),  // Ghế người khác đang giữ (Realtime)
    val bookedSeatIds: Set<Long> = emptySet(),       // Ghế đã thanh toán xong
    val basePrice: Double = 85000.0,
    val totalPrice: Double = 0.0,
    val errorMessage: String? = null
)

class SeatViewModel(
    private val getSeatsUseCase: GetSeatsUseCase,
    private val holdSeatsUseCase: HoldSeatsUseCase,
    private val releaseSeatsUseCase: ReleaseSeatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeatUiState())
    val uiState: StateFlow<SeatUiState> = _uiState.asStateFlow()

    private var heldSeatsListener: ListenerRegistration? = null
    private var currentShowtimeId: Long = 0L

    // 1. Tải sơ đồ ghế tĩnh từ API Spring Boot
    fun loadSeats(showtimeId: Long, basePrice: Double = 85000.0) {
        currentShowtimeId = showtimeId
        _uiState.value = _uiState.value.copy(basePrice = basePrice)

        viewModelScope.launch {
            getSeatsUseCase(showtimeId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val seats = resource.data ?: emptyList()
                        // Gom nhóm ghế theo Hàng (Row A, Row B...)
                        val grouped = seats.groupBy { it.seatRow }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            seatsByRow = grouped,
                            errorMessage = null
                        )
                        // Bắt đầu lắng nghe Realtime từ Firestore ngay sau khi có layout
                        listenHeldSeatsRealtime(showtimeId)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Không thể tải sơ đồ ghế"
                        )
                    }
                }
            }
        }
    }

    // 2. LẮNG NGHE REALTIME FIRESTORE (Sub-collection "held_seats" do Spring Boot cập nhật)
    private fun listenHeldSeatsRealtime(showtimeId: Long) {
        heldSeatsListener?.remove() // Hủy đăng ký cũ nếu có

        val db = Firebase.firestore
        heldSeatsListener = db.collection("showtimes")
            .document(showtimeId.toString())
            .collection("held_seats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val othersHeld = mutableSetOf<Long>()
                    val booked = mutableSetOf<Long>()

                    for (doc in snapshot.documents) {
                        val seatId = doc.getLong("seatId") ?: continue
                        val status = doc.getString("status") ?: ""

                        when (status.uppercase()) {
                            "HOLDING" -> {
                                // Nếu không nằm trong danh sách ghế do chính tôi chọn -> là người khác đang giữ
                                if (!uiState.value.selectedSeatIds.contains(seatId)) {
                                    othersHeld.add(seatId)
                                }
                            }
                            "BOOKED" -> booked.add(seatId)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        heldByOtherSeatIds = othersHeld,
                        bookedSeatIds = booked
                    )
                }
            }
    }

    // 3. Người dùng bấm chọn / bỏ chọn ghế
    fun onSeatClick(seat: Seat) {
        val seatId = seat.seatId
        val currentState = _uiState.value

        // Nếu ghế đã bị người khác giữ hoặc đã đặt -> Không cho chọn
        if (currentState.heldByOtherSeatIds.contains(seatId) || currentState.bookedSeatIds.contains(seatId)) {
            return
        }

        val currentSelected = currentState.selectedSeatIds.toMutableSet()

        if (currentSelected.contains(seatId)) {
            // BỎ CHỌN: Gọi API releaseSeats
            currentSelected.remove(seatId)
            viewModelScope.launch {
                releaseSeatsUseCase(currentShowtimeId, listOf(seatId)).collect { }
            }
        } else {
            // CHỌN THÊM: Tối đa 8 ghế
            if (currentSelected.size >= 8) {
                _uiState.value = _uiState.value.copy(errorMessage = "Chỉ được chọn tối đa 8 ghế")
                return
            }
            currentSelected.add(seatId)
            // GỌI API Spring Boot để kiểm tra & ghi nhận giữ ghế vào Firestore
            viewModelScope.launch {
                holdSeatsUseCase(currentShowtimeId, listOf(seatId)).collect { resource ->
                    if (resource is Resource.Error) {
                        // Nếu server báo xung đột (bị người khác vừa giữ trước), tự gỡ khỏi danh sách
                        currentSelected.remove(seatId)
                        _uiState.value = _uiState.value.copy(
                            selectedSeatIds = currentSelected,
                            errorMessage = resource.message ?: "Ghế vừa bị người khác chọn"
                        )
                    }
                }
            }
        }

        // Tạm tính tổng tiền: Ghế VIP phụ thu 20.000đ
        var total = 0.0
        val allSeats = currentState.seatsByRow.values.flatten()
        currentSelected.forEach { id ->
            val s = allSeats.find { it.seatId == id }
            val price = if (s?.seatType == "VIP") currentState.basePrice + 20000.0 else currentState.basePrice
            total += price
        }

        _uiState.value = _uiState.value.copy(
            selectedSeatIds = currentSelected,
            totalPrice = total,
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Hủy lắng nghe realtime khi thoát màn hình để không ngốn pin và mạng
        heldSeatsListener?.remove()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CineOneApp.instance
                SeatViewModel(
                    getSeatsUseCase = app.useCaseModule.getSeatsUseCase,
                    holdSeatsUseCase = app.useCaseModule.holdSeatsUseCase,
                    releaseSeatsUseCase = app.useCaseModule.releaseSeatsUseCase
                )
            }
        }
    }
}