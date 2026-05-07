package com.example.resqnet.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.firebase.FirebaseAuthRepository
import com.example.resqnet.data.firebase.FirebaseVolunteerRepository
import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.User
import com.example.resqnet.domain.model.VolunteerProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

val VOLUNTEER_SKILLS = listOf(
    "First Aid", "CPR", "Firefighting", "Medical",
    "Navigation", "Search & Rescue", "Counseling", "Driver"
)

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Edit name
    val isEditing: Boolean = false,
    val editName: String = "",
    // Volunteer registration
    val isRegisteringVolunteer: Boolean = false,
    val registerPhone: String = "",
    val registerSelectedSkills: Set<String> = emptySet(),
    val isVolunteerRegistering: Boolean = false,
    val registerError: String? = null
)

class ProfileViewModel(
    private val context: Context,
    private val volunteerRepository: VolunteerRepository = FirebaseVolunteerRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not signed in") }
                return@launch
            }
            try {
                val snap = firestore.collection("users").document(uid).get().await()
                val data = snap.data ?: emptyMap()
                var user = FirebaseAuthRepository.snapshotToUser(uid, data)

                // Recalculate reliability score from stats and persist if changed
                val vp = user.volunteerProfile
                if (vp != null && vp.totalResponses > 0) {
                    val computed = computeReliabilityScore(vp)
                    if (computed != vp.reliabilityScore) {
                        user = user.copy(volunteerProfile = vp.copy(reliabilityScore = computed))
                        viewModelScope.launch {
                            runCatching {
                                firestore.collection("users").document(uid)
                                    .update("volunteerProfile.reliabilityScore", computed.toDouble())
                                    .await()
                            }
                        }
                    }
                }

                _uiState.update { it.copy(isLoading = false, user = user, editName = user.name) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Edit name ────────────────────────────────────────────────────────────

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, editName = it.user?.name ?: "") }
    }

    fun onEditNameChanged(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun saveProfile(onSaved: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("name", name).await()
                _uiState.update { state ->
                    state.copy(
                        isEditing = false,
                        user = state.user?.copy(name = name)
                    )
                }
                onSaved()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    // ── Volunteer registration ───────────────────────────────────────────────

    fun startVolunteerRegistration() {
        _uiState.update {
            it.copy(
                isRegisteringVolunteer = true,
                registerPhone = "",
                registerSelectedSkills = emptySet(),
                registerError = null
            )
        }
    }

    fun onRegisterPhoneChanged(phone: String) {
        _uiState.update { it.copy(registerPhone = phone, registerError = null) }
    }

    fun toggleSkill(skill: String) {
        _uiState.update { state ->
            val updated = if (skill in state.registerSelectedSkills)
                state.registerSelectedSkills - skill
            else
                state.registerSelectedSkills + skill
            state.copy(registerSelectedSkills = updated)
        }
    }

    fun cancelVolunteerRegistration() {
        _uiState.update { it.copy(isRegisteringVolunteer = false, registerError = null) }
    }

    fun submitVolunteerRegistration(onSuccess: () -> Unit) {
        val phone = _uiState.value.registerPhone.trim()
        val digits = phone.filter { it.isDigit() }
        if (digits.length != 10) {
            _uiState.update { it.copy(registerError = "Enter a valid 10-digit phone number") }
            return
        }
        if (_uiState.value.registerSelectedSkills.isEmpty()) {
            _uiState.update { it.copy(registerError = "Select at least one skill") }
            return
        }

        _uiState.update { it.copy(isVolunteerRegistering = true, registerError = null) }
        viewModelScope.launch {
            volunteerRepository.registerAsVolunteer(
                phone = digits,
                skills = _uiState.value.registerSelectedSkills.toList()
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isVolunteerRegistering = false, isRegisteringVolunteer = false) }
                    loadProfile()
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isVolunteerRegistering = false, registerError = e.message ?: "Registration failed")
                    }
                }
            )
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    fun logout(onLoggedOut: () -> Unit) {
        auth.signOut()
        context.getSharedPreferences("resqnet_auth_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        onLoggedOut()
    }

    // ── Reliability score ────────────────────────────────────────────────────

    companion object {
        fun computeReliabilityScore(profile: VolunteerProfile): Float {
            if (profile.totalResponses == 0) return 50f
            val successRate = (profile.successfulResponses.toFloat() / profile.totalResponses).coerceIn(0f, 1f)
            val successPts = successRate * 40f
            val speedPts = when (profile.avgResponseTimeSec) {
                null         -> 15f
                in 0..60     -> 30f
                in 61..180   -> 22f
                in 181..360  -> 14f
                in 361..600  -> 7f
                else         -> 2f
            }
            val activityPts = (profile.totalResponses.toFloat() / 20f).coerceIn(0f, 1f) * 30f
            return (successPts + speedPts + activityPts).coerceIn(0f, 100f)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(context) as T
    }
}
