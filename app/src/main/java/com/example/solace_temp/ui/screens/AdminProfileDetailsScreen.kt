/**
 * AdminProfileDetailsScreen.kt
 *
 * Purpose: Provides a detailed administrative profile moderation view for inspecting
 * user identity, active reservations, owned campsites, lottery entries, and executing
 * profile removals / owner policy enforcement bans with modal confirmation and dependency cleanup.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.AccountStatus
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileDetailsScreen(
    deviceId: String,
    repository: AppRepository,
    onBack: () -> Unit = {}
) {
    val profilesList by repository.profiles.collectAsState(initial = emptyList())
    val currentProfile by repository.currentProfile.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()

    val targetUser = profilesList.find { it.deviceId == deviceId }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var removalReason by remember { mutableStateOf("") }
    var removalFeedback by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isAuthorizedAdmin = currentProfile.isAdmin || currentProfile.role == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Moderation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!isAuthorizedAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
                    .testTag("admin_profile_access_denied"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Access Denied",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Access Denied: Administrator Credentials Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            return@Scaffold
        }

        if (targetUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = removalFeedback ?: "Profile not found or was removed.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onBack) { Text("Back to Directory") }
                }
            }
            return@Scaffold
        }

        val userReservations = reservations.filter { it.entrantId == targetUser.deviceId && it.status == ReservationStatus.ACTIVE }
        val userWaitlists = lotteries.filter { targetUser.deviceId in it.waitingListEntrantIds }
        val ownedCampsites = campsites.filter { it.ownerId == targetUser.deviceId }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Confirmation Feedback Banner
            removalFeedback?.let { feedback ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("profile_removal_feedback")
                ) {
                    Text(
                        text = feedback,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // User Identity & Status Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetUser.name.ifBlank { "Unknown User" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ID: ${targetUser.deviceId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        val statusColor = when (targetUser.accountStatus) {
                            AccountStatus.ACTIVE -> Color(0xFF2E7D32)
                            AccountStatus.INACTIVE -> Color(0xFF757575)
                            AccountStatus.SUSPENDED -> MaterialTheme.colorScheme.error
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = targetUser.accountStatus.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("Email: ${targetUser.email.ifBlank { "Not provided" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Phone: ${targetUser.phone.ifBlank { "Not provided" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Role: ${targetUser.role.name}", style = MaterialTheme.typography.bodyMedium)
                    Text("Registered: ${formatDateMillis(targetUser.registrationDateMillis)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // Active Relationships Overview
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Active Relationships & Dependencies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("• Active Reservations: ${userReservations.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Lottery Waitlist Entries: ${userWaitlists.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Owned Campsites: ${ownedCampsites.size}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Destructive Action: Remove Profile / Remove Owner Button
            val isOwner = targetUser.role == UserRole.CAMPSITE_OWNER
            Button(
                onClick = {
                    removalReason = ""
                    validationError = null
                    showDeleteConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isOwner) "remove_owner_button_${targetUser.deviceId}" else "remove_profile_button_${targetUser.deviceId}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Profile")
                Spacer(Modifier.width(8.dp))
                Text(if (isOwner) "Remove Owner & Enforce Policy" else "Remove Profile from Platform")
            }
        }
    }

    // Modal Confirmation Dialog Requiring Explicit Confirmation
    if (showDeleteConfirmDialog && targetUser != null) {
        val isOwnerAccount = targetUser.role == UserRole.CAMPSITE_OWNER

        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                validationError = null
            },
            title = {
                Text(
                    if (isOwnerAccount) "Remove Owner & Enforce Policy" else "Confirm Profile Removal",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isOwnerAccount)
                            "You are initiating the removal and ban of campsite owner '${targetUser.name.ifBlank { targetUser.deviceId }}' (ID: ${targetUser.deviceId}) for policy violation."
                        else
                            "Are you sure you want to permanently remove user profile '${targetUser.name.ifBlank { targetUser.deviceId }}' (ID: ${targetUser.deviceId})?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "System cleanup will execute the following actions:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isOwnerAccount) {
                        Text("• Associated campsites will be delisted from public search & browsing.", style = MaterialTheme.typography.bodySmall)
                        Text("• Ongoing and upcoming lotteries will be cancelled/flagged.", style = MaterialTheme.typography.bodySmall)
                        Text("• Active reservations will be cancelled & affected entrants notified.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("• Active reservations & invitations will be cancelled.", style = MaterialTheme.typography.bodySmall)
                        Text("• Entrant lottery waitlist entries will be purged.", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("• Profile media assets (avatar images) will be unlinked.", style = MaterialTheme.typography.bodySmall)
                    Text("• Account credentials will be revoked (SUSPENDED) preventing logins.", style = MaterialTheme.typography.bodySmall)
                    Text("• Administrative action will be recorded in audit logs.", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = removalReason,
                        onValueChange = {
                            removalReason = it
                            validationError = null
                        },
                        label = { Text(if (isOwnerAccount) "Stated Policy Violation Reason (Required)" else "Reason for removal (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("removal_reason_input"),
                        singleLine = true
                    )

                    validationError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("owner_violation_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isOwnerAccount && removalReason.isBlank()) {
                            validationError = "A policy violation reason is required to remove an owner account."
                            return@Button
                        }

                        showDeleteConfirmDialog = false
                        validationError = null

                        if (isOwnerAccount) {
                            repository.removeOwnerForPolicyViolation(
                                adminId = currentProfile.deviceId,
                                adminName = currentProfile.name,
                                targetOwnerId = targetUser.deviceId,
                                policyViolationReason = removalReason
                            ) { result ->
                                result.fold(
                                    onSuccess = { notifiedCount ->
                                        removalFeedback = "Campsite owner '${targetUser.name.ifBlank { targetUser.deviceId }}' was successfully removed for policy violation. $notifiedCount affected entrant(s) notified."
                                    },
                                    onFailure = { err ->
                                        removalFeedback = "Error removing owner: ${err.localizedMessage}"
                                    }
                                )
                            }
                        } else {
                            repository.removeProfileAsAdmin(
                                adminId = currentProfile.deviceId,
                                adminName = currentProfile.name,
                                targetUserId = targetUser.deviceId,
                                reason = removalReason
                            ) { result ->
                                result.fold(
                                    onSuccess = {
                                        removalFeedback = "Profile '${targetUser.name.ifBlank { targetUser.deviceId }}' was successfully removed."
                                    },
                                    onFailure = { err ->
                                        removalFeedback = "Error removing profile: ${err.localizedMessage}"
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_remove_profile")
                ) {
                    Text(if (isOwnerAccount) "Enforce Ban & Notify Entrants" else "Remove Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    validationError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
