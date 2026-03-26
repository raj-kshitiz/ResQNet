package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.User

interface UserRepository {
    /** Get user profile by ID. */
    suspend fun getUserById(id: String): Result<User>

    /** Update user profile (name, avatar, etc.). */
    suspend fun updateProfile(name: String, skills: List<String>? = null): Result<User>
}
