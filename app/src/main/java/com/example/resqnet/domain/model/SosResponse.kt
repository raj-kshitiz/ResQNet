package com.example.resqnet.domain.model

data class SosResponse(
    val id: String,
    val sosRequestId: String,
    val volunteerId: String,
    val volunteerName: String,
    val action: ResponseAction,
    val respondedAt: Long = System.currentTimeMillis(),
    val distanceMeters: Float? = null
)

enum class ResponseAction {
    NOTIFIED,
    ACCEPTED,
    DECLINED,
    TIMED_OUT
}
