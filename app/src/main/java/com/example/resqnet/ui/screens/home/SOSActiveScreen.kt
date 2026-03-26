package com.example.resqnet.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.components.StatusBadge
import com.example.resqnet.ui.components.VolunteerCard
import com.example.resqnet.ui.theme.ResQNetTheme

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
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Status badge
            StatusBadge(status = uiState.currentStatus)

            Spacer(modifier = Modifier.height(24.dp))

            // Status steps
            StatusStep(
                icon = Icons.Default.HourglassTop,
                label = "Request Sent",
                isActive = uiState.currentStatus.ordinal >= SosStatus.PENDING.ordinal,
                isCurrent = uiState.currentStatus == SosStatus.PENDING
            )
            StatusStep(
                icon = Icons.Default.NotificationsActive,
                label = "Notifying Volunteers",
                isActive = uiState.currentStatus.ordinal >= SosStatus.NOTIFIED.ordinal,
                isCurrent = uiState.currentStatus == SosStatus.NOTIFIED
            )
            StatusStep(
                icon = Icons.Default.PersonPin,
                label = "Volunteer Accepted",
                isActive = uiState.currentStatus.ordinal >= SosStatus.ACCEPTED.ordinal,
                isCurrent = uiState.currentStatus == SosStatus.ACCEPTED
            )
            StatusStep(
                icon = Icons.Default.Route,
                label = "Help On The Way",
                isActive = uiState.currentStatus.ordinal >= SosStatus.IN_PROGRESS.ordinal,
                isCurrent = uiState.currentStatus == SosStatus.IN_PROGRESS
            )
            StatusStep(
                icon = Icons.Default.CheckCircle,
                label = "Resolved",
                isActive = uiState.currentStatus == SosStatus.RESOLVED,
                isCurrent = uiState.currentStatus == SosStatus.RESOLVED
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
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

            if (uiState.currentStatus != SosStatus.RESOLVED &&
                uiState.currentStatus != SosStatus.CANCELLED
            ) {
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

            if (uiState.currentStatus == SosStatus.RESOLVED ||
                uiState.currentStatus == SosStatus.CANCELLED
            ) {
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
}

@Composable
private fun StatusStep(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
        ) {
            if (isCurrent) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) MaterialTheme.colorScheme.onTertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
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
