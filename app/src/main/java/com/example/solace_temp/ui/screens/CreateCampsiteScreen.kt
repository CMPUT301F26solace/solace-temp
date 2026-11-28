/**
 * CreateCampsiteScreen.kt
 *
 * Purpose: Composable screen allowing campsite owners to register new campsites with
 * live image URL preview, property details, GPS coordinates, tabular pricing & guest capacity,
 * and rules & cancellation policy inside grouped #FFFFFF cards on the #EBEBF2 canvas.
 *
 * Issues / Outstanding Issues:
 * - Geo-coordinates default to Banff region if custom coordinates are omitted.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Geolocation
import com.example.solace_temp.ui.components.PerspectivePageTransition
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.ErrorRed
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral50
import com.example.solace_temp.ui.theme.Neutral500
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.OwnerBackground
import com.example.solace_temp.ui.theme.SolaceCardShape
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceRedSoft
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceCardColors
import com.example.solace_temp.ui.theme.solaceCardElevation
import com.example.solace_temp.ui.theme.solaceTactileScale

// =========================================================================
// CREATE CAMPSITE SCREEN
// =========================================================================
/**
 * Composable screen allowing Owners to create a new campsite.
 *
 * @param repository The data repository for executing the creation logic.
 * @param onCreated Callback invoked after successful creation, receiving the new campsite's ID.
 */
