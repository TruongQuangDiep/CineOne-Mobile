package com.example.cineone.ui.seat

import android.content.Context
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
    val remainingSeconds: Int = 600,
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
    private var countdownJob: kotlinx.coroutines.Job? = null

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

                    // Lấy userId của tài khoản đang đăng nhập hiện tại từ SharedPreferences
                    val context = CineOneApp.instance
                    val myUserId = context.getSharedPreferences("cineone_session", Context.MODE_PRIVATE)
                        .getLong("user_id", 0L)

                    val myHeldFromFirestore = mutableSetOf<Long>()

                    for (doc in snapshot.documents) {
                        val seatId = doc.getLong("seatId") ?: continue
                        val status = doc.getString("status") ?: ""
                        val holdingUserId = doc.getLong("userId") ?: 0L

                        when (status.uppercase()) {
                            "HOLDING" -> {
                                if (holdingUserId > 0L && holdingUserId == myUserId) {
                                    // Ghế này do CHÍNH BẠN đang giữ trên Firestore
                                    myHeldFromFirestore.add(seatId)
                                } else {
                                    // Ghế này do NGƯỜI KHÁC giữ -> khóa lại
                                    othersHeld.add(seatId)
                                }
                            }
                            "BOOKED" -> booked.add(seatId)
                        }
                    }

                    // Danh sách ghế của bạn: ưu tiên ghế đang giữ hợp lệ trên Firestore
                    val finalSelectedSeats = if (myHeldFromFirestore.isNotEmpty()) {
                        myHeldFromFirestore
                    } else {
                        // Nếu Firestore không có (hoặc vừa bị xóa do hết hạn), loại bỏ ghế đã hết hạn
                        uiState.value.selectedSeatIds.filter { !othersHeld.contains(it) && !booked.contains(it) }.toSet()
                    }

                    // Tự động tính lại tổng tiền cho các ghế được phục hồi
                    var calculatedTotal = 0.0
                    val allSeats = _uiState.value.seatsByRow.values.flatten()
                    finalSelectedSeats.forEach { id ->
                        val s = allSeats.find { it.seatId == id }
                        val price = if (s?.seatType == "VIP") _uiState.value.basePrice + 20000.0 else _uiState.value.basePrice
                        calculatedTotal += price
                    }

                    // Bật lại đồng hồ đếm ngược nếu có ghế đang giữ mà đồng hồ chưa chạy
                    if (finalSelectedSeats.isNotEmpty() && countdownJob?.isActive != true) {
                        startCountdown()
                    } else if (finalSelectedSeats.isEmpty()) {
                        countdownJob?.cancel()
                    }

                    _uiState.value = _uiState.value.copy(
                        selectedSeatIds = finalSelectedSeats,
                        totalPrice = if (calculatedTotal > 0) calculatedTotal else _uiState.value.totalPrice,
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
            if (currentSelected.isEmpty()) {
                countdownJob?.cancel()
            }
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
            if (countdownJob?.isActive != true) {
                startCountdown()
            }
            // GỌI API Spring Boot để kiểm tra & ghi nhận giữ ghế vào Firestore
            viewModelScope.launch {
                holdSeatsUseCase(currentShowtimeId, listOf(seatId)).collect { resource ->
                    if (resource is Resource.Error) {
                        // Nếu server báo xung đột (bị người khác vừa giữ trước), tự gỡ khỏi danh sách
                        currentSelected.remove(seatId)
                        if (currentSelected.isEmpty()) {
                            countdownJob?.cancel()
                        }
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

    // Bắt đầu đếm ngược 10 phút khi có ghế được giữ
    private fun startCountdown() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(remainingSeconds = 600)
        countdownJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = _uiState.value.remainingSeconds - 1
                )
            }
            // Hết 10 phút tự động gỡ chọn
            releaseAllSelectedSeats()
        }
    }

    // Nhả toàn bộ ghế đang giữ khi người dùng bấm "Rời khỏi"
    fun releaseAllSelectedSeats(onComplete: () -> Unit = {}) {
        countdownJob?.cancel()
        val currentSelected = _uiState.value.selectedSeatIds.toList()
        if (currentSelected.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(selectedSeatIds = emptySet(), totalPrice = 0.0)
            viewModelScope.launch {
                try {
                    releaseSeatsUseCase(currentShowtimeId, currentSelected).collect { }
                } finally {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Hủy lắng nghe realtime khi thoát màn hình để không ngốn pin và mạng
        heldSeatsListener?.remove()
        countdownJob?.cancel()
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