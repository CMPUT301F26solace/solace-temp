/**
 * CampsiteDialogs.kt
 * 
 * Purpose: Provides dialog composables for editing campsite details, managing image assets, and confirming administrative deletions.
 * 
 * Issues / Outstanding Issues:
 * - Image asset updates require internet connectivity when uploading to Firebase Storage.
 */
package com.example.solace_temp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.CampsiteRemovalDependencies

// =========================================================================
// EDIT CAMPSITE DIALOG
// =========================================================================
/**
 * Dialog allowing Campsite Owners or Admins to edit an existing campsite's details.
 * 
 * Includes form fields for name, location, description, price, occupancy, and cover image,
 * with identical validation checks as [CreateCampsiteScreen]. Upon successful validation and save/upload,
 * the dialog transitions to display a success status view.
 * 
 * @param campsite The [Campsite] entity being edited.
 * @param repository The data repository used to execute campsite updates and image uploads.
 * @param onDismiss Callback invoked when the dialog is dismissed or cancelled.
 * @param onSuccess Callback invoked when the user completes the campsite edit.
 */
@Composable
fun EditCampsiteDialog(
    campsite: Campsite,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember(campsite) { mutableStateOf(campsite.name) }
    var parkLocation by remember(campsite) { mutableStateOf(campsite.location.locationName) }
    var description by remember(campsite) { mutableStateOf(campsite.description) }
    var price by remember(campsite) {
        mutableStateOf(
            if (campsite.pricePerNight % 1.0 == 0.0) campsite.pricePerNight.toInt().toString()
            else campsite.pricePerNight.toString()
        )
    }
    var maxOccupancy by remember(campsite) { mutableStateOf(campsite.maxOccupancy.toString()) }
    var imageUrlInput by remember(campsite) { mutableStateOf(campsite.imageUrl ?: "") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            if (isSuccessState) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Campsite Updated", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Edit Campsite Details", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (isSuccessState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "All campsite information for \"${name.trim()}\" has been successfully edited and updated.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Changes have been persisted to Firebase storage and are now active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Modify your campsite details below. All required fields will be verified before saving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val textFieldShape = RoundedCornerShape(16.dp)
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Campsite Name (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = parkLocation,
                        onValueChange = { parkLocation = it },
                        label = { Text("Location (Required, e.g., Banff, Jasper)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Amenities (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price per Night ($) (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = maxOccupancy,
                        onValueChange = { maxOccupancy = it },
                        label = { Text("Maximum Occupancy (Required)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        enabled = !isSaving
                    )

                    // Image selection / upload section
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Campsite Cover Image",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (selectedImageUri != null || imageUrlInput.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri ?: imageUrlInput,
                                        contentDescription = "Selected Campsite Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = !isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (selectedImageUri != null) "Change Selected Image" else "Choose Image from Device")
                            }

                            OutlinedTextField(
                                value = imageUrlInput,
                                onValueChange = {
                                    imageUrlInput = it
                                    selectedImageUri = null
                                },
                                label = { Text("Or Image URL (Optional, e.g., https://...)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = textFieldShape,
                                colors = textFieldColors,
                                enabled = !isSaving
                            )
                        }
                    }

                    if (feedbackMsg.isNotBlank()) {
                        Text(
                            text = feedbackMsg,
                            color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isSaving) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Saving updates to Firebase...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccessState) {
                Button(
                    onClick = {
                        onSuccess()
                    }
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        val parsedPrice = price.toDoubleOrNull()
                        val parsedOccupancy = maxOccupancy.toIntOrNull()

                        when {
                            name.isBlank() -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Campsite name cannot be empty."
                            }
                            parkLocation.isBlank() -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Location cannot be empty."
                            }
                            description.isBlank() -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Description cannot be empty."
                            }
                            parsedPrice == null || parsedPrice < 0 -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Please enter a valid fee/price per night."
                            }
                            parsedOccupancy == null || parsedOccupancy <= 0 -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Please enter a valid maximum occupancy."
                            }
                            else -> {
                                feedbackIsError = false
                                isSaving = true
                                feedbackMsg = ""

                                val currentUri = selectedImageUri
                                if (currentUri != null) {
                                    repository.uploadCampsiteImage(campsite.id, currentUri) { uploadResult ->
                                        uploadResult.fold(
                                            onSuccess = { downloadUrl ->
                                                val updatedSite = campsite.copy(
                                                    name = name.trim(),
                                                    location = campsite.location.copy(locationName = parkLocation.trim()),
                                                    description = description.trim(),
                                                    pricePerNight = parsedPrice,
                                                    maxOccupancy = parsedOccupancy,
                                                    imageUrl = downloadUrl
                                                )
                                                repository.updateCampsite(updatedSite) { updateResult ->
                                                    isSaving = false
                                                    updateResult.fold(
                                                        onSuccess = {
                                                            isSuccessState = true
                                                        },
                                                        onFailure = { err ->
                                                            feedbackIsError = true
                                                            feedbackMsg = "Error: ${err.message ?: "Failed to update campsite."}"
                                                        }
                                                    )
                                                }
                                            },
                                            onFailure = { err ->
                                                isSaving = false
                                                feedbackIsError = true
                                                feedbackMsg = "Error uploading image to Firebase Storage: ${err.message}"
                                            }
                                        )
                                    }
                                } else {
                                    val updatedSite = campsite.copy(
                                        name = name.trim(),
                                        location = campsite.location.copy(locationName = parkLocation.trim()),
                                        description = description.trim(),
                                        pricePerNight = parsedPrice,
                                        maxOccupancy = parsedOccupancy,
                                        imageUrl = imageUrlInput.trim().ifBlank { null }
                                    )
                                    repository.updateCampsite(updatedSite) { updateResult ->
                                        isSaving = false
                                        updateResult.fold(
                                            onSuccess = {
                                                isSuccessState = true
                                            },
                                            onFailure = { err ->
                                                feedbackIsError = true
                                                feedbackMsg = "Error: ${err.message ?: "Failed to update campsite."}"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            if (!isSuccessState) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

/**
 * Confirmation and dependency-override dialog for administrative campsite removal (US 03.01.01).
 *
 * Details the standard consequences of deletion (listing removal, QR invalidation, cloud media
 * cleanup, waitlist cleanup, and audit logging). When active reservations or open-window lotteries
 * exist, blocks deletion until the administrator explicitly acknowledges the override checkbox.
 *
 * @param campsite The [Campsite] targeted for removal.
 * @param dependencies Pre-fetched [CampsiteRemovalDependencies] snapshot for this campsite.
 * @param isRemoving True while the asynchronous repository removal request is in flight.
 * @param onDismiss Callback invoked when the administrator cancels or dismisses the dialog.
 * @param onConfirmRemoval Callback invoked when removal is confirmed, passing whether override was checked.
 */
@Composable
fun AdminRemoveCampsiteDialog(
    campsite: Campsite,
    dependencies: CampsiteRemovalDependencies,
    isRemoving: Boolean,
    onDismiss: () -> Unit,
    onConfirmRemoval: (forceOverride: Boolean) -> Unit
) {
    // Reset explicit override acknowledgment whenever a different campsite is targeted
    var overrideConfirmed by remember(campsite.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isRemoving) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Removal Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (dependencies.requiresOverrideWarning) "Override & Remove Campsite?"
                else "Remove Campsite?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You are about to permanently remove \"${campsite.name}\" from the platform."
                )

                // Detail consequences before the admin proceeds
                Text(
                    text = buildString {
                        append("This action will:\n")
                        append("• Immediately remove the listing from public browsing, search results, and owner listings.\n")
                        if (dependencies.hasQrCode) {
                            append("• Invalidate its promotional QR code.\n")
                        }
                        if (dependencies.hasMediaAsset) {
                            append("• Clean up associated media assets from cloud storage.\n")
                        }
                        if (dependencies.affectedLotteryIds.isNotEmpty()) {
                            append("• Clean up ${dependencies.affectedLotteryIds.size} linked lottery(s) and ${dependencies.totalWaitlistedEntrants} unfulfilled waitlist entry(s).\n")
                        }
                        append("• Record your admin ID, campsite ID, and timestamp in the audit log.")
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                // Explicit override warning if active reservations or open registration windows exist
                if (dependencies.requiresOverrideWarning) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Active Dependencies Detected",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (dependencies.activeReservationIds.isNotEmpty()) {
                                Text(
                                    text = "• ${dependencies.activeReservationIds.size} active reservation(s) will be cancelled.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (dependencies.openLotteryIds.isNotEmpty()) {
                                Text(
                                    text = "• ${dependencies.openLotteryIds.size} lottery(s) with an open registration window will be closed/updated.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Gate override confirmation behind an explicit checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isRemoving) {
                                overrideConfirmed = !overrideConfirmed
                            }
                    ) {
                        Checkbox(
                            checked = overrideConfirmed,
                            onCheckedChange = { overrideConfirmed = it },
                            enabled = !isRemoving
                        )
                        Text(
                            text = "I understand and confirm overriding active reservations and open lotteries.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmRemoval(dependencies.requiresOverrideWarning && overrideConfirmed)
                },
                enabled = !isRemoving && (!dependencies.requiresOverrideWarning || overrideConfirmed),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = when {
                        isRemoving -> "Removing..."
                        dependencies.requiresOverrideWarning -> "Override & Remove"
                        else -> "Remove Campsite"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRemoving
            ) {
                Text("Cancel")
            }
        }
    )
}
