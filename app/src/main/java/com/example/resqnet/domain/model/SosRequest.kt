package com.example.resqnet.domain.model

data class SosRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val emergencyType: EmergencyType,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,
    val addressHint: String? = null,
    val status: SosStatus = SosStatus.PENDING,
    val radiusKm: Float = 3.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val resolvedAt: Long? = null,
    val responderId: String? = null,
    val responderName: String? = null,
    val responderDistance: Float? = null
)

enum class EmergencyType(val label: String) {
    MEDICAL("Medical"),
    ACCIDENT("Accident"),
    BLOOD_REQUEST("Blood Request"),
    SAFETY_ALERT("Safety Alert"),
    OTHER("Other")
}

enum class SosStatus(val label: String) {
    PENDING("Pending"),
    NOTIFIED("Notified"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired")
}
