package com.example.resqnet

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.resqnet.navigation.NavGraph
import com.example.resqnet.ui.theme.ResQNetTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val pendingSosId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingSosId.value = intent.getStringExtra("sos_id")
        saveFcmToken()
        requestNotificationPermission()
        setContent {
            ResQNetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController, initialSosId = pendingSosId.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSosId.value = intent.getStringExtra("sos_id")
    }

    private fun saveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            com.example.resqnet.service.FcmService.saveTokenToFirestore(token)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}
