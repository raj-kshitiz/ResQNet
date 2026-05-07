package com.example.resqnet.ui.screens.volunteer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.components.StatusBadge
import com.example.resqnet.ui.theme.ResQNetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveResponseScreen(
    sosId: String,
    onDone: () -> Unit,
    volunteerViewModel: VolunteerViewModel = viewModel(factory = VolunteerViewModel.Factory)
) {
    val uiState by volunteerViewModel.uiState.collectAsState()

    LaunchedEffect(sosId) { volunteerViewModel.loadSosById(sosId) }

    val sos = uiState.selectedSos
    val status = sos?.status ?: SosStatus.ACCEPTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Active Response",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            // Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (status) {
                        SosStatus.ACCEPTED    -> "You accepted – tap On the Way"
                        SosStatus.IN_PROGRESS -> "Heading to requester"
                        SosStatus.RESOLVED    -> "Mission complete!"
                        else                  -> "Active response"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Map placeholder
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (status == SosStatus.RESOLVED)
                                Icons.Default.CheckCircle else Icons.Default.Navigation,
                            contentDescription = null,
                            tint = if (status == SosStatus.RESOLVED)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (status == SosStatus.RESOLVED) "Resolved" else "Navigation Map",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (status == SosStatus.RESOLVED)
                                "The requester has marked this resolved"
                            else "Live directions to requester",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Requester info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Requester",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sos?.requesterName ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = sos?.addressHint ?: "Unknown location",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { /* TODO: call requester */ }) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (sos?.description != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = sos.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            when (status) {
                SosStatus.ACCEPTED -> {
                    // Volunteer taps this to let the requester know help is on the way
                    Button(
                        onClick = { volunteerViewModel.setOnTheWay(sosId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            "🚗  I'm On the Way",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                SosStatus.IN_PROGRESS -> {
                    // Waiting for requester to mark resolved
                    Text(
                        text = "Waiting for the requester to mark this resolved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDone,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Go Back") }
                }

                SosStatus.RESOLVED -> {
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            "✓  Done",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    OutlinedButton(
                        onClick = onDone,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Go Back") }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun ActiveResponseScreenPreview() {
    ResQNetTheme {
        ActiveResponseScreen(sosId = "sos4", onDone = {})
    }
}
