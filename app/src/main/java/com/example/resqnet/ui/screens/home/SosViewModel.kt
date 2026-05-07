package com.example.resqnet.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.firebase.FirebaseSosRepository
import com.example.resqnet.data.firebase.FirebaseVolunteerRepository
import com.example.resqnet.data.repository.SosRepository
import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.util.LatLng
import com.example.resqnet.util.LocationUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: EmergencyType? = null,
    val description: String = "",
    val activeSos: SosRequest? = null,
    val currentStatus: SosStatus = SosStatus.PENDING,
    val myRequests: List<SosRequest> = emptyList(),
    val myResponses: List<SosRequest> = emptyList(),
    // Map state
    val userLocation: LatLng? = null,
    val nearbyActiveSos: List<SosRequest> = emptyList(),
    // Volunteer mode
    val userIsVolunteer: Boolean = false,
    val volunteerModeOn: Boolean = false
)

class SosViewModel(
    private val sosRepository: SosRepository = FirebaseSosRepository(),
    private val volunteerRepository: VolunteerRepository = FirebaseVolunteerRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun selectType(type: EmergencyType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    /**
     * Fetches fresh GPS location each time (two-step: last-known → high-accuracy → Delhi fallback),
     * then writes the SOS to Firestore.
     */
    fun createSos(context: Context, onCreated: (String) -> Unit) {
        val type = _uiState.value.selectedType ?: run {
            _uiState.update { it.copy(error = "Please select an emergency type") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Always fetch a fresh location for the new request
            val location = LocationUtil.getCurrentLocation(context)
            // Also update the stored userLocation so the map re-centres if needed
            _uiState.update { it.copy(userLocation = location) }

            val result = sosRepository.createSos(
                type        = type,
                latitude    = location.latitude,
                longitude   = location.longitude,
                description = _uiState.value.description.ifBlank { null }
            )
            result.fold(
                onSuccess = { sos ->
                    _uiState.update { it.copy(isLoading = false, activeSos = sos, error = null) }
                    // Notify volunteers within the initial search radius
                    fanOutToVolunteers(sos, minRadiusKm = 0f, maxRadiusKm = sos.radiusKm)
                    onCreated(sos.id)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to send SOS: ${e.message ?: "Unknown error"}"
                        )
                    }
                }
            )
        }
    }

    fun loadActiveSos(sosId: String) {
        viewModelScope.launch {
            sosRepository.getSosById(sosId).onSuccess { sos ->
                _uiState.update { it.copy(activeSos = sos, currentStatus = sos.status) }
                // Start expansion only if SOS is still awaiting a response
                if (sos.status == SosStatus.PENDING || sos.status == SosStatus.NOTIFIED) {
                    startRadiusExpansion(sosId, sos.radiusKm)
                }
            }
            // Real-time listener for status changes (suspends forever)
            sosRepository.observeSosStatus(sosId).collect { status ->
                _uiState.update { state ->
                    state.copy(
                        currentStatus = status,
                        activeSos = state.activeSos?.copy(status = status)
                    )
                }
            }
        }

        // Fallback: if Cloud Functions are not deployed, advance PENDING → NOTIFIED client-side.
        viewModelScope.launch {
            kotlinx.coroutines.delay(4_000)
            if (_uiState.value.currentStatus == SosStatus.PENDING &&
                sosRepository is FirebaseSosRepository
            ) {
                sosRepository.advanceToNotified(sosId)
            }
        }
    }

    /**
     * Every 3 minutes while the SOS is still unresponded, expands the search radius by 5 km
     * up to the 25 km cap. Writing to Firestore triggers the Cloud Function which notifies
     * volunteers in the newly-added ring.
     */
    private fun startRadiusExpansion(sosId: String, initialRadiusKm: Float) {
        viewModelScope.launch {
            var radiusKm = initialRadiusKm
            while (true) {
                kotlinx.coroutines.delay(3 * 60 * 1000L) // 3 minutes
                val status = _uiState.value.currentStatus
                if (status != SosStatus.PENDING && status != SosStatus.NOTIFIED) break
                val prevRadius = radiusKm
                radiusKm = (radiusKm + 5f).coerceAtMost(25f)
                sosRepository.expandRadius(sosId, radiusKm)
                _uiState.update { it.copy(activeSos = it.activeSos?.copy(radiusKm = radiusKm)) }
                // Notify volunteers in the newly-added ring
                _uiState.value.activeSos?.let {
                    fanOutToVolunteers(it, minRadiusKm = prevRadius, maxRadiusKm = radiusKm)
                }
                if (radiusKm >= 25f) break
            }
        }
    }

    /**
     * Writes a notification record to each available volunteer's Firestore sub-collection
     * if they are within the radius ring [minRadiusKm, maxRadiusKm].
     * The volunteer's app picks these up via a snapshot listener and shows a local notification.
     * No server or paid plan required.
     */
    private suspend fun fanOutToVolunteers(
        sos: SosRequest,
        minRadiusKm: Float,
        maxRadiusKm: Float
    ) {
        try {
            val volunteers = firestore.collection("users")
                .whereEqualTo("role", "VOLUNTEER")
                .whereEqualTo("isAvailable", true)
                .get().await()

            for (doc in volunteers.documents) {
                if (doc.id == sos.requesterId) continue
                val data = doc.data ?: continue

                val lastLoc = data["lastLocation"] as? com.google.firebase.firestore.GeoPoint
                if (lastLoc != null) {
                    val dist = LocationUtil.haversineKm(
                        sos.latitude, sos.longitude,
                        lastLoc.latitude, lastLoc.longitude
                    )
                    val inRing = if (minRadiusKm == 0f) dist <= maxRadiusKm
                                 else dist > minRadiusKm && dist <= maxRadiusKm
                    if (!inRing) continue
                } else if (minRadiusKm > 0f) {
                    // Location unknown — skip on radius expansions to avoid duplicate spam
                    continue
                }

                firestore.collection("notifications").document(doc.id)
                    .collection("alerts")
                    .add(
                        mapOf(
                            "sosId"          to sos.id,
                            "emergencyType"  to sos.emergencyType.name,
                            "requesterName"  to sos.requesterName,
                            "addressHint"    to sos.addressHint,
                            "createdAt"      to com.google.firebase.Timestamp.now(),
                            "seen"           to false
                        )
                    ).await()
            }
        } catch (_: Exception) { }
    }

    fun cancelSos(sosId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            sosRepository.cancelSos(sosId)
            _uiState.update { it.copy(activeSos = null, currentStatus = SosStatus.CANCELLED) }
            onDone()
        }
    }

    fun resolveSos(sosId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            sosRepository.resolveSos(sosId)
            _uiState.update { it.copy(activeSos = null, currentStatus = SosStatus.RESOLVED) }
            onDone()
        }
    }

    fun loadMyRequests() {
        viewModelScope.launch {
            sosRepository.getMyRequests().onSuccess { list ->
                _uiState.update { it.copy(myRequests = list) }
            }
        }
    }

    fun loadMyResponses() {
        viewModelScope.launch {
            sosRepository.getMyResponses().onSuccess { list ->
                _uiState.update { it.copy(myResponses = list) }
            }
        }
    }

    /**
     * Fetches user GPS position, user volunteer status, and active SOS markers.
     * Uses the two-step location strategy so the map shows real location immediately.
     */
    fun initMap(context: Context) {
        viewModelScope.launch {
            val location = LocationUtil.getCurrentLocation(context)
            _uiState.update { it.copy(userLocation = location) }

            // Load user volunteer status + save last known location for Cloud Functions
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val snap = firestore.collection("users").document(uid).get().await()
                    val roleStr = snap.getString("role") ?: "REQUESTER"
                    val isAvailable = snap.getBoolean("isAvailable") ?: true
                    val isVolunteer = runCatching { UserRole.valueOf(roleStr) }
                        .getOrDefault(UserRole.REQUESTER) == UserRole.VOLUNTEER
                    _uiState.update {
                        it.copy(
                            userIsVolunteer = isVolunteer,
                            volunteerModeOn = isVolunteer && isAvailable
                        )
                    }
                    // Persist location so Cloud Functions can compute proximity
                    firestore.collection("users").document(uid).update(
                        "lastLocation",
                        com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude)
                    )
                } catch (_: Exception) { }
            }

            // Load active SOS markers — no orderBy to avoid index requirement
            if (sosRepository is FirebaseSosRepository) {
                sosRepository.getActiveAndRecent()
                    .onSuccess { list ->
                        _uiState.update { it.copy(nearbyActiveSos = list) }
                    }
                    .onFailure { e ->
                        android.util.Log.w("SosViewModel", "getNearbyActive failed: ${e.message}")
                    }
            }
        }
    }

    fun toggleVolunteerMode() {
        val newMode = !_uiState.value.volunteerModeOn
        viewModelScope.launch {
            volunteerRepository.setAvailability(newMode).onSuccess {
                _uiState.update { it.copy(volunteerModeOn = newMode) }
                if (newMode && sosRepository is FirebaseSosRepository) {
                    sosRepository.getActiveAndRecent().onSuccess { list ->
                        _uiState.update { it.copy(nearbyActiveSos = list) }
                    }
                }
            }
        }
    }
}
