package com.example.resqnet.data.repository

import com.example.resqnet.domain.model.SosRequest

interface VolunteerRepository {
    /** Register the current user as a volunteer with given skills. */
    suspend fun registerAsVolunteer(skills: List<String>): Result<Boolean>

    /** Toggle volunteer availability. */
    suspend fun setAvailability(available: Boolean): Result<Boolean>

    /** Get nearby active SOS requests (for volunteer view). */
    suspend fun getNearbySos(): Result<List<SosRequest>>

    /** Accept an SOS request. */
    suspend fun acceptSos(id: String): Result<Boolean>

    /** Decline an SOS request. */
    suspend fun declineSos(id: String): Result<Boolean>
}
