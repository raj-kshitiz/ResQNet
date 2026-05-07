package com.example.resqnet.data.mock

import com.example.resqnet.domain.model.*

object FakeData {

    // ── Current logged-in user ──
    var currentUser = User(
        id = "u1",
        phone = "+919876543210",
        name = "Kshitiz",
        role = UserRole.REQUESTER,
        isVerified = true,
        isAvailable = true,
        volunteerProfile = null
    )

    // ── Sample Users ──
    val users = listOf(
        currentUser,
        User(
            id = "u2", phone = "+919876543211", name = "Rahul Sharma",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = true,
            volunteerProfile = VolunteerProfile(
                skills = listOf("First Aid", "CPR"),
                reliabilityScore = 87f, totalResponses = 24,
                successfulResponses = 21, avgResponseTimeSec = 95
            )
        ),
        User(
            id = "u3", phone = "+919876543212", name = "Priya Patel",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = true,
            volunteerProfile = VolunteerProfile(
                skills = listOf("Blood Donor (O+)", "First Aid"),
                reliabilityScore = 92f, totalResponses = 38,
                successfulResponses = 36, avgResponseTimeSec = 72
            )
        ),
        User(
            id = "u4", phone = "+919876543213", name = "Amit Kumar",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = false,
            volunteerProfile = VolunteerProfile(
                skills = listOf("Paramedic"),
                reliabilityScore = 78f, totalResponses = 15,
                successfulResponses = 12, avgResponseTimeSec = 140
            )
        ),
        User(
            id = "u5", phone = "+919876543214", name = "Admin User",
            role = UserRole.ADMIN, isVerified = true
        ),
        User(
            id = "u6", phone = "+919876543215", name = "Neha Singh",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = true,
            volunteerProfile = VolunteerProfile(
                skills = listOf("Nurse", "CPR", "First Aid"),
                reliabilityScore = 95f, totalResponses = 52,
                successfulResponses = 50, avgResponseTimeSec = 60
            )
        ),
        User(
            id = "u7", phone = "+919876543216", name = "Vikram Joshi",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = true,
            volunteerProfile = VolunteerProfile(
                skills = listOf("Blood Donor (B+)"),
                reliabilityScore = 65f, totalResponses = 8,
                successfulResponses = 5, avgResponseTimeSec = 180
            )
        ),
        User(
            id = "u8", phone = "+919876543217", name = "Anita Desai",
            role = UserRole.VOLUNTEER, isVerified = true, isAvailable = true,
            volunteerProfile = VolunteerProfile(
                skills = listOf("First Aid", "Fire Safety"),
                reliabilityScore = 80f, totalResponses = 20,
                successfulResponses = 17, avgResponseTimeSec = 110
            )
        )
    )

    val volunteers get() = users.filter { it.role == UserRole.VOLUNTEER }

    // ── Sample SOS Requests ──
    val sosRequests = mutableListOf(
        SosRequest(
            id = "sos1", requesterId = "u1", requesterName = "Kshitiz",
            emergencyType = EmergencyType.MEDICAL,
            description = "Severe chest pain, need immediate help",
            latitude = 28.6139, longitude = 77.2090,
            addressHint = "Connaught Place, New Delhi",
            status = SosStatus.RESOLVED,
            createdAt = System.currentTimeMillis() - 86400000,
            resolvedAt = System.currentTimeMillis() - 86000000,
            responderId = "u2", responderName = "Rahul Sharma",
            responderDistance = 1.2f
        ),
        SosRequest(
            id = "sos2", requesterId = "u1", requesterName = "Kshitiz",
            emergencyType = EmergencyType.ACCIDENT,
            description = "Minor road accident, need assistance",
            latitude = 28.6280, longitude = 77.2197,
            addressHint = "Rajiv Chowk, New Delhi",
            status = SosStatus.RESOLVED,
            createdAt = System.currentTimeMillis() - 172800000,
            resolvedAt = System.currentTimeMillis() - 172400000,
            responderId = "u3", responderName = "Priya Patel",
            responderDistance = 0.8f
        ),
        SosRequest(
            id = "sos3", requesterId = "u1", requesterName = "Kshitiz",
            emergencyType = EmergencyType.BLOOD_REQUEST,
            description = "Need O+ blood urgently for surgery",
            latitude = 28.6353, longitude = 77.2250,
            addressHint = "AIIMS, New Delhi",
            status = SosStatus.CANCELLED,
            createdAt = System.currentTimeMillis() - 259200000
        ),
        SosRequest(
            id = "sos4", requesterId = "u9", requesterName = "Sanjay Gupta",
            emergencyType = EmergencyType.MEDICAL,
            description = "Diabetic emergency, low sugar",
            latitude = 28.6200, longitude = 77.2100,
            addressHint = "Janpath, New Delhi",
            status = SosStatus.PENDING,
            createdAt = System.currentTimeMillis() - 120000
        ),
        SosRequest(
            id = "sos5", requesterId = "u10", requesterName = "Meera Reddy",
            emergencyType = EmergencyType.SAFETY_ALERT,
            description = "Feeling unsafe, need escort",
            latitude = 28.6100, longitude = 77.2300,
            addressHint = "India Gate, New Delhi",
            status = SosStatus.PENDING,
            createdAt = System.currentTimeMillis() - 60000
        ),
        SosRequest(
            id = "sos6", requesterId = "u11", requesterName = "Arjun Nair",
            emergencyType = EmergencyType.ACCIDENT,
            description = "Bike skid, minor injuries",
            latitude = 28.6450, longitude = 77.2160,
            addressHint = "Kashmere Gate, New Delhi",
            status = SosStatus.ACCEPTED,
            createdAt = System.currentTimeMillis() - 300000,
            responderId = "u2", responderName = "Rahul Sharma",
            responderDistance = 1.8f
        ),
        SosRequest(
            id = "sos7", requesterId = "u12", requesterName = "Pooja Mishra",
            emergencyType = EmergencyType.BLOOD_REQUEST,
            description = "B+ blood needed at hospital",
            latitude = 28.5800, longitude = 77.2350,
            addressHint = "Safdarjung Hospital, New Delhi",
            status = SosStatus.PENDING,
            createdAt = System.currentTimeMillis() - 90000
        )
    )

    // Helpers
    val volunteerDistances = mapOf(
        "u2" to 1.2f, "u3" to 0.8f, "u4" to 2.5f,
        "u6" to 0.5f, "u7" to 3.1f, "u8" to 1.9f
    )
}
