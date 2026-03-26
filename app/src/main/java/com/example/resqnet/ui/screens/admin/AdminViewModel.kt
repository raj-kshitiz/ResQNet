package com.example.resqnet.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqnet.data.mock.MockAdminRepository
import com.example.resqnet.data.repository.AdminRepository
import com.example.resqnet.data.repository.AdminStats
import com.example.resqnet.domain.model.SosRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val stats: AdminStats? = null,
    val allRequests: List<SosRequest> = emptyList(),
    val error: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository = MockAdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            adminRepository.getDashboardStats().onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
            adminRepository.getAllSosRequests().onSuccess { requests ->
                _uiState.update { it.copy(allRequests = requests, isLoading = false) }
            }
        }
    }
}
