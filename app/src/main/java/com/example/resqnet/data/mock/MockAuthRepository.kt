package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.AuthRepository
import com.example.resqnet.domain.model.User
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {

    private var loggedInUser: User? = null

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        dobMillis: Long
    ): Result<User> {
        delay(Constants.MOCK_DELAY_MS)
        val user = FakeData.currentUser.copy(name = fullName)
        loggedInUser = user
        return Result.success(user)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        delay(Constants.MOCK_DELAY_MS)
        val user = when {
            email.contains("admin")     -> FakeData.users.first { it.role == UserRole.ADMIN }
            email.contains("volunteer") -> FakeData.users.first { it.role == UserRole.VOLUNTEER }
            else                        -> FakeData.currentUser.copy(phone = email)
        }
        loggedInUser = user
        return Result.success(user)
    }

    override fun getCurrentUser(): User? = loggedInUser

    override fun needsReAuth(): Boolean = loggedInUser == null

    override fun logout() {
        loggedInUser = null
    }
}
