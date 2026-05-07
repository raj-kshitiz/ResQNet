package com.example.resqnet.data.firebase

import com.example.resqnet.data.repository.SosRepository
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SosRepository {

    private val col get() = firestore.collection("sos_requests")
    private val currentUid get() = auth.currentUser?.uid ?: error("Not authenticated")

    override suspend fun createSos(
        type: EmergencyType,
        latitude: Double,
        longitude: Double,
        description: String?
    ): Result<SosRequest> = runCatching {
        val uid = currentUid
        val userSnap = firestore.collection("users").document(uid).get().await()
        val requesterName = userSnap.getString("name") ?: "User"

        val data = mutableMapOf<String, Any?>(
            "requesterId" to uid,
            "requesterName" to requesterName,
            "emergencyType" to type.name,
            "description" to description,
            "location" to GeoPoint(latitude, longitude),
            "addressHint" to null,
            "status" to SosStatus.PENDING.name,
            "radiusKm" to 3.0,
            "createdAt" to Timestamp.now(),
            "acceptedAt" to null,
            "resolvedAt" to null,
            "responderId" to null,
            "responderName" to null,
            "responderDistance" to null
        )

        val ref = col.add(data).await()
        SosRequest(
            id = ref.id,
            requesterId = uid,
            requesterName = requesterName,
            emergencyType = type,
            description = description,
            latitude = latitude,
            longitude = longitude,
            status = SosStatus.PENDING
        )
    }

    override suspend fun getSosById(id: String): Result<SosRequest> = runCatching {
        val snap = col.document(id).get().await()
        snap.data?.let { docToSos(id, it) } ?: error("SOS not found")
    }

    override suspend fun cancelSos(id: String): Result<Boolean> = runCatching {
        col.document(id).update("status", SosStatus.CANCELLED.name).await()
        true
    }

    override suspend fun resolveSos(id: String): Result<Boolean> = runCatching {
        col.document(id).update(
            mapOf(
                "status" to SosStatus.RESOLVED.name,
                "resolvedAt" to Timestamp.now()
            )
        ).await()
        true
    }

    /**
     * Simulates the backend notification sweep: advances a PENDING SOS to NOTIFIED.
     * In production this would be done by a Firebase Cloud Function.
     */
    suspend fun advanceToNotified(id: String): Result<Boolean> = runCatching {
        col.document(id).update("status", SosStatus.NOTIFIED.name).await()
        true
    }

    override suspend fun getMyRequests(): Result<List<SosRequest>> = runCatching {
        val uid = currentUid
        col.whereEqualTo("requesterId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .documents
            .mapNotNull { it.data?.let { d -> docToSos(it.id, d) } }
    }

    override suspend fun getMyResponses(): Result<List<SosRequest>> = runCatching {
        val uid = currentUid
        col.whereEqualTo("responderId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .documents
            .mapNotNull { it.data?.let { d -> docToSos(it.id, d) } }
    }

    /** Returns PENDING/NOTIFIED SOS requests — used for the volunteer dashboard. */
    suspend fun getNearbyActive(): Result<List<SosRequest>> = runCatching {
        col.whereIn("status", listOf(SosStatus.PENDING.name, SosStatus.NOTIFIED.name))
            .get().await()
            .documents
            .mapNotNull { it.data?.let { d -> docToSos(it.id, d) } }
    }

    /**
     * Returns all active requests plus RESOLVED ones within the last 2 minutes.
     * Used for the requester map view (color-coded by status).
     */
    suspend fun getActiveAndRecent(): Result<List<SosRequest>> = runCatching {
        val twoMinAgo = System.currentTimeMillis() - 2 * 60 * 1000L
        col.whereIn(
            "status",
            listOf(
                SosStatus.PENDING.name,
                SosStatus.NOTIFIED.name,
                SosStatus.ACCEPTED.name,
                SosStatus.IN_PROGRESS.name,
                SosStatus.RESOLVED.name
            )
        ).get().await()
            .documents
            .mapNotNull { it.data?.let { d -> docToSos(it.id, d) } }
            .filter { sos ->
                sos.status != SosStatus.RESOLVED || (sos.resolvedAt ?: 0L) >= twoMinAgo
            }
    }

    override suspend fun expandRadius(id: String, radiusKm: Float): Result<Boolean> = runCatching {
        col.document(id).update("radiusKm", radiusKm.toDouble()).await()
        true
    }

    override fun observeSosStatus(id: String): Flow<SosStatus> = callbackFlow {
        val listener = col.document(id).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val statusStr = snap?.getString("status") ?: return@addSnapshotListener
            val status = runCatching { SosStatus.valueOf(statusStr) }.getOrDefault(SosStatus.PENDING)
            trySend(status)
        }
        awaitClose { listener.remove() }
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private fun docToSos(id: String, data: Map<String, Any?>): SosRequest =
        docToSosStatic(id, data)

    companion object {
        fun docToSosStatic(id: String, data: Map<String, Any?>): SosRequest {
            val location = data["location"] as? GeoPoint
            val type = runCatching {
                EmergencyType.valueOf(data["emergencyType"] as? String ?: "OTHER")
            }.getOrDefault(EmergencyType.OTHER)
            val status = runCatching {
                SosStatus.valueOf(data["status"] as? String ?: "PENDING")
            }.getOrDefault(SosStatus.PENDING)

            return SosRequest(
                id = id,
                requesterId = data["requesterId"] as? String ?: "",
                requesterName = data["requesterName"] as? String ?: "Unknown",
                emergencyType = type,
                description = data["description"] as? String,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                addressHint = data["addressHint"] as? String,
                status = status,
                radiusKm = (data["radiusKm"] as? Number)?.toFloat() ?: 3f,
                createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                acceptedAt = (data["acceptedAt"] as? Timestamp)?.toDate()?.time,
                resolvedAt = (data["resolvedAt"] as? Timestamp)?.toDate()?.time,
                responderId = data["responderId"] as? String,
                responderName = data["responderName"] as? String,
                responderDistance = (data["responderDistance"] as? Number)?.toFloat()
            )
        }
    }
}
