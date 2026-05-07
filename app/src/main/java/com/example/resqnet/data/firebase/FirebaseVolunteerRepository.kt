package com.example.resqnet.data.firebase

import com.example.resqnet.data.repository.VolunteerRepository
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseVolunteerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VolunteerRepository {

    private val usersCol get() = firestore.collection("users")
    private val sosCol get() = firestore.collection("sos_requests")
    private val currentUid get() = auth.currentUser?.uid ?: error("Not authenticated")

    override suspend fun registerAsVolunteer(phone: String, skills: List<String>): Result<Boolean> = runCatching {
        val uid = currentUid
        usersCol.document(uid).update(
            mapOf(
                "role" to "VOLUNTEER",
                "phone" to phone,
                "volunteerProfile" to mapOf(
                    "skills" to skills,
                    "reliabilityScore" to 50.0,
                    "totalResponses" to 0,
                    "successfulResponses" to 0,
                    "avgResponseTimeSec" to null
                )
            )
        ).await()
        true
    }

    override suspend fun setAvailability(available: Boolean): Result<Boolean> = runCatching {
        usersCol.document(currentUid).update("isAvailable", available).await()
        true
    }

    override suspend fun getNearbySos(): Result<List<SosRequest>> = runCatching {
        // Fetch all PENDING or NOTIFIED SOS requests.
        // In a production app you'd filter by GeoHash/radius; here we load all active ones
        // and let the UI/ViewModel further filter by distance if needed.
        sosCol.whereIn("status", listOf(SosStatus.PENDING.name, SosStatus.NOTIFIED.name))
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.data?.let { FirebaseSosRepository.docToSosStatic(doc.id, it) }
            }
    }

    override suspend fun acceptSos(id: String): Result<Boolean> = runCatching {
        val uid = currentUid
        val userSnap = usersCol.document(uid).get().await()
        val responderName = userSnap.getString("name") ?: "Volunteer"

        sosCol.document(id).update(
            mapOf(
                "status" to SosStatus.ACCEPTED.name,
                "responderId" to uid,
                "responderName" to responderName,
                "acceptedAt" to Timestamp.now()
            )
        ).await()
        true
    }

    override suspend fun declineSos(id: String): Result<Boolean> = runCatching {
        // Volunteer declines — no status change on the SOS request itself
        true
    }
}
