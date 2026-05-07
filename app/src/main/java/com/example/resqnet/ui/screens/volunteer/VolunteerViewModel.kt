package com.example.resqnet.ui.screens.volunteer

import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.example.resqnet.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
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

    private var alertListener: ListenerRegistration? = null

    init {
        loadNearbySos()
        loadVolunteerStats()
        listenForAlerts()
    }

    // ── Firestore snapshot listener: fires a local notification for each new alert ──

    private fun listenForAlerts() {
        val uid = auth.currentUser?.uid ?: return
        alertListener = firestore.collection("notifications").document(uid)
            .collection("alerts")
            .whereEqualTo("seen", false)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        showLocalNotification(change.document.data ?: return@forEach)
                        change.document.reference.update("seen", true)
                    }
            }
    }

    private fun showLocalNotification(data: Map<String, Any?>) {
        val context = getApplication<Application>()
        val emergencyType = (data["emergencyType"] as? String ?: "EMERGENCY")
            .replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        val requesterName = data["requesterName"] as? String ?: "Someone"
        val hint = data["addressHint"] as? String
        val body = if (hint != null) "$requesterName needs help near $hint"
                   else "$requesterName needs help nearby"

        val notification = NotificationCompat.Builder(context, FcmService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 $emergencyType Alert")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) { }
    }

    // ── Existing volunteer logic ──────────────────────────────────────────────

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

    fun loadSosById(id: String) {
        viewModelScope.launch {
            try {
                val snap = firestore.collection("sos_requests").document(id).get().await()
                val sos = snap.data?.let { FirebaseSosRepository.docToSosStatic(id, it) }
                _uiState.update { it.copy(selectedSos = sos) }
            } catch (_: Exception) { }
        }
    }

    fun resolveSos(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("sos_requests").document(id).update(
                    mapOf(
                        "status" to SosStatus.RESOLVED.name,
                        "resolvedAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
                val uid = auth.currentUser?.uid
                if (uid != null) {
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
                }
            } catch (_: Exception) { }
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        alertListener?.remove()
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
