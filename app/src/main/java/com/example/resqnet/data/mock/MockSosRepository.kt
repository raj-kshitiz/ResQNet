package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.SosRepository
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class MockSosRepository : SosRepository {

    override suspend fun createSos(
        type: EmergencyType,
        latitude: Double,
        longitude: Double,
        description: String?
    ): Result<SosRequest> {
        delay(Constants.MOCK_DELAY_MS)
        val newSos = SosRequest(
            id = UUID.randomUUID().toString().take(8),
            requesterId = FakeData.currentUser.id,
            requesterName = FakeData.currentUser.name,
            emergencyType = type,
            description = description,
            latitude = latitude,
            longitude = longitude,
            addressHint = "Near your location",
            status = SosStatus.PENDING
        )
        FakeData.sosRequests.add(0, newSos)
        return Result.success(newSos)
    }

    override suspend fun getSosById(id: String): Result<SosRequest> {
        delay(300)
        val sos = FakeData.sosRequests.find { it.id == id }
        return if (sos != null) Result.success(sos)
        else Result.failure(Exception("SOS not found"))
    }

    override suspend fun cancelSos(id: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        val index = FakeData.sosRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            FakeData.sosRequests[index] = FakeData.sosRequests[index].copy(
                status = SosStatus.CANCELLED
            )
            return Result.success(true)
        }
        return Result.failure(Exception("SOS not found"))
    }

    override suspend fun resolveSos(id: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        val index = FakeData.sosRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            FakeData.sosRequests[index] = FakeData.sosRequests[index].copy(
                status = SosStatus.RESOLVED,
                resolvedAt = System.currentTimeMillis()
            )
            return Result.success(true)
        }
        return Result.failure(Exception("SOS not found"))
    }

    override suspend fun getMyRequests(): Result<List<SosRequest>> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(
            FakeData.sosRequests.filter { it.requesterId == FakeData.currentUser.id }
        )
    }

    override suspend fun getMyResponses(): Result<List<SosRequest>> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(
            FakeData.sosRequests.filter { it.responderId == FakeData.currentUser.id }
        )
    }

    override fun observeSosStatus(id: String): Flow<SosStatus> = flow {
        // Simulate status progression
        emit(SosStatus.PENDING)
        delay(2000)
        emit(SosStatus.NOTIFIED)
        delay(3000)
        emit(SosStatus.ACCEPTED)
        delay(2000)
        emit(SosStatus.IN_PROGRESS)
        // Stays in IN_PROGRESS until user resolves
    }
}
