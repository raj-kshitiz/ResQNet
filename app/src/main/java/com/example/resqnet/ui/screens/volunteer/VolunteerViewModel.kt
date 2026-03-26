package com.example.resqnet.ui.screens.volunteer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.mock.MockVolunteerRepository
import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.SosRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolunteerUiState(
    val isAvailable: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val nearbySos: List<SosRequest> = emptyList(),
    val activeSos: SosRequest? = null
)

class VolunteerViewModel(
    private val volunteerRepository: VolunteerRepository = MockVolunteerRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VolunteerUiState())
    val uiState: StateFlow<VolunteerUiState> = _uiState.asStateFlow()

    init {
        loadNearbySos()
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
}