@Composable
fun CreateCampsiteScreen(
    repository: AppRepository,
    onCreated: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    var name by remember { mutableStateOf("") }
    var parkLocation by remember { mutableStateOf("") }
    var latitudeInput by remember { mutableStateOf("51.1784") }
    var longitudeInput by remember { mutableStateOf("-115.5708") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var maxOccupancy by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var rulesAndPolicyInput by remember { mutableStateOf("") }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdCampsiteId by remember { mutableStateOf<String?>(null) }

    var showScreen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showScreen = true
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

    PerspectivePageTransition(
        showForeground = showScreen,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OwnerBackground)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OwnerBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SolaceTokens.SpaceLG)
                .padding(top = SolaceTokens.SpaceLG, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
        ) {
            // ── Header ──────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)) {
                Text(
                    text = "NEW LISTING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SolaceRed
                )
                Text(
                    text = "Create Campsite",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Neutral900
                )
                Text(
                    text = "Register your campsite and auto-generate a unique QR code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral600
                )
            }

            // ── Card 1: CAMPSITE IMAGE ──────────────────────────────────────────
            Card(
                shape = SolaceCardShape,
                colors = solaceCardColors(),
                border = solaceCardBorder(),
                elevation = solaceCardElevation(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SolaceTokens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Text(
                        text = "CAMPSITE IMAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Neutral500
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .clip(SolaceInnerCardShape)
                            .background(Neutral50)
                            .border(
                                SolaceTokens.HairlineWidth,
                                HairlineBorder,
                                SolaceInnerCardShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val trimmedUrl = imageUrlInput.trim()
                        if (trimmedUrl.isNotBlank()) {
                            AsyncImage(
                                model = trimmedUrl,
                                contentDescription = "Campsite Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            TopographicImageFallback(
                                modifier = Modifier.fillMaxSize(),
                                showLabel = true,
                                label = "Campsite Image Preview",
                                subtitle = "Enter an image URL below to preview"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("Image URL (Optional, e.g., https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                }
            }

            // ── Card 2: Property Details ────────────────────────────────────────
            Card(
                shape = SolaceCardShape,
                colors = solaceCardColors(),
                border = solaceCardBorder(),
                elevation = solaceCardElevation(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SolaceTokens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Text(
                        text = "PROPERTY DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Neutral500
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Campsite Name (Required)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Amenities (Required)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                }
            }

            // ── Card 3: Location & GPS Coordinates ──────────────────────────────
            Card(
                shape = SolaceCardShape,
                colors = solaceCardColors(),
                border = solaceCardBorder(),
                elevation = solaceCardElevation(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SolaceTokens.SpaceXL),
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
                            text = "LOCATION & GPS COORDINATES",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
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
                            modifier = Modifier.weight(1f),
                            shape = fieldShape,
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = longitudeInput,
                            onValueChange = { longitudeInput = it },
                            label = { Text("Longitude") },
                            singleLine = true,
                            textStyle = tnumTextStyle,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = fieldShape,
                            colors = fieldColors
                        )
                    }

                    val previewLat = latitudeInput.toDoubleOrNull() ?: 51.1784
                    val previewLng = longitudeInput.toDoubleOrNull() ?: -115.5708
                    Surface(
                        shape = SolaceInnerCardShape,
                        color = Neutral50,
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

            // ── Card 4: Pricing & Max Guest Capacity ────────────────────────────
            Card(
                shape = SolaceCardShape,
                colors = solaceCardColors(),
                border = solaceCardBorder(),
                elevation = solaceCardElevation(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SolaceTokens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Text(
                        text = "PRICING & MAX GUEST CAPACITY",
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = maxOccupancy,
                        onValueChange = { maxOccupancy = it },
                        label = { Text("Maximum Occupancy (Required)") },
                        singleLine = true,
                        textStyle = tnumTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
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

            // ── Card 5: Rules & Cancellation Policy ─────────────────────────────
            Card(
                shape = SolaceCardShape,
                colors = solaceCardColors(),
                border = solaceCardBorder(),
                elevation = solaceCardElevation(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(SolaceTokens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Text(
                        text = "RULES & CANCELLATION POLICY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Neutral500
                    )

                    OutlinedTextField(
                        value = rulesAndPolicyInput,
                        onValueChange = { rulesAndPolicyInput = it },
                        label = { Text("Rules & Cancellation Policy (Optional)") },
                        placeholder = {
                            Text(
                                "Quiet hours 11 PM - 7 AM. Full refund if forest fire warning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral500
                            )
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape,
                        colors = fieldColors
                    )
                }
            }

            if (feedbackMsg.isNotBlank()) {
                Surface(
                    shape = SolaceInnerCardShape,
                    color = if (feedbackIsError) SolaceRedSoft else CardSurface,
                    border = BorderStroke(
                        SolaceTokens.HairlineWidth,
                        if (feedbackIsError) SolaceRed.copy(alpha = 0.4f) else StatusActiveGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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

            Button(
                onClick = {
                    val parsedPrice = price.toDoubleOrNull()
                    val parsedOccupancy = maxOccupancy.toIntOrNull()
                    val parsedLat = latitudeInput.trim().toDoubleOrNull() ?: 51.1784
                    val parsedLng = longitudeInput.trim().toDoubleOrNull() ?: -115.5708

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
                            feedbackMsg = "Campsite registered successfully!"
                            val defaultPolicy = Campsite().rulesAndPolicy
                            val created = repository.createCampsite(
                                Campsite(
                                    ownerId = profile.deviceId,
                                    name = name.trim(),
                                    description = description.trim(),
                                    location = Geolocation(
                                        latitude = parsedLat,
                                        longitude = parsedLng,
                                        locationName = parkLocation.trim()
                                    ),
                                    pricePerNight = parsedPrice,
                                    maxOccupancy = parsedOccupancy,
                                    imageUrl = imageUrlInput.trim().ifBlank { null },
                                    rulesAndPolicy = rulesAndPolicyInput.trim().ifBlank { defaultPolicy }
                                )
                            )
                            createdCampsiteId = created.id
                            showSuccessDialog = true
                        }
                    }
                },
                interactionSource = ctaInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolaceRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .solaceTactileScale(ctaInteraction)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                Text(
                    text = "Generate QR Code & Publish",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSuccessDialog && createdCampsiteId != null) {
        AlertDialog(
            onDismissRequest = { /* Keep dialog until action */ },
            containerColor = CardSurface,
            titleContentColor = Neutral900,
            textContentColor = Neutral600,
            title = {
                Text(
                    "Campsite Created Successfully",
                    fontWeight = FontWeight.Bold,
                    color = Neutral900
                )
            },
            text = {
                Text("Your campsite has been registered and is now active. Tap below to view details.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = createdCampsiteId!!
                        showSuccessDialog = false
                        onCreated(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolaceRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                ) {
                    Text("View Details")
                }
            }
        )
    }
}
