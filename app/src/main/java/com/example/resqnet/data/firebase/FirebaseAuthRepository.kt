package com.example.resqnet.data.firebase

import android.content.Context
import com.example.resqnet.data.repository.AuthRepository
import com.example.resqnet.domain.model.User
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.domain.model.VolunteerProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "resqnet_auth_prefs"
private const val KEY_LAST_AUTH_MS = "last_auth_timestamp_ms"
private const val RE_AUTH_INTERVAL_DAYS = 7L

class FirebaseAuthRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ── Public API ───────────────────────────────────────────────────────────

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        dobMillis: Long
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: error("Registration failed — no user returned")

        // Persist basic profile to Firestore
        val userData = mapOf(
            "name"        to fullName,
            "email"       to email,
            "dob"         to dobMillis,
            "role"        to "REQUESTER",
            "isVerified"  to true,
            "isAvailable" to true
        )
        firestore.collection("users").document(firebaseUser.uid).set(userData).await()

        stampAuthTime()
        User(
            id          = firebaseUser.uid,
            phone       = "",
            name        = fullName,
            role        = UserRole.REQUESTER,
            isVerified  = true,
            isAvailable = true
        )
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: error("Login failed — no user returned")
        stampAuthTime()
        fetchUserDocument(firebaseUser.uid)
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id    = firebaseUser.uid,
            phone = "",
            name  = firebaseUser.displayName ?: "User",
            role  = UserRole.REQUESTER
        )
    }

    override fun needsReAuth(): Boolean {
        if (auth.currentUser == null) return true
        val lastAuth = prefs.getLong(KEY_LAST_AUTH_MS, 0L)
        if (lastAuth == 0L) return true
        val elapsed = System.currentTimeMillis() - lastAuth
        return elapsed > TimeUnit.DAYS.toMillis(RE_AUTH_INTERVAL_DAYS)
    }

    override fun logout() {
        auth.signOut()
        prefs.edit().remove(KEY_LAST_AUTH_MS).apply()
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun stampAuthTime() {
        prefs.edit().putLong(KEY_LAST_AUTH_MS, System.currentTimeMillis()).apply()
    }

    private suspend fun fetchUserDocument(uid: String): User {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val data = snapshot.data ?: emptyMap()
        return snapshotToUser(uid, data)
    }

    companion object {
        fun snapshotToUser(uid: String, data: Map<String, Any?>): User {
            val roleStr = data["role"] as? String ?: "REQUESTER"
            val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.REQUESTER)

            val vpData = data["volunteerProfile"] as? Map<*, *>
            val volunteerProfile = vpData?.let {
                @Suppress("UNCHECKED_CAST")
                VolunteerProfile(
                    skills              = (it["skills"] as? List<String>) ?: emptyList(),
                    reliabilityScore    = (it["reliabilityScore"] as? Number)?.toFloat() ?: 50f,
                    totalResponses      = (it["totalResponses"] as? Number)?.toInt() ?: 0,
                    successfulResponses = (it["successfulResponses"] as? Number)?.toInt() ?: 0,
                    avgResponseTimeSec  = (it["avgResponseTimeSec"] as? Number)?.toInt()
                )
            }

            return User(
                id              = uid,
                phone           = data["phone"] as? String ?: "",
                name            = data["name"] as? String ?: "User",
                role            = role,
                isVerified      = data["isVerified"] as? Boolean ?: false,
                isAvailable     = data["isAvailable"] as? Boolean ?: true,
                volunteerProfile = volunteerProfile
            )
        }
    }
}
