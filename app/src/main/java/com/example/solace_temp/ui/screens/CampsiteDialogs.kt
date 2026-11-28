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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.CampsiteRemovalDependencies
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.ErrorRed
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral300
import com.example.solace_temp.ui.theme.Neutral50
import com.example.solace_temp.ui.theme.Neutral500
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.SolaceDivider
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceRedSoft
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusActiveBg
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceTactileScale

// =========================================================================
// EDIT CAMPSITE DIALOG
// =========================================================================
/**
 * Modal sheet/card allowing Campsite Owners or Admins to edit an existing campsite's details.
 *
 * Mirrors the owner design system and [EditLotteryDialog] modal layout with grouped form sections,
 * cover image selection/preview, GPS coordinates, pricing & occupancy, and rules & policy.
 * Upon successful validation and save/upload, transitions to display a success status view.
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
    var latitudeInput by remember(campsite) { mutableStateOf(campsite.location.latitude.toString()) }
    var longitudeInput by remember(campsite) { mutableStateOf(campsite.location.longitude.toString()) }
    var description by remember(campsite) { mutableStateOf(campsite.description) }
    var price by remember(campsite) {
        mutableStateOf(
            if (campsite.pricePerNight % 1.0 == 0.0) campsite.pricePerNight.toInt().toString()
            else campsite.pricePerNight.toString()
        )
    }
    var maxOccupancy by remember(campsite) { mutableStateOf(campsite.maxOccupancy.toString()) }
    var rulesAndPolicyInput by remember(campsite) { mutableStateOf(campsite.rulesAndPolicy) }
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

    val fieldShape = RoundedCornerShape(SolaceTokens.RadiusMD)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = CardSurface,
        unfocusedContainerColor = CardSurface,
        focusedBorderColor = SolaceRed,
        unfocusedBorderColor = HairlineBorder,
        focusedLabelColor = SolaceRed,
        unfocusedLabelColor = Neutral600,
        focusedTextColor = Neutral900,
        unfocusedTextColor = Neutral900
    )
    val tnumTextStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    val ctaInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SolaceTokens.SpaceMD, vertical = SolaceTokens.SpaceLG)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(SolaceTokens.RadiusSheet),
            color = CardSurface,
            border = solaceCardBorder(),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sheet handle + header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SolaceTokens.SpaceXL,
                            end = SolaceTokens.SpaceMD,
                            top = SolaceTokens.SpaceMD,
                            bottom = SolaceTokens.SpaceSM
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                            .background(Neutral300)
                    )
                    Spacer(Modifier.height(SolaceTokens.SpaceMD))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSuccessState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = StatusActiveGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    "Campsite Updated",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "CAMPSITE SETTINGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SolaceRed
                                )
                                Text(
                                    "Edit Campsite Details",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }
                        }
                        if (!isSuccessState) {
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isSaving
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Neutral600
                                )
                            }
                        }
                    }
                }

                SolaceDivider()

                if (isSuccessState) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SolaceTokens.SpaceXXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                    ) {
                        Text(
                            "All campsite information for \"${name.trim()}\" has been successfully edited and updated.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Neutral900,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Changes have been persisted to Firebase storage and are now active.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral600,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(SolaceTokens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
                    ) {
                        Text(
                            "Modify your campsite details below. All required fields will be verified before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral600
                        )

                        // Section 1: Property Details
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Text(
                                    "PROPERTY DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral500
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Campsite Name (Required)") },
                                    singleLine = true,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description & Amenities (Required)") },
                                    minLines = 3,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Section 2: Location & GPS Coordinates
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = SolaceRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "LOCATION & GPS COORDINATES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Neutral500
                                    )
                                }
                                OutlinedTextField(
                                    value = parkLocation,
                                    onValueChange = { parkLocation = it },
                                    label = { Text("Location (Required, e.g., Banff, Jasper)") },
                                    singleLine = true,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                                ) {
                                    OutlinedTextField(
                                        value = latitudeInput,
                                        onValueChange = { latitudeInput = it },
                                        label = { Text("Latitude") },
                                        singleLine = true,
                                        textStyle = tnumTextStyle,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        enabled = !isSaving,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = longitudeInput,
                                        onValueChange = { longitudeInput = it },
                                        label = { Text("Longitude") },
                                        singleLine = true,
                                        textStyle = tnumTextStyle,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        enabled = !isSaving,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val previewLat = latitudeInput.trim().toDoubleOrNull() ?: campsite.location.latitude
                                val previewLng = longitudeInput.trim().toDoubleOrNull() ?: campsite.location.longitude
                                Surface(
                                    shape = SolaceInnerCardShape,
                                    color = CardSurface,
                                    border = solaceCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TabularText(
                                        text = "GPS Pin: $previewLat, $previewLng",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Neutral600,
                                        modifier = Modifier.padding(
                                            horizontal = SolaceTokens.SpaceMD,
                                            vertical = SolaceTokens.SpaceSM
                                        )
                                    )
                                }
                            }
                        }

                        // Section 3: Pricing & Max Guest Capacity
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Text(
                                    "PRICING & MAX GUEST CAPACITY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral500
                                )
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { price = it },
                                    label = { Text("Price per Night ($) (Required)") },
                                    singleLine = true,
                                    textStyle = tnumTextStyle,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = maxOccupancy,
                                    onValueChange = { maxOccupancy = it },
                                    label = { Text("Maximum Occupancy (Required)") },
                                    singleLine = true,
                                    textStyle = tnumTextStyle,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val liveRate = price.toDoubleOrNull()?.let { "$${it.toInt()}/night" } ?: "--"
                                val liveGuests = maxOccupancy.toIntOrNull()?.let { "$it guests" } ?: "--"
                                Surface(
                                    shape = SolaceInnerCardShape,
                                    color = SolaceRedSoft,
                                    border = BorderStroke(
                                        SolaceTokens.HairlineWidth,
                                        SolaceRed.copy(alpha = 0.20f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = SolaceTokens.SpaceMD,
                                                vertical = SolaceTokens.SpaceSM
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TabularText(
                                            text = "Nightly Rate: $liveRate",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = SolaceRed
                                        )
                                        TabularText(
                                            text = "Capacity: $liveGuests",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Neutral900
                                        )
                                    }
                                }
                            }
                        }

                        // Section 4: Rules & Cancellation Policy
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Text(
                                    "RULES & CANCELLATION POLICY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral500
                                )
                                OutlinedTextField(
                                    value = rulesAndPolicyInput,
                                    onValueChange = { rulesAndPolicyInput = it },
                                    label = { Text("Rules & Cancellation Policy (Optional)") },
                                    minLines = 2,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Section 5: Campsite Cover Image
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Text(
                                    "CAMPSITE COVER IMAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral500
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(156.dp)
                                        .clip(SolaceInnerCardShape)
                                        .background(CardSurface)
                                        .border(
                                            SolaceTokens.HairlineWidth,
                                            HairlineBorder,
                                            SolaceInnerCardShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val trimmedUrl = imageUrlInput.trim()
                                    if (selectedImageUri != null || trimmedUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = selectedImageUri ?: trimmedUrl,
                                            contentDescription = "Selected Campsite Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        TopographicImageFallback(
                                            modifier = Modifier.fillMaxSize(),
                                            showLabel = true,
                                            label = "Campsite Image Preview",
                                            subtitle = "Upload a photo or enter a URL below"
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                                    border = solaceCardBorder(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CardSurface,
                                        contentColor = Neutral900
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = SolaceRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                    Text(
                                        text = if (selectedImageUri != null) "Change Selected Image" else "Choose Image from Device",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedTextField(
                                    value = imageUrlInput,
                                    onValueChange = {
                                        imageUrlInput = it
                                        selectedImageUri = null
                                    },
                                    label = { Text("Or Image URL (Optional, e.g., https://...)") },
                                    singleLine = true,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    enabled = !isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (feedbackMsg.isNotBlank()) {
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = if (feedbackIsError) SolaceRedSoft else StatusActiveBg,
                            border = BorderStroke(
                                SolaceTokens.HairlineWidth,
                                if (feedbackIsError) SolaceRed.copy(alpha = 0.4f) else StatusActiveGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SolaceTokens.SpaceXL)
                                .padding(bottom = SolaceTokens.SpaceSM)
                        ) {
                            Text(
                                text = feedbackMsg,
                                color = if (feedbackIsError) ErrorRed else StatusActiveGreen,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(SolaceTokens.SpaceMD)
                            )
                        }
                    }

                    if (isSaving) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SolaceTokens.SpaceXL, vertical = SolaceTokens.SpaceSM),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SolaceRed,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(SolaceTokens.SpaceSM))
                            Text(
                                "Saving updates to Firebase...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral600
                            )
                        }
                    }
                }

                SolaceDivider()

                // Sticky bottom action footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SolaceTokens.SpaceXL, vertical = SolaceTokens.SpaceMD),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSuccessState) {
                        Button(
                            onClick = onSuccess,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSaving
                        ) {
                            Text("Cancel", color = Neutral600, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(SolaceTokens.SpaceSM))
                        Button(
                            onClick = {
                                val parsedPrice = price.toDoubleOrNull()
                                val parsedOccupancy = maxOccupancy.toIntOrNull()
                                val parsedLat = latitudeInput.trim().toDoubleOrNull() ?: campsite.location.latitude
                                val parsedLng = longitudeInput.trim().toDoubleOrNull() ?: campsite.location.longitude

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

                                        val updatedBase = campsite.copy(
                                            name = name.trim(),
                                            location = campsite.location.copy(
                                                locationName = parkLocation.trim(),
                                                latitude = parsedLat,
                                                longitude = parsedLng
                                            ),
                                            description = description.trim(),
                                            pricePerNight = parsedPrice,
                                            maxOccupancy = parsedOccupancy,
                                            rulesAndPolicy = rulesAndPolicyInput.trim().ifBlank { campsite.rulesAndPolicy }
                                        )

                                        val currentUri = selectedImageUri
                                        if (currentUri != null) {
                                            repository.uploadCampsiteImage(campsite.id, currentUri) { uploadResult ->
                                                uploadResult.fold(
                                                    onSuccess = { downloadUrl ->
                                                        val updatedSite = updatedBase.copy(imageUrl = downloadUrl)
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
                                            val updatedSite = updatedBase.copy(
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
                            enabled = !isSaving,
                            interactionSource = ctaInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                            modifier = Modifier.solaceTactileScale(ctaInteraction)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
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
