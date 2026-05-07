package com.example.resqnet.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resqnet.domain.model.UserRole
import com.example.resqnet.ui.components.StatCard
import com.example.resqnet.ui.theme.Amber600
import com.example.resqnet.ui.theme.ResQNetTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLoggedOut: (() -> Unit)? = null,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(LocalContext.current)
    )
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val user = uiState.user
    val profile = user?.volunteerProfile

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { profileViewModel.startEditing() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->

        // ── Edit Name Dialog ────────────────────────────────────────────────
        if (uiState.isEditing) {
            AlertDialog(
                onDismissRequest = { profileViewModel.cancelEditing() },
                title = { Text("Edit Name") },
                text = {
                    OutlinedTextField(
                        value = uiState.editName,
                        onValueChange = { profileViewModel.onEditNameChanged(it) },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { profileViewModel.saveProfile {} }) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { profileViewModel.cancelEditing() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Volunteer Registration Dialog ───────────────────────────────────
        if (uiState.isRegisteringVolunteer) {
            AlertDialog(
                onDismissRequest = { profileViewModel.cancelVolunteerRegistration() },
                title = { Text("Register as Volunteer") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.registerPhone,
                            onValueChange = { profileViewModel.onRegisterPhoneChanged(it) },
                            label = { Text("Phone Number") },
                            placeholder = { Text("10-digit mobile number") },
                            singleLine = true,
                            prefix = { Text("+91 ") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Select your skills",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            VOLUNTEER_SKILLS.forEach { skill ->
                                val selected = skill in uiState.registerSelectedSkills
                                FilterChip(
                                    selected = selected,
                                    onClick = { profileViewModel.toggleSkill(skill) },
                                    label = { Text(skill) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Amber600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Starting reliability score: 50%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (uiState.registerError != null) {
                            Text(
                                text = uiState.registerError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { profileViewModel.submitVolunteerRegistration {} },
                        enabled = !uiState.isVolunteerRegistering
                    ) {
                        if (uiState.isVolunteerRegistering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Register")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { profileViewModel.cancelVolunteerRegistration() },
                        enabled = !uiState.isVolunteerRegistering
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {

            // ── Loading / Error states ──────────────────────────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (uiState.error != null && user == null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { profileViewModel.loadProfile() }) { Text("Retry") }
                return@Column
            }

            // ── Avatar ──────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Name ────────────────────────────────────────────────────────
            Text(
                text = user?.name ?: "–",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Role + verified badge ───────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (user?.isVerified == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Phone ───────────────────────────────────────────────────────
            val phone = user?.phone
            if (!phone.isNullOrBlank()) {
                Text(
                    text = "+91 ●●●●●●${phone.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Volunteer stats ─────────────────────────────────────────────
            if (profile != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Amber600,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reliability Score",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${profile.reliabilityScore.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(profile.reliabilityScore)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { profile.reliabilityScore / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = scoreColor(profile.reliabilityScore),
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scoreLabel(profile.reliabilityScore),
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreColor(profile.reliabilityScore)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(title = "Total", value = "${profile.totalResponses}", subtitle = "Responses", modifier = Modifier.weight(1f))
                    StatCard(title = "Success", value = "${profile.successfulResponses}", subtitle = "Completed", modifier = Modifier.weight(1f))
                    StatCard(title = "Avg Time", value = "${(profile.avgResponseTimeSec ?: 0) / 60}m", subtitle = "Response", modifier = Modifier.weight(1f))
                }

                if (profile.skills.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Skills",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        profile.skills.forEach { skill ->
                            AssistChip(onClick = {}, label = { Text(skill) })
                        }
                    }
                }
            }

            // ── Register as volunteer (requester only) ──────────────────────
            if (user?.role == UserRole.REQUESTER) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { profileViewModel.startVolunteerRegistration() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Register as Volunteer")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Logout ──────────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    profileViewModel.logout {
                        onLoggedOut?.invoke() ?: onBack()
                    }
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
        }
    }
}

@Composable
private fun scoreColor(score: Float) = when {
    score >= 70f -> MaterialTheme.colorScheme.tertiary
    score >= 40f -> Amber600
    else         -> MaterialTheme.colorScheme.error
}

private fun scoreLabel(score: Float) = when {
    score >= 80f -> "Excellent"
    score >= 60f -> "Good"
    score >= 40f -> "Average"
    else         -> "Needs improvement"
}

@Preview(showBackground = true, widthDp = 380, heightDp = 780)
@Composable
private fun ProfileScreenPreview() {
    ResQNetTheme {
        ProfileScreen(onBack = {})
    }
}
