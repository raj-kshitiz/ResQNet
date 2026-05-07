package com.example.resqnet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.resqnet.service.FcmService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ResQNetApplication : Application() {

    private var alertListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Starts/stops the Firestore alert listener whenever auth state changes.
        // Because this lives on the Application, it persists for the entire process lifetime —
        // notifications arrive whether the user is on any screen or the app is in background.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) startAlertListener(uid)
            else {
                alertListener?.remove()
                alertListener = null
            }
        }
    }

    private fun startAlertListener(uid: String) {
        alertListener?.remove()
        alertListener = FirebaseFirestore.getInstance()
            .collection("notifications").document(uid)
            .collection("alerts")
            .whereEqualTo("seen", false)
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        showLocalNotification(change.document.data ?: return@forEach)
                        change.document.reference.update("seen", true)
                    }
            }
    }

    private fun showLocalNotification(data: Map<String, Any?>) {
        val emergencyType = (data["emergencyType"] as? String ?: "EMERGENCY")
            .replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        val requesterName = data["requesterName"] as? String ?: "Someone"
        val hint = data["addressHint"] as? String
        val body = if (hint != null) "$requesterName needs help near $hint"
                   else "$requesterName needs help nearby"
        val sosId = data["sosId"] as? String

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (sosId != null) putExtra("sos_id", sosId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, sosId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FcmService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 $emergencyType Alert")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) { }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FcmService.CHANNEL_ID, "SOS Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS alerts from nearby community members"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
