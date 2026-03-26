package com.example.resqnet.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.mock.MockSosRepository
import com.example.resqnet.data.repository.SosRepository
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: EmergencyType? = null,
    val description: String = "",
    val activeSos: SosRequest? = null,
    val currentStatus: SosStatus = SosStatus.PENDING,
    val myRequests: List<SosRequest> = emptyList(),
    val myResponses: List<SosRequest> = emptyList()
)

class SosViewModel(
    private val sosRepository: SosRepository = MockSosRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun selectType(type: EmergencyType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun createSos(onCreated: (String) -> Unit) {
        val type = _uiState.value.selectedType ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Mock location (New Delhi center)
            val result = sosRepository.createSos(
                type = type,
                latitude = 28.6139,
                longitude = 77.2090,
                description = _uiState.value.description.ifBlank { null }
            )
            result.fold(
                onSuccess = { sos ->
                    _uiState.update { it.copy(isLoading = false, activeSos = sos) }
                    onCreated(sos.id)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun loadActiveSos(sosId: String) {
        viewModelScope.launch {
            sosRepository.getSosById(sosId).onSuccess { sos ->
                _uiState.update { it.copy(activeSos = sos) }
            }
            // Observe status changes
            sosRepository.observeSosStatus(sosId).collect { status ->
                _uiState.update { state ->
                    state.copy(
                        currentStatus = status,
                        activeSos = state.activeSos?.copy(
                            status = status,
                            responderName = if (status == SosStatus.ACCEPTED) "Priya Patel" else state.activeSos?.responderName,
                            responderDistance = if (status == SosStatus.ACCEPTED) 0.8f else state.activeSos?.responderDistance
                        )
                    )
                }
            }
        }
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
}
