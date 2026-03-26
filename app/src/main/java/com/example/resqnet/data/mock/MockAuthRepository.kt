package com.example.resqnet.data.mock

import com.example.resqnet.data.repository.AuthRepository
import com.example.resqnet.domain.model.User
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.util.Constants
import kotlinx.coroutines.delay

class MockAuthRepository : AuthRepository {

    private var loggedInUser: User? = null

    override suspend fun sendOtp(phone: String): Result<Boolean> {
        delay(Constants.MOCK_DELAY_MS)
        // Mock: always succeeds for any 10-digit number
        return if (phone.replace("+91", "").length == 10) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid phone number"))
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String): Result<User> {
        delay(Constants.MOCK_DELAY_MS)
        // Mock: any 6-digit OTP is accepted
        if (otp.length != 6) {
            return Result.failure(Exception("Invalid OTP"))
        }

        // Return different roles based on phone suffix for testing
        val user = when {
            phone.endsWith("0000") -> FakeData.users.first { it.role == UserRole.ADMIN }
            phone.endsWith("1111") -> FakeData.users.first { it.role == UserRole.VOLUNTEER }
            else -> FakeData.currentUser.copy(phone = phone)
        }

        loggedInUser = user
        return Result.success(user)
    }

    override fun getCurrentUser(): User? = loggedInUser

    override fun logout() {
        loggedInUser = null
    }
}
