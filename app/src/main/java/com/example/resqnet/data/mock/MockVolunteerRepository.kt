package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.domain.model.VolunteerProfile
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay

class MockVolunteerRepository : VolunteerRepository {

    override suspend fun registerAsVolunteer(skills: List<String>): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        // Mock: update the current user to become a volunteer
        return Result.success(true)
    }

    override suspend fun setAvailability(available: Boolean): Result<Boolean> {
        delay(300)
        return Result.success(true)
    }

    override suspend fun getNearbySos(): Result<List<SosRequest>> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(
            FakeData.sosRequests.filter {
                it.status == SosStatus.PENDING || it.status == SosStatus.NOTIFIED
            }
        )
    }

    override suspend fun acceptSos(id: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        val index = FakeData.sosRequests.indexOfFirst { it.id == id }
        if (index != -1) {
            FakeData.sosRequests[index] = FakeData.sosRequests[index].copy(
                status = SosStatus.ACCEPTED,
                acceptedAt = System.currentTimeMillis(),
                responderId = FakeData.currentUser.id,
                responderName = FakeData.currentUser.name,
                responderDistance = 1.5f
            )
            return Result.success(true)
        }
        return Result.failure(Exception("SOS not found"))
    }

    override suspend fun declineSos(id: String): Result<Boolean> {
        delay(300)
        return Result.success(true)
    }
}
