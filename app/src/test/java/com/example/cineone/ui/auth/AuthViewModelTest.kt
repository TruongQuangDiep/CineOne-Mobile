package com.example.cineone.ui.auth

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.User
import com.example.cineone.domain.repository.AuthRepository
import com.example.cineone.domain.usecase.LoginUseCase
import com.example.cineone.domain.usecase.RegisterUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

private class FakeAuthRepository : AuthRepository {
    var verifyOtpResult: Resource<Unit> = Resource.Success(Unit)
    var updateProfileResult: Resource<User> = Resource.Success(User())
    var sendPhoneOtpResult: Resource<Unit> = Resource.Success(Unit)
    var verifyPhoneOtpResult: Resource<Unit> = Resource.Success(Unit)

    override fun login(email: String, pass: String): Flow<Resource<User>> =
        flowOf(Resource.Success(User(email = email)))

    override fun register(email: String, pass: String, name: String, phone: String?): Flow<Resource<User>> =
        flowOf(Resource.Success(User(email = email, fullName = name, phone = phone)))

    override fun verifyOtp(email: String, otp: String, type: String): Flow<Resource<Unit>> =
        flowOf(verifyOtpResult)

    override fun getProfile(): Flow<Resource<User>> =
        flowOf(Resource.Success(User()))

    override fun updateProfile(
        fullName: String,
        phone: String?,
        address: String?,
        dateOfBirth: String?,
        phoneVerified: Boolean?
    ): Flow<Resource<User>> = flowOf(updateProfileResult)

    override fun forgotPassword(email: String): Flow<Resource<Unit>> = flowOf(Resource.Success(Unit))

    override fun resetPassword(email: String, newPassword: String): Flow<Resource<Unit>> = flowOf(Resource.Success(Unit))

    override fun sendPhoneOtp(phone: String): Flow<Resource<Unit>> = flowOf(sendPhoneOtpResult)

    override fun verifyPhoneOtp(phone: String, otp: String, email: String): Flow<Resource<Unit>> = flowOf(verifyPhoneOtpResult)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

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
    fun verifyOtp_whenValid_setsIsOtpSuccessTrue() = runTest(testDispatcher) {
        val repo = FakeAuthRepository().apply {
            verifyOtpResult = Resource.Success(Unit)
        }
        val viewModel = AuthViewModel(LoginUseCase(repo), RegisterUseCase(repo), repo)
        viewModel.verifyOtp(email = "user@cineone.vn", otp = "123456", type = "EMAIL_VERIFY")
        runCurrent()
        assertTrue(viewModel.uiState.value.isOtpSuccess)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun verifyOtp_whenInvalid_setsErrorMessageAndKeepsOtpSuccessFalse() = runTest(testDispatcher) {
        val repo = FakeAuthRepository().apply {
            verifyOtpResult = Resource.Error("Mã OTP không chính xác hoặc đã hết hạn")
        }
        val viewModel = AuthViewModel(LoginUseCase(repo), RegisterUseCase(repo), repo)
        viewModel.verifyOtp(email = "user@cineone.vn", otp = "000000")
        runCurrent()
        assertFalse(viewModel.uiState.value.isOtpSuccess)
        assertEquals("Mã OTP không chính xác hoặc đã hết hạn", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun updateProfile_withPhoneVerifiedTrue_updatesUserPhoneState() = runTest(testDispatcher) {
        val updatedUser = User(
            userId = 1L,
            email = "user@cineone.vn",
            fullName = "Nguyen Van A",
            phone = "0911223344",
            isPhoneVerified = true
        )
        val repo = FakeAuthRepository().apply {
            updateProfileResult = Resource.Success(updatedUser)
        }
        val viewModel = AuthViewModel(LoginUseCase(repo), RegisterUseCase(repo), repo)
        viewModel.updateProfile(
            fullName = "Nguyen Van A",
            phone = "0911223344",
            address = null,
            phoneVerified = true
        )
        runCurrent()
        val state = viewModel.uiState.value
        assertTrue(state.isUpdateSuccess)
        assertEquals("0911223344", state.user?.phone)
        assertTrue(state.user?.isPhoneVerified == true)
    }
}
