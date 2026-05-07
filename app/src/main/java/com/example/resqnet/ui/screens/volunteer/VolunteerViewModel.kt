package com.example.resqnet.ui.screens.volunteer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.resqnet.data.firebase.FirebaseSosRepository
import com.example.resqnet.data.firebase.FirebaseVolunteerRepository
import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class VolunteerUiState(
    val isAvailable: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val nearbySos: List<SosRequest> = emptyList(),
    val activeSos: SosRequest? = null,
    val selectedSos: SosRequest? = null,
    val totalResponses: Int = 0,
    val reliabilityScore: Float = 0f
)

class VolunteerViewModel(
    application: Application,
    private val volunteerRepository: VolunteerRepository = FirebaseVolunteerRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VolunteerUiState())
    val uiState: StateFlow<VolunteerUiState> = _uiState.asStateFlow()

    private var selectedSosListener: ListenerRegistration? = null

    init {
        loadNearbySos()
        loadVolunteerStats()
    }

    private fun loadVolunteerStats() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val snap = firestore.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                val profile = snap.get("volunteerProfile") as? Map<String, Any?> ?: return@launch
                val score = (profile["reliabilityScore"] as? Number)?.toFloat() ?: 50f
                val total = (profile["totalResponses"] as? Number)?.toInt() ?: 0
                val isAvailable = snap.getBoolean("isAvailable") ?: true
                _uiState.update { it.copy(reliabilityScore = score, totalResponses = total, isAvailable = isAvailable) }
            } catch (_: Exception) { }
        }
    }

    fun toggleAvailability() {
        val newAvailability = !_uiState.value.isAvailable
        viewModelScope.launch {
            volunteerRepository.setAvailability(newAvailability).onSuccess {
                _uiState.update { it.copy(isAvailable = newAvailability) }
                if (newAvailability) loadNearbySos()
                else _uiState.update { it.copy(nearbySos = emptyList()) }
            }
        }
    }

    fun loadNearbySos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            volunteerRepository.getNearbySos().fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(isLoading = false, nearbySos = list) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun acceptSos(id: String, onAccepted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            volunteerRepository.acceptSos(id).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onAccepted()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun declineSos(id: String, onDeclined: () -> Unit) {
        viewModelScope.launch {
            volunteerRepository.declineSos(id)
            onDeclined()
        }
    }

    // Real-time listener so ActiveResponseScreen reacts to status changes (e.g. requester resolves)
    fun loadSosById(id: String) {
        selectedSosListener?.remove()
        selectedSosListener = firestore.collection("sos_requests").document(id)
            .addSnapshotListener { snap, _ ->
                val sos = snap?.data?.let { FirebaseSosRepository.docToSosStatic(id, it) }
                val prevStatus = _uiState.value.selectedSos?.status
                _uiState.update { it.copy(selectedSos = sos) }
                // Update volunteer stats when requester resolves the SOS
                if (sos?.status == SosStatus.RESOLVED && prevStatus != SosStatus.RESOLVED) {
                    updateVolunteerStats()
                }
            }
    }

    fun setOnTheWay(id: String) {
        viewModelScope.launch {
            try {
                firestore.collection("sos_requests").document(id)
                    .update("status", SosStatus.IN_PROGRESS.name).await()
            } catch (_: Exception) { }
        }
    }

    private fun updateVolunteerStats() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val snap = firestore.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                val profile = snap.get("volunteerProfile") as? Map<String, Any?> ?: emptyMap()
                val total = ((profile["totalResponses"] as? Number)?.toInt() ?: 0) + 1
                val successful = ((profile["successfulResponses"] as? Number)?.toInt() ?: 0) + 1
                val score = (successful.toFloat() / total * 100f).coerceAtMost(100f)
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "volunteerProfile.totalResponses" to total,
                        "volunteerProfile.successfulResponses" to successful,
                        "volunteerProfile.reliabilityScore" to score
                    )
                ).await()
                _uiState.update { it.copy(totalResponses = total, reliabilityScore = score) }
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        selectedSosListener?.remove()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return VolunteerViewModel(app) as T
            }
        }
    }
}
