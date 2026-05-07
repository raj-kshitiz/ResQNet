package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.AdminRepository
import com.example.resqnet.data.repository.AdminStats
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay

class MockAdminRepository : AdminRepository {

    override suspend fun getAllSosRequests(): Result<List<SosRequest>> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(FakeData.sosRequests.toList())
    }

    override suspend fun getDashboardStats(): Result<AdminStats> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(
            AdminStats(
                totalSosToday = 23,
                avgResponseTimeSec = 94,
                activeVolunteers = FakeData.volunteers.count { it.isAvailable },
                flaggedRequests = 2,
                totalUsers = FakeData.users.size,
                resolvedToday = FakeData.sosRequests.count { it.status == SosStatus.RESOLVED }
            )
        )
    }

    override suspend fun banUser(userId: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        return Result.success(true)
    }

    override suspend fun dismissSos(sosId: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        val index = FakeData.sosRequests.indexOfFirst { it.id == sosId }
        if (index != -1) {
            FakeData.sosRequests[index] = FakeData.sosRequests[index].copy(
                status = SosStatus.CANCELLED
            )
            return Result.success(true)
        }
        return Result.failure(Exception("SOS not found"))
    }
}
