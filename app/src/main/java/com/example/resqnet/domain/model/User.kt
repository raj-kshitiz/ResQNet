package com.example.resqnet.domain.model

data class User(
    val id: String,
    val phone: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: UserRole = UserRole.REQUESTER,
    val isVerified: Boolean = false,
    val isAvailable: Boolean = true,
    val volunteerProfile: VolunteerProfile? = null
)

enum class UserRole {
    REQUESTER,
    VOLUNTEER,
    ADMIN
}

data class VolunteerProfile(
    val skills: List<String> = emptyList(),
    val reliabilityScore: Float = 50f,
    val totalResponses: Int = 0,
    val successfulResponses: Int = 0,
    val avgResponseTimeSec: Int? = null
)
