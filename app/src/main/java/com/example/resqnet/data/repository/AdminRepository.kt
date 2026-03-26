package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.SosRequest

interface AdminRepository {
    /** Get all SOS requests (admin view). */
    suspend fun getAllSosRequests(): Result<List<SosRequest>>

    /** Get dashboard statistics. */
    suspend fun getDashboardStats(): Result<AdminStats>

    /** Ban a user. */
    suspend fun banUser(userId: String): Result<Boolean>

    /** Dismiss a flagged SOS request. */
    suspend fun dismissSos(sosId: String): Result<Boolean>
}

data class AdminStats(
    val totalSosToday: Int,
    val avgResponseTimeSec: Int,
    val activeVolunteers: Int,
    val flaggedRequests: Int,
    val totalUsers: Int,
    val resolvedToday: Int
)
