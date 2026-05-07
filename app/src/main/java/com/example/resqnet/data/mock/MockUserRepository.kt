package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.UserRepository
import com.example.resqnet.domain.model.User
import com.example.resqnet.domain.model.VolunteerProfile
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay

class MockUserRepository : UserRepository {

    override suspend fun getUserById(id: String): Result<User> {
        delay(Constants.MOCK_DELAY_MS)
        val user = FakeData.users.find { it.id == id }
        return if (user != null) Result.success(user)
        else Result.failure(Exception("User not found"))
    }

    override suspend fun updateProfile(name: String, skills: List<String>?): Result<User> {
        delay(Constants.MOCK_DELAY_MS)
        val updated = FakeData.currentUser.copy(
            name = name,
            volunteerProfile = if (skills != null) {
                (FakeData.currentUser.volunteerProfile ?: VolunteerProfile()).copy(skills = skills)
            } else {
                FakeData.currentUser.volunteerProfile
            }
        )
        return Result.success(updated)
    }
}
