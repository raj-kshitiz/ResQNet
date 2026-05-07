package com.example.resqnet.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.components.StatusBadge
import com.example.resqnet.ui.components.VolunteerCard
import com.example.resqnet.ui.theme.ResQNetTheme

private data class EmergencyContact(val name: String, val number: String)

private fun emergencyContactsFor(type: EmergencyType): List<EmergencyContact> = when (type) {
    EmergencyType.MEDICAL -> listOf(
        EmergencyContact("Ambulance (EMRI)", "108"),
        EmergencyContact("National Emergency", "112"),
        EmergencyContact("Health Helpline", "104")
    )
    EmergencyType.ACCIDENT -> listOf(
        EmergencyContact("Ambulance (EMRI)", "108"),
        EmergencyContact("Police", "100"),
        EmergencyContact("National Emergency", "112")
    )
    EmergencyType.BLOOD_REQUEST -> listOf(
        EmergencyContact("Health Helpline", "104"),
        EmergencyContact("Blood Bank (NBTC)", "1910"),
        EmergencyContact("National Emergency", "112")
    )
    EmergencyType.SAFETY_ALERT -> listOf(
        EmergencyContact("Police", "100"),
        EmergencyContact("Women Helpline", "1091"),
        EmergencyContact("National Emergency", "112")
    )
    EmergencyType.OTHER -> listOf(
        EmergencyContact("National Emergency", "112"),
        EmergencyContact("Police", "100"),
        EmergencyContact("Ambulance (EMRI)", "108")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSActiveScreen(
    sosId: String,
    onDone: () -> Unit,
    sosViewModel: SosViewModel = viewModel()
) {
    val uiState by sosViewModel.uiState.collectAsState()

    LaunchedEffect(sosId) {
        sosViewModel.loadActiveSos(sosId)
    }

    val isActive = uiState.currentStatus != SosStatus.RESOLVED &&
            uiState.currentStatus != SosStatus.CANCELLED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SOS Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                if (uiState.currentStatus == SosStatus.IN_PROGRESS) {
                    Button(
                        onClick = { sosViewModel.resolveSos(sosId, onDone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "✓  Mark Resolved",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (isActive) {
                    OutlinedButton(
                        onClick = { sosViewModel.cancelSos(sosId, onDone) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel SOS")
                    }
                }
                if (!isActive) {
                    Button(
                        onClick = onDone,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Back to Home",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Colored status banner
            val cs = MaterialTheme.colorScheme
            val (bannerBg, bannerText) = when (uiState.currentStatus) {
                SosStatus.PENDING, SosStatus.NOTIFIED -> cs.errorContainer to cs.onErrorContainer
                SosStatus.ACCEPTED, SosStatus.IN_PROGRESS -> cs.secondaryContainer to cs.onSecondaryContainer
                SosStatus.RESOLVED -> cs.tertiaryContainer to cs.onTertiaryContainer
                else -> cs.surfaceVariant to cs.onSurfaceVariant
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(bannerBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val radiusKm = uiState.activeSos?.radiusKm ?: 3f
                    Text(
                        text = when (uiState.currentStatus) {
                            SosStatus.PENDING     -> "Sending your request..."
                            SosStatus.NOTIFIED    -> "Searching within ${radiusKm.toInt()} km"
                            SosStatus.ACCEPTED    -> "A volunteer has accepted"
                            SosStatus.IN_PROGRESS -> "Help is on the way"
                            SosStatus.RESOLVED    -> "Situation resolved"
                            SosStatus.CANCELLED   -> "Request cancelled"
                            else                  -> "Processing..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bannerText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = uiState.currentStatus)
                }
            }

            // Timeline steps
            val s = uiState.currentStatus
            StatusStep(
                icon = Icons.Default.HourglassTop,
                label = "Request Sent",
                isActive = true,
                isCurrent = false
            )
            StatusStep(
                icon = Icons.Default.NotificationsActive,
                label = "Notifying Volunteers",
                isActive = s.ordinal >= SosStatus.NOTIFIED.ordinal,
                isCurrent = s == SosStatus.PENDING
            )
            StatusStep(
                icon = Icons.Default.PersonPin,
                label = "Volunteer Accepted",
                isActive = s.ordinal >= SosStatus.ACCEPTED.ordinal,
                isCurrent = s == SosStatus.NOTIFIED
            )
            StatusStep(
                icon = Icons.Default.Route,
                label = "Help On The Way",
                isActive = s.ordinal >= SosStatus.IN_PROGRESS.ordinal,
                isCurrent = s == SosStatus.ACCEPTED
            )
            StatusStep(
                icon = Icons.Default.CheckCircle,
                label = "Resolved",
                isActive = s == SosStatus.RESOLVED,
                isCurrent = s == SosStatus.IN_PROGRESS,
                isLast = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Emergency contacts — shown while waiting for help
            if (isActive) {
                uiState.activeSos?.emergencyType?.let { type ->
                    EmergencyContactsCard(type = type)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Volunteer info (appears after acceptance)
            AnimatedVisibility(
                visible = uiState.currentStatus.ordinal >= SosStatus.ACCEPTED.ordinal
                        && uiState.activeSos?.responderName != null,
                enter = fadeIn()
            ) {
                VolunteerCard(
                    name = uiState.activeSos?.responderName ?: "",
                    distanceKm = uiState.activeSos?.responderDistance ?: 0f,
                    reliabilityScore = 92f,
                    skills = listOf("Blood Donor (O+)", "First Aid")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmergencyContactsCard(
    type: EmergencyType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts = emergencyContactsFor(type)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Have you called for help?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Official emergency numbers • India",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            contacts.forEachIndexed { index, contact ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.number}"))
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call ${contact.name}",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Call",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (index < contacts.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusStep(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isCurrent: Boolean,
    isLast: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> cs.primary
                            isActive  -> cs.tertiary
                            else      -> cs.surfaceVariant
                        }
                    )
            ) {
                if (isCurrent) {
                    CircularProgressIndicator(
                        color = cs.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isActive) cs.onTertiary else cs.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(
                            if (isActive) cs.tertiary.copy(alpha = 0.5f)
                            else cs.outlineVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) cs.onSurface else cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = if (!isLast) 20.dp else 0.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun SOSActiveScreenPreview() {
    ResQNetTheme {
        SOSActiveScreen(sosId = "sos1", onDone = {})
    }
}
