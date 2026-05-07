package com.example.resqnet.data.firebase

import com.example.resqnet.data.repository.AdminRepository
import com.example.resqnet.data.repository.AdminStats
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirebaseAdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AdminRepository {

    private val sosCol get() = firestore.collection("sos_requests")
    private val usersCol get() = firestore.collection("users")

    override suspend fun getAllSosRequests(): Result<List<SosRequest>> = runCatching {
        sosCol.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.data?.let { FirebaseSosRepository.docToSosStatic(doc.id, it) }
            }
    }

    override suspend fun getDashboardStats(): Result<AdminStats> = runCatching {
        val allDocs = sosCol.get().await().documents

        // Start of today
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayDocs = allDocs.filter { doc ->
            val ts = (doc["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0
            ts >= startOfDay
        }

        val resolvedToday = todayDocs.count { it.getString("status") == SosStatus.RESOLVED.name }
        val totalSosToday = todayDocs.size

        // Avg response time (in seconds) for accepted requests
        val acceptedDocs = allDocs.filter { it.getString("status") == SosStatus.ACCEPTED.name }
        val avgResponse = if (acceptedDocs.isNotEmpty()) {
            acceptedDocs.mapNotNull { doc ->
                val created = (doc["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: return@mapNotNull null
                val accepted = (doc["acceptedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: return@mapNotNull null
                ((accepted - created) / 1000).toInt()
            }.average().toInt()
        } else 0

        // Count active volunteers
        val activeVolunteers = usersCol
            .whereEqualTo("role", "VOLUNTEER")
            .whereEqualTo("isAvailable", true)
            .get().await().size()

        val totalUsers = usersCol.get().await().size()

        AdminStats(
            totalSosToday = totalSosToday,
            avgResponseTimeSec = avgResponse,
            activeVolunteers = activeVolunteers,
            flaggedRequests = 0,
            totalUsers = totalUsers,
            resolvedToday = resolvedToday
        )
    }

    override suspend fun banUser(userId: String): Result<Boolean> = runCatching {
        usersCol.document(userId).update("isBanned", true).await()
        true
    }

    override suspend fun dismissSos(sosId: String): Result<Boolean> = runCatching {
        sosCol.document(sosId).update("status", SosStatus.CANCELLED.name).await()
        true
    }
}
