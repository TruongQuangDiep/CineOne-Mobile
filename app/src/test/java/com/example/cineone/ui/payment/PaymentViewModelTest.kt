package com.example.cineone.ui.payment

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.payment.PaymentLinkResponseDto
import com.example.cineone.data.dto.payment.PaymentStatusDto
import com.example.cineone.domain.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePaymentRepository : PaymentRepository {
    var linkResponse: PaymentLinkResponseDto = PaymentLinkResponseDto()
    var statusResponse: PaymentStatusDto = PaymentStatusDto()
    val cancelCalls = mutableListOf<Pair<Long, Boolean>>()

    override fun createPaymentLink(bookingId: Long): Flow<Resource<PaymentLinkResponseDto>> =
        flowOf(Resource.Success(linkResponse))

    override fun getPaymentStatus(bookingId: Long): Flow<Resource<PaymentStatusDto>> =
        flowOf(Resource.Success(statusResponse))

    override fun cancelPayment(bookingId: Long, expired: Boolean): Flow<Resource<Unit>> {
        cancelCalls.add(bookingId to expired)
        return flowOf(Resource.Success(Unit))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initPayment_loadsQrAndRemainingSeconds_andManualCancelCallsExpiredFalse() = runTest(testDispatcher) {
        val repo = FakePaymentRepository().apply {
            linkResponse = PaymentLinkResponseDto(
                orderCode = 123456L,
                checkoutUrl = "https://pay.payos.vn/web/123",
                qrCode = "vietqr-sample",
                status = "PENDING",
                remainingSeconds = 420L
            )
        }
        val viewModel = PaymentViewModel(repo)

        viewModel.initPayment(bookingId = 10L, bookingCode = "CNE10", amount = 85000L, movieTitle = "Deadpool")
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("vietqr-sample", state.qrCode)
        assertEquals(420, state.remainingSeconds)
        assertFalse(state.isExpired)

        var finished = false
        viewModel.cancelPayment { finished = true }
        runCurrent()

        assertTrue(finished)
        assertEquals(listOf(10L to false), repo.cancelCalls)
    }

    @Test
    fun countdownExpires_marksExpiredAndNotifiesBackendWithExpiredTrue() = runTest(testDispatcher) {
        val repo = FakePaymentRepository().apply {
            linkResponse = PaymentLinkResponseDto(
                orderCode = 999L,
                qrCode = "vietqr-2s",
                status = "PENDING",
                remainingSeconds = 2L
            )
            statusResponse = PaymentStatusDto(bookingId = 20L, bookingStatus = "HELD", status = "PENDING")
        }
        val viewModel = PaymentViewModel(repo)

        viewModel.initPayment(bookingId = 20L, bookingCode = "CNE20", amount = 50000L, movieTitle = "Test")
        runCurrent()

        // Tua thời gian 2.5 giây để hết countdown 2 giây
        advanceTimeBy(2_500L)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(0, state.remainingSeconds)
        assertTrue(state.isExpired)
        assertEquals(listOf(20L to true), repo.cancelCalls)
    }

    @Test
    fun pollingReceivesExpired_stopsTimerAndMarksExpired() = runTest(testDispatcher) {
        val repo = FakePaymentRepository().apply {
            linkResponse = PaymentLinkResponseDto(
                orderCode = 888L,
                qrCode = "vietqr-600s",
                status = "PENDING",
                remainingSeconds = 600L
            )
            statusResponse = PaymentStatusDto(
                bookingId = 30L,
                bookingStatus = "EXPIRED",
                isPaid = false,
                status = "EXPIRED"
            )
        }
        val viewModel = PaymentViewModel(repo)

        viewModel.initPayment(bookingId = 30L, bookingCode = "CNE30", amount = 75000L, movieTitle = "Test")
        runCurrent()

        // Tua 2.1 giây để polling gọi getPaymentStatus
        advanceTimeBy(2_100L)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isExpired)
        assertEquals(0, state.remainingSeconds)
    }
}