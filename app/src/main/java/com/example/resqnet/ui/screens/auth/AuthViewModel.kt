package com.example.resqnet.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.firebase.FirebaseAuthRepository
import com.example.resqnet.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AuthUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val dobMillis: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val authenticatedUser: User? = null
)

class AuthViewModel(
    val authRepository: FirebaseAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Field callbacks ───────────────────────────────────────────────────────

    fun onFullNameChanged(v: String) = _uiState.update { it.copy(fullName = v, error = null) }
    fun onEmailChanged(v: String)    = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChanged(v: String) =
        _uiState.update { it.copy(confirmPassword = v, error = null) }

    fun onDobSelected(millis: Long)  = _uiState.update { it.copy(dobMillis = millis, error = null) }

    // ── Auth state check ─────────────────────────────────────────────────────

    /**
     * Called at startup. Navigates straight to home if the user is still signed in
     * and the 7-day session has not expired; otherwise shows Login.
     *
     * [onHome] receives the role string, [onLogin] tells the NavGraph to show Login.
     */
    fun checkAuthState(onHome: (String) -> Unit, onLogin: () -> Unit) {
        if (authRepository.needsReAuth()) {
            onLogin()
        } else {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                onHome(user.role.name.lowercase())
            } else {
                onLogin()
            }
        }
    }

    // ── Registration ─────────────────────────────────────────────────────────

    fun register(onSuccess: (String) -> Unit) {
        val s = _uiState.value

        // Validation
        if (s.fullName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your full name") }; return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }; return
        }
        if (s.password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }; return
        }
        if (s.password != s.confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }; return
        }
        val dob = s.dobMillis
        if (dob == null) {
            _uiState.update { it.copy(error = "Please select your date of birth") }; return
        }
        if (!isAtLeast18(dob)) {
            _uiState.update { it.copy(error = "You must be at least 18 years old to register") }; return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.register(s.fullName.trim(), s.email.trim(), s.password, dob)
                .fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
                        onSuccess(user.role.name.lowercase())
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = friendlyError(e.message))
                        }
                    }
                )
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    fun login(onSuccess: (String) -> Unit) {
        val s = _uiState.value
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(s.email).matches()) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }; return
        }
        if (s.password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your password") }; return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(s.email.trim(), s.password)
                .fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
                        onSuccess(user.role.name.lowercase())
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = friendlyError(e.message))
                        }
                    }
                )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isAtLeast18(dobMillis: Long): Boolean {
        val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
        return age >= 18
    }

    private fun friendlyError(raw: String?): String = when {
        raw == null                                 -> "An unexpected error occurred"
        raw.contains("email address is already")   -> "This email is already registered"
        raw.contains("no user record")             -> "No account found with this email"
        raw.contains("password is invalid")        -> "Incorrect password"
        raw.contains("badly formatted")            -> "Please enter a valid email address"
        raw.contains("network")                    -> "Network error. Check your connection"
        else                                       -> raw
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AuthViewModel(FirebaseAuthRepository(context)) as T
    }
}
