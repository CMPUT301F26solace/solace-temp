package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.validation.ProfileValidator

// =========================================================================
// PROFILE & ROLE SWITCHER SCREEN
// =========================================================================
/**
 * Composable screen for user profile management and application role switching.
 * 
 * @param repository The data repository for updating user profile and role settings.
 */
@Composable
fun ProfileAndRoleScreen(
    repository: AppRepository,
    onNavigateToPolicy: () -> Unit = {}
) {
    val profile by repository.currentProfile.collectAsState()
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var receiveNotifs by remember(profile.receiveNotifications) { mutableStateOf(profile.receiveNotifications) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Acceptance Criteria: Admin can navigate between views within the same profile
        if (profile.isAdmin) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Admin View Navigation", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Switch your active view to access Entrant or Owner features.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { roleOption ->
                            FilterChip(
                                selected = profile.role == roleOption,
                                onClick = { repository.switchRole(roleOption) },
                                label = { Text(roleOption.name) }
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null; saveFeedback = null },
                    label = { Text("Name") },
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name")
                )
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null; saveFeedback = null },
                    label = { Text("Email") },
                    isError = emailError != null,
                    modifier = Modifier.fillMaxWidth().testTag("profile_email")
                )
                emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = null; saveFeedback = null },
                    label = { Text("Phone (Optional)") },
                    isError = phoneError != null,
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone")
                )
                phoneError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receive Notifications")
                        Text(
                            text = "Turn off to stop alerts. Lottery invitations and reservations still appear in the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = receiveNotifs,
                        onCheckedChange = { receiveNotifs = it },
                        modifier = Modifier.testTag("profile_notifications_toggle")
                    )
                }

                Button(
                    onClick = {
                        if (!isSaving) {
                            val trimmedName = name.trim()
                            val trimmedEmail = email.trim()
                            val trimmedPhone = phone.trim()
                            nameError = ProfileValidator.nameError(trimmedName)
                            emailError = ProfileValidator.emailError(trimmedEmail)
                            phoneError = ProfileValidator.phoneError(trimmedPhone)
                            saveFeedback = null

                            if (nameError == null && emailError == null && phoneError == null) {
                                isSaving = true
                                repository.updateProfile(
                                    profile.copy(
                                        name = trimmedName,
                                        email = trimmedEmail,
                                        phone = trimmedPhone,
                                        receiveNotifications = receiveNotifs
                                    )
                                ) { result ->
                                    isSaving = false
                                    saveFeedback = if (result.isSuccess) {
                                        "Profile updated."
                                    } else {
                                        "Could not save your profile. Please try again."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier.fillMaxWidth().testTag("profile_save")
                ) {
                    Text(if (isSaving) "Saving..." else "Save Changes")
                }
                saveFeedback?.let { message ->
                    Text(
                        text = message,
                        color = if (message == "Profile updated.") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("profile_save_feedback")
                    )
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onNavigateToPolicy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View App Policy & Guidelines")
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "📱 Device ID: ${profile.deviceId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
