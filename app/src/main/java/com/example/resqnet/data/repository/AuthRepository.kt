package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.User

interface AuthRepository {

    /** Register a new user with email + password. Returns the created User on success. */
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        dobMillis: Long
    ): Result<User>

    /** Sign in an existing user. Returns the authenticated User on success. */
    suspend fun login(email: String, password: String): Result<User>

    /** Get the currently signed-in Firebase user, or null. */
    fun getCurrentUser(): User?

    /** Returns true if the user must re-authenticate (session > 7 days old or not signed in). */
    fun needsReAuth(): Boolean

    /** Log out and clear local session timestamp. */
    fun logout()
}
