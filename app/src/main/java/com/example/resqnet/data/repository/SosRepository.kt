package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import kotlinx.coroutines.flow.Flow

interface SosRepository {
    /** Create a new SOS request. */
    suspend fun createSos(
        type: EmergencyType,
        latitude: Double,
        longitude: Double,
        description: String? = null
    ): Result<SosRequest>

    /** Get an SOS request by ID. */
    suspend fun getSosById(id: String): Result<SosRequest>

    /** Cancel an existing SOS request. */
    suspend fun cancelSos(id: String): Result<Boolean>

    /** Mark an SOS request as resolved. */
    suspend fun resolveSos(id: String): Result<Boolean>

    /** Get all SOS requests created by the current user. */
    suspend fun getMyRequests(): Result<List<SosRequest>>

    /** Get all SOS requests the current user responded to. */
    suspend fun getMyResponses(): Result<List<SosRequest>>

    /** Observe real-time status changes for a given SOS request. */
    fun observeSosStatus(id: String): Flow<SosStatus>

    /** Expand the search radius on an active SOS request. Cloud Functions pick this up. */
    suspend fun expandRadius(id: String, radiusKm: Float): Result<Boolean>
}
