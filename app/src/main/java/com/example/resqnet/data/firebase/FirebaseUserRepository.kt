package com.example.resqnet.data.firebase

import com.example.resqnet.data.repository.UserRepository
import com.example.resqnet.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val col get() = firestore.collection("users")

    override suspend fun getUserById(id: String): Result<User> = runCatching {
        val snap = col.document(id).get().await()
        val data = snap.data ?: error("User not found")
        FirebaseAuthRepository.snapshotToUser(id, data)
    }

    override suspend fun updateProfile(name: String, skills: List<String>?): Result<User> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val updates = mutableMapOf<String, Any?>("name" to name)
        if (skills != null) {
            updates["volunteerProfile.skills"] = skills
        }
        col.document(uid).update(updates).await()
        val snap = col.document(uid).get().await()
        FirebaseAuthRepository.snapshotToUser(uid, snap.data ?: emptyMap())
    }
}
