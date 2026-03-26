package com.example.resqnet.domain.model

data class Feedback(
    val id: String,
    val sosRequestId: String,
    val fromUserId: String,
    val toUserId: String,
    val rating: Int, // 1–5
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
