/**
 * CampsiteDetailsScreen.kt
 * 
 * Purpose: Composable screen rendering detailed information for a single campsite, including amenities, rules, lottery waitlist status, and booking triggers.
 * 
 * Issues / Outstanding Issues:
 * - QR code scanning navigates directly to campsite details based on the deep link payload.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.CampsiteRemovalDependencies
import com.example.solace_temp.models.UserRole
import kotlinx.coroutines.launch

/**
 * Composable displaying the details of a specific campsite.
 * Used by both Entrants (to join waitlists) and Owners (to view/share QR codes).
 *
 * @param campsiteId The ID of the campsite to display.
 * @param repository The data repository for fetching campsite data.
 * @param onBack Callback invoked when the user navigates back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteDetailsScreen(
    campsiteId: String,
    repository: AppRepository,
    onBack: () -> Unit,
    onSelectLottery: ((String) -> Unit)? = null
) {
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val profiles by repository.profiles.collectAsState()
    val context = LocalContext.current

    // State for administrative removal workflow and visual confirmation (US 03.01.01)
    var showAdminRemoveDialog by remember { mutableStateOf(false) }
    var removalDependencies by remember { mutableStateOf<CampsiteRemovalDependencies?>(null) }
    var isRemovingCampsite by remember { mutableStateOf(false) }
    var removedCampsiteName by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val campsite = campsites.find { it.id == campsiteId }

    // Display explicit visual confirmation if this admin just removed the campsite
    if (campsite == null && removedCampsiteName != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Campsite Removed",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Campsite \"$removedCampsiteName\" was successfully removed.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Associated listings, QR codes, and waitlists have been cleaned up and logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onBack) {
                    Text("Return to Registry")
                }
            }
        }
        return
    }

    if (campsite == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Error: Campsite data failed to load.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Avoid crashing when the stored QR payload is missing.
    val qrPayload = campsite.qrCodePayload.ifBlank { "solace://campsite/${campsite.id}" }
    val qrBitmap = remember(qrPayload) {
        QrCodeGeneratorUtil.generateQrBitmap(qrPayload)
    }
    val activeLottery = lotteries.find { it.campsiteIds.contains(campsite.id) && getLotteryStatus(it, campsite) != "Finished" && getLotteryStatus(it, campsite) != "N/A" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campsite Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Image Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!campsite.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = campsite.imageUrl,
                                contentDescription = "Campsite Promotional Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = "No Image", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(8.dp))
                                Text("No Image Available", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = campsite.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${campsite.pricePerNight.toInt()}/night",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "📍 ${campsite.location.locationName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "🌐 Coordinates: ${campsite.location.latitude}, ${campsite.location.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Max ${campsite.maxOccupancy} guests · ★ ${campsite.rating}", style = MaterialTheme.typography.bodyMedium)

                        // Tags
                        if (campsite.tags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                campsite.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = tag,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            campsite.description.ifBlank { "No description provided." },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Amenities
                        if (campsite.amenities.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            campsite.amenities.forEach { amenity ->
                                Text("• $amenity", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Information provided by the campsite owner.
                        Spacer(Modifier.height(16.dp))
                        Text("Rules & Cancellation Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            campsite.rulesAndPolicy.ifBlank {
                                "Quiet hours 11 PM - 7 AM. Full refund if forest fire warning."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("Owner Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val campsiteOwner = profiles.find { it.deviceId == campsite.ownerId }
                        if (campsiteOwner != null) {
                            Text("Name: ${campsiteOwner.name.ifBlank { "Registered Owner" }}")
                            if (campsiteOwner.email.isNotBlank()) Text("Email: ${campsiteOwner.email}")
                            if (campsiteOwner.phone.isNotBlank()) Text("Phone: ${campsiteOwner.phone}")
                        } else {
                            Text("Owner: Registered Owner (ID: ${campsite.ownerId})")
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Availability & Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (campsite.availableDates.isEmpty()) {
                            Text("Flexible availability. Select dates below to request booking.")
                        } else {
                            campsite.availableDates.forEach { availableDate ->
                                Text("• $availableDate")
                            }
                        }

                        // Active Lottery Listing Status & Entry Window Display
                        val lotteryStatus = getLotteryStatus(activeLottery, campsite)
                        val isLotteryActiveOrBooking = activeLottery != null && (lotteryStatus == "Active" || lotteryStatus == "Booking")

                        if (profile.role != UserRole.ENTRANT || isLotteryActiveOrBooking) {
                            Spacer(Modifier.height(16.dp))
                            Text("Active Lottery Listing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (activeLottery != null && onSelectLottery != null) {
                                            Modifier.clickable { onSelectLottery(activeLottery.id) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    if (activeLottery != null) {
                                        Text("Name: ${activeLottery.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Status: $lotteryStatus", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        Text(
                                            "Registration Window: ${activeLottery.registrationStart.ifBlank { formatDateMillis(activeLottery.registrationStartMillis) }} to ${activeLottery.registrationEnd.ifBlank { formatDateMillis(activeLottery.registrationEndMillis) }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Booking Window: ${activeLottery.bookingStart.ifBlank { formatDateMillis(activeLottery.bookingStartMillis) }} to ${activeLottery.bookingEnd.ifBlank { formatDateMillis(activeLottery.bookingEndMillis) }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (onSelectLottery != null) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Tap to view lottery details →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("No active lottery listing currently scheduled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CAMPSITE OWNER QR CODE & MANAGEMENT SECTION
            val isAuthorizedOwner = profile.deviceId == campsite.ownerId || (profile.isAdmin && profile.role == UserRole.ADMIN)

            if ((profile.role == UserRole.CAMPSITE_OWNER || (profile.role == UserRole.ADMIN && profile.isAdmin)) && isAuthorizedOwner) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Owner Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))

                        // Management Quick Actions
                        var showEditCampsiteDialog by remember { mutableStateOf(false) }
                        var showCreateLotteryDialog by remember { mutableStateOf(false) }
                        var ownerLotteryFeedback by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledIconButton(
                                    onClick = { showEditCampsiteDialog = true },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Details", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledIconButton(
                                    onClick = {
                                        val hasActive = !isCampsiteAvailableForLottery(campsite, lotteries)
                                        if (hasActive) {
                                            ownerLotteryFeedback = "Error: An active lottery already exists for this campsite."
                                        } else {
                                            ownerLotteryFeedback = ""
                                            showCreateLotteryDialog = true
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Create Lottery", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Text("Lottery", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledIconButton(onClick = { /* TODO */ }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Entrants", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Text("Entrants", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (showEditCampsiteDialog) {
                            EditCampsiteDialog(
                                campsite = campsite,
                                repository = repository,
                                onDismiss = { showEditCampsiteDialog = false },
                                onSuccess = {
                                    showEditCampsiteDialog = false
                                }
                            )
                        }

                        if (showCreateLotteryDialog) {
                            CreateLotteryDialog(
                                campsite = campsite,
                                repository = repository,
                                onDismiss = { showCreateLotteryDialog = false },
                                onSuccess = { msg: String ->
                                    ownerLotteryFeedback = msg
                                    showCreateLotteryDialog = false
                                }
                            )
                        }

                        if (ownerLotteryFeedback.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = ownerLotteryFeedback,
                                color = if (ownerLotteryFeedback.startsWith("Error")) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 16.dp))

                        Text(
                            "Promotional QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Unique Link: ${campsite.qrCodePayload}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Campsite QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { QrCodeGeneratorUtil.shareQrCodeImage(context, campsite, qrBitmap) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share / Save QR Code")
                        }
                    }
                }
            }

            // ADMINISTRATOR MODERATION SECTION (US 03.01.01)
            // Strictly accessible only to users with verified administrator privileges
            if (profile.isAdmin && profile.role == UserRole.ADMIN) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Administrator Moderation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Removing this campsite will immediately unlist it across the platform, clean up associated assets/waitlists, and log the action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                // Snapshot active dependencies right before opening confirmation dialog
                                removalDependencies = repository.getCampsiteRemovalDependencies(campsite.id)
                                showAdminRemoveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Campsite",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Remove Registered Campsite")
                        }
                    }
                }
            }
        }
    }

    // Show dependency-aware removal confirmation dialog when triggered
    val deps = removalDependencies
    if (showAdminRemoveDialog && deps != null) {
        AdminRemoveCampsiteDialog(
            campsite = campsite,
            dependencies = deps,
            isRemoving = isRemovingCampsite,
            onDismiss = {
                showAdminRemoveDialog = false
                removalDependencies = null
            },
            onConfirmRemoval = { forceOverride ->
                isRemovingCampsite = true
                val targetName = campsite.name
                repository.removeCampsiteAsAdmin(
                    campsiteId = campsite.id,
                    forceOverride = forceOverride
                ) { result ->
                    isRemovingCampsite = false
                    showAdminRemoveDialog = false
                    removalDependencies = null
                    result.onSuccess {
                        // Triggers the visual confirmation view once the campsite leaves state
                        removedCampsiteName = targetName
                    }.onFailure { error ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = error.message ?: "Failed to remove campsite."
                            )
                        }
                    }
                }
            }
        )
    }
}
