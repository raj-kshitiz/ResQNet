package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.User

interface AuthRepository {
    /** Send OTP to the given phone number. Returns true on success. */
    suspend fun sendOtp(phone: String): Result<Boolean>

    /** Verify OTP. Returns the authenticated User on success. */
    suspend fun verifyOtp(phone: String, otp: String): Result<User>

    /** Get the currently logged-in user, or null. */
    fun getCurrentUser(): User?

    /** Log out and clear session. */
    fun logout()
}
