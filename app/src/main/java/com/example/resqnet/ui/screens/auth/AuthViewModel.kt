package com.example.resqnet.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.mock.MockAuthRepository
import com.example.resqnet.data.repository.AuthRepository
import com.example.resqnet.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val phone: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val verifiedUser: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = MockAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onPhoneChanged(phone: String) {
        if (phone.length <= 10 && phone.all { it.isDigit() }) {
            _uiState.update { it.copy(phone = phone, error = null) }
        }
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otp = otp, error = null) }
        }
    }

    fun sendOtp(onSuccess: () -> Unit) {
        // TODO: Restore auth logic — temporarily bypassed for UI testing
        // viewModelScope.launch {
        //     _uiState.update { it.copy(isLoading = true, error = null) }
        //     val result = authRepository.sendOtp("+91${_uiState.value.phone}")
        //     result.fold(
        //         onSuccess = {
        //             _uiState.update { it.copy(isLoading = false, otpSent = true) }
        //             onSuccess()
        //         },
        //         onFailure = { e ->
        //             _uiState.update { it.copy(isLoading = false, error = e.message) }
        //         }
        //     )
        // }

        // ── Bypass: skip API call, navigate immediately ──
        _uiState.update { it.copy(otpSent = true) }
        onSuccess()
    }

    fun verifyOtp(onSuccess: (String) -> Unit) {
        // TODO: Restore auth logic — temporarily bypassed for UI testing
        // viewModelScope.launch {
        //     _uiState.update { it.copy(isLoading = true, error = null) }
        //     val result = authRepository.verifyOtp(
        //         "+91${_uiState.value.phone}",
        //         _uiState.value.otp
        //     )
        //     result.fold(
        //         onSuccess = { user ->
        //             _uiState.update { it.copy(isLoading = false, verifiedUser = user) }
        //             onSuccess(user.role.name.lowercase())
        //         },
        //         onFailure = { e ->
        //             _uiState.update { it.copy(isLoading = false, error = e.message) }
        //         }
        //     )
        // }

        // ── Bypass: skip OTP verification, navigate as "user" role ──
        onSuccess("user")
    }
}
