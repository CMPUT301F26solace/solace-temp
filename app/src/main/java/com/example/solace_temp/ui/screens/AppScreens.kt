/**
 * AppScreens.kt
 * 
 * Purpose: Contains all the UI screens and their composables used in the application.
 * 
 * Needs Change/Issues:
 * - AppScreens.kt is a monolithic file that desperately needs splitting. Screens like [CampsiteDetailsScreen], [CampsiteListScreen], [CreateCampsiteScreen], and [ProfileAndRoleScreen] should each have their own file.
 * - Business logic is mixed directly inside Composables; should be extracted into ViewModels.
 */
package com.example.solace_temp.ui.screens

import android.content.Context
import android.content.Intent
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.validation.ProfileValidator
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

// =========================================================================
// Someone should split this file into separate files so everyone can work on their part without creating merge conflicts
//
//                      ^
//                     ^ ^
//                   ^     ^
//                 ^         ^
//               ^             ^
//             ^                 ^
//           ^                     ^
//         ^                         ^
//       ^-----------------------------^
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//                     | |
//
//
// =========================================================================

sealed class AppScreen(val route: String) {
    object EntrantNotifications : AppScreen("entrant_notifications")
    object EntrantReservations : AppScreen("entrant_reservations")
    object CampsiteReservation : AppScreen("campsite_reservation/{campsiteId}/{notificationId}") {
        fun createRoute(campsiteId: String, notificationId: String) = "campsite_reservation/$campsiteId/$notificationId"
    }
}

/**
 * Utility object for generating and sharing QR codes.
 */
object QrCodeGeneratorUtil {
    
    /**
     * Generates a QR Code Bitmap from the provided string payload.
     * 
     * @param payload The string data to encode into the QR code.
     * @param size The width and height in pixels for the generated QR code (default 512).
     * @return A [Bitmap] object representing the QR code.
     */
    fun generateQrBitmap(payload: String, size: Int = 512): Bitmap {
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Invokes the system share sheet to share a campsite's generated QR code image and deep link.
     * 
     * @param context The application context used to start the share intent.
     * @param campsite The [Campsite] object containing the data to share.
     * @param bitmap The generated QR code [Bitmap] to share.
     */
    fun shareQrCodeImage(context: Context, campsite: Campsite, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "campsite_${campsite.id}_qr.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${campsite.name} on Solace Campsites! Scan or open: ${campsite.qrCodePayload}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Campsite QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sharing just the text link if saving/sharing the image fails
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${campsite.name} on Solace Campsites! Scan or open: ${campsite.qrCodePayload}"
                )
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Campsite QR Link"))
        }
    }
}

/**
 * Formats a given timestamp in milliseconds into a readable date string.
 * @param millis The timestamp in milliseconds.
 * @return A formatted string like "Oct 24, 2026", or "Not Set" if null.
 */
fun formatDateMillis(millis: Long?): String {
    if (millis == null) return "Not Set"
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
}

/**
 * Determines the current status of a lottery based on registration and booking timestamps.
 * 
 * @param lottery The [Lottery] object to evaluate.
 * @return A string representing the status: "Upcoming", "Active", "Booking", "Finished", or "N/A".
 */
fun getLotteryStatus(lottery: Lottery?, campsite: Campsite? = null): String {
    if (lottery == null) return "N/A"
    val now = System.currentTimeMillis()
    val regStart = lottery.registrationStartMillis ?: 0L
    val regEnd = lottery.registrationEndMillis ?: Long.MAX_VALUE
    val bookingStart = lottery.bookingStartMillis ?: campsite?.bookingStartDateMillis ?: regEnd
    val bookingEnd = lottery.bookingEndMillis ?: campsite?.bookingEndDateMillis ?: Long.MAX_VALUE

    return when {
        now < regStart -> "Upcoming"
        now in regStart..regEnd -> "Active"
        lottery.isDrawn && now > bookingEnd -> "Finished"
        now in bookingStart..bookingEnd || now in (regEnd + 1)..bookingEnd || lottery.isDrawn -> "Booking"
        now > bookingEnd -> "Finished"
        else -> "N/A"
    }
}

// =========================================================================
// CAMPSITE DETAILS SCREEN (Shared by Entrant & Owner)
// =========================================================================
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

    val campsite = campsites.find { it.id == campsiteId }
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
        }
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
                                    Text(
                                        "Registration Deadline: ${activeLottery.registrationEnd.ifBlank { formatDateMillis(activeLottery.registrationEndMillis) }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    val status = getLotteryStatus(activeLottery, campsite)
                                    Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
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

            // ENTRANT ACTIONS: Join / Leave Waiting List (US 01.01.06, US 01.02.01)
            if (profile.role == UserRole.ENTRANT) {
                var showBookingDialog by remember { mutableStateOf(false) }
                var entrantFeedback by remember { mutableStateOf("") }
                
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showBookingDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Dates & Book")
                }
                
                if (entrantFeedback.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = entrantFeedback, 
                        color = if (entrantFeedback.startsWith("Error")) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showBookingDialog) {
                    val dateState = rememberDateRangePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showBookingDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selStart = dateState.selectedStartDateMillis
                                val selEnd = dateState.selectedEndDateMillis
                                val campStart = campsite.bookingStartDateMillis ?: 0L
                                val campEnd = campsite.bookingEndDateMillis ?: Long.MAX_VALUE
                                
                                if (selStart == null || selEnd == null) {
                                    entrantFeedback = "Error: Please select both a check-in and check-out date."
                                } else if (selStart < campStart || selEnd > campEnd) {
                                    entrantFeedback = "Error: Selected dates are outside the allowed booking period of ${formatDateMillis(campStart)} to ${formatDateMillis(campEnd)}."
                                } else {
                                    entrantFeedback = "Success! Dates are within the allowed booking period."
                                }
                                showBookingDialog = false
                            }) {
                                Text("Confirm Booking")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBookingDialog = false }) { Text("Cancel") }
                        }
                    ) {
                        DateRangePicker(state = dateState, modifier = Modifier.weight(1f))
                    }
                }
                
                // One shared join/leave UI for all lotteries on this campsite.
                // Uses asynchronous completion and per-lottery status messages.
                LotteryWaitingListSection(
                    campsiteId = campsite.id,
                    repository = repository
                )
            }

            // CAMPSITE OWNER QR CODE & MANAGEMENT SECTION
            val isAuthorizedOwner = profile.deviceId == campsite.ownerId || profile.isAdmin
            
            if ((profile.role == UserRole.CAMPSITE_OWNER || profile.role == UserRole.ADMIN) && isAuthorizedOwner) {
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
                                        val hasActive = lotteries.any { it.campsiteIds.contains(campsite.id) && !it.isDrawn }
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
        }
    }
}

// =========================================================================
// CAMPSITE LIST SCREEN (Used by Entrant Search & Owner My Campsites)
// =========================================================================
/**
 * Composable screen to display a list of campsites, with a search filter.
 * 
 * @param title The title text for this screen.
 * @param repository The data repository for fetching campsite data.
 * @param onSelectCampsite Callback invoked when a campsite is tapped, receiving the campsite ID.
 */
@Composable
fun CampsiteListScreen(
    title: String,
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit
) {
    val campsites by repository.campsites.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = campsites.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.location.locationName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name or Alberta park...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank())
                    "No campsites available."
                else
                    "No campsites found.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { site ->
                val ratingLabel = if (site.rating > 0.0) {
                    "★ ${site.rating}"
                } else {
                    "Not yet rated"
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCampsite(site.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("$${site.pricePerNight.toInt()}/night", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "📍 ${site.location.locationName} · " + "Max ${site.maxOccupancy} guests · $ratingLabel")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to view details & QR code →", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}

/**
 * Composable screen for campsite owners to view a list of their owned campsites.
 * Handles the case where the owner has no campsites yet.
 * 
 * @param repository The data repository for fetching campsite data.
 * @param onSelectCampsite Callback invoked when a campsite is tapped, receiving the campsite ID.
 * @param onCreateNew Callback invoked to navigate to the create campsite screen.
 */
@Composable
fun OwnerCampsitesScreen(
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    
    // Filter to only include campsites owned by this user
    val myCampsites = campsites.filter { it.ownerId == profile.deviceId }
    val ownerBackgroundBlue = Color(0xFFE8EEF8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ownerBackgroundBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My Registered Campsites", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "View your registered campsites, monitor active lottery statuses, and manage bookings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))

        if (myCampsites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "You haven't registered any campsites yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add a Campsite")
                    }
                }
            }
        } else {
            myCampsites.forEach { site ->
                val activeLottery = lotteries.find { it.campsiteIds.contains(site.id) && getLotteryStatus(it, site) != "Finished" && getLotteryStatus(it, site) != "N/A" }
                val status = getLotteryStatus(activeLottery, site)
                val ratingLabel = if (site.rating > 0.0) "★ ${site.rating}" else "Not yet rated"

                val (statusBg, statusTextColor, badgeColor) = when (status) {
                    "Active" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF2E7D32))
                    "Upcoming" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), Color(0xFF1976D2))
                    "Booking" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), Color(0xFF7B1FA2))
                    else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), Color(0xFF9E9E9E))
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCampsite(site.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Row: Thumbnail on left, Info on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Placeholder Image Box
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE3E8EF)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!site.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = site.imageUrl,
                                        contentDescription = "Campsite Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = "Placeholder Image", tint = Color.Gray, modifier = Modifier.size(36.dp))
                                }
                            }

                            // Campsite info column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = site.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$${site.pricePerNight.toInt()}/night",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFE11D48),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "📍 ${site.location.locationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Max ${site.maxOccupancy} guests · $ratingLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 2.dp))

                        // Lottery Status & Extra Space for Lottery Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = statusBg,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val lotteryPrefix = if (activeLottery != null && activeLottery.campsiteIds.size > 1) "Multi Lottery:" else "Lottery:"
                                Text(
                                    text = if (activeLottery != null) "$lotteryPrefix ${activeLottery.name}" else "Lottery Status",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (activeLottery != null) {
                                Text(
                                    text = "Registration Window: ${activeLottery.registrationStart} to ${activeLottery.registrationEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Booking Window: ${activeLottery.bookingStart} to ${activeLottery.bookingEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Enrolled: ${activeLottery.waitingListEntrantIds.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Max Winners: ${activeLottery.maxWinners}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Configure a registration period for this campsite from details view.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Tap to view details & QR code →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// CREATE CAMPSITE SCREEN
// =========================================================================
/**
 * Composable screen allowing Owners to create a new campsite.
 * 
 * @param repository The data repository for executing the creation logic.
 * @param onCreated Callback invoked after successful creation, receiving the new campsite's ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampsiteScreen(
    repository: AppRepository,
    onCreated: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    var name by remember { mutableStateOf("") }
    var parkLocation by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var maxOccupancy by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdCampsiteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Campsite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Register your campsite and auto-generate a unique QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
            colors = textFieldColors
        )
        OutlinedTextField(
            value = parkLocation,
            onValueChange = { parkLocation = it },
            label = { Text("Location (Required, e.g., Banff, Jasper)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description & Amenities (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price per Night ($) (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = { maxOccupancy = it },
            label = { Text("Maximum Occupancy (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = imageUrlInput,
            onValueChange = { imageUrlInput = it },
            label = { Text("Image URL (Optional, e.g., https://...)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )

        if (feedbackMsg.isNotBlank()) {
            Text(
                text = feedbackMsg,
                color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

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
                        feedbackMsg = "Campsite registered successfully!"
                        val created = repository.createCampsite(
                            Campsite(
                                ownerId = profile.deviceId,
                                name = name.trim(),
                                description = description.trim(),
                                location = Geolocation(locationName = parkLocation.trim()),
                                pricePerNight = parsedPrice,
                                maxOccupancy = parsedOccupancy,
                                imageUrl = imageUrlInput.trim().ifBlank { null }
                            )
                        )
                        createdCampsiteId = created.id
                        showSuccessDialog = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Generate QR Code & Publish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    if (showSuccessDialog && createdCampsiteId != null) {
        AlertDialog(
            onDismissRequest = { /* Keep dialog until action */ },
            title = { Text("Campsite Created Successfully") },
            text = { Text("Your campsite has been registered and is now active. Tap below to view details.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = createdCampsiteId!!
                        showSuccessDialog = false
                        onCreated(id)
                    }
                ) {
                    Text("View Details")
                }
            }
        )
    }
}

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

        // Acceptance Criteria: Admin can navigate between views within the same profile[cite: 15]
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
                    Text("Receive Notifications")
                    Switch(checked = receiveNotifs, onCheckedChange = { receiveNotifs = it })
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

// =========================================================================
// OWNER LOTTERIES SCREEN
// =========================================================================
/**
 * Composable screen for campsite owners to view a list of their managed lotteries.
 * 
 * @param repository The data repository for fetching lotteries and profile data.
 * @param onSelectLottery Callback invoked when a lottery item is tapped, receiving the lottery ID.
 */
@Composable
fun OwnerLotteriesScreen(
    repository: AppRepository,
    onSelectLottery: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()

    // Show only lotteries whose campsites actually belong to this owner.
    val ownedCampsiteIds = campsites.filter { it.ownerId == profile.deviceId }
        .map { it.id }.toSet()
    val ownedLotteries = lotteries.filter { lottery ->
        lottery.ownerId == profile.deviceId &&
            lottery.campsiteIds.isNotEmpty() &&
            lottery.campsiteIds.all { it in ownedCampsiteIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Manage Lotteries",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "View and manage waiting list lotteries across your registered campsites.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        if (ownedLotteries.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "No Lotteries",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No lotteries configured yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create a lottery from any of your campsite details pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            ownedLotteries.forEach { lottery ->
                val associatedCampsites = campsites.filter { lottery.campsiteIds.contains(it.id) }
                val campsiteNames = associatedCampsites.joinToString(", ") { it.name }.ifBlank { "Assigned Campsites" }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLottery(lottery.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lottery.name.ifBlank { "Campsite Lottery" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            val status = getLotteryStatus(lottery, associatedCampsites.firstOrNull())
                            val badgeColor = when (status) {
                                "Active" -> Color(0xFF2E7D32)
                                "Upcoming" -> Color(0xFF1976D2)
                                "Booking" -> Color(0xFF7B1FA2)
                                "Finished" -> Color(0xFF555555)
                                else -> Color(0xFF9E9E9E)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor
                            ) {
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "📍 $campsiteNames",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Registration Window", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.registrationStart} to ${lottery.registrationEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Booking Window", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.bookingStart} to ${lottery.bookingEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Waitlist Entrants", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.waitingListEntrantIds.size} Enrolled",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Room for more future stuff
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Max Winners: ${lottery.maxWinners}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Tap for Details & Draw Controls →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatLotteryCountdown(targetMillis: Long, nowMillis: Long): String {
    val left = (targetMillis - nowMillis).coerceAtLeast(0L)
    val days = left / 86_400_000L
    val hours = (left / 3_600_000L) % 24
    val minutes = (left / 60_000L) % 60
    return "${days}d ${hours}h ${minutes}m"
}

// =========================================================================
// LOTTERY DETAILS SCREEN (Placeholder for upcoming features)
// =========================================================================
/**
 * Composable screen displaying detailed information about a specific lottery.
 * 
 * @param lotteryId The unique ID of the lottery.
 * @param repository The data repository for fetching lottery data.
 * @param onBack Callback invoked when navigating back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryDetailsScreen(
    lotteryId: String,
    repository: AppRepository,
    onBack: () -> Unit,
    onSelectCampsite: (String) -> Unit,
    onViewWaitlist: (String) -> Unit
) {
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val lottery = lotteries.find { it.id == lotteryId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lottery?.name ?: "Lottery Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (lottery == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Error: Lottery data not found.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        } else {
            val associatedCampsites = campsites.filter { lottery.campsiteIds.contains(it.id) }
            var countdownNow by remember(lottery.id) { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(lottery.id) {
                while (true) {
                    delay(60_000)
                    countdownNow = System.currentTimeMillis()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = lottery.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Associated Campsites:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (associatedCampsites.isEmpty()) {
                            Text("None", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            associatedCampsites.forEach { site ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCampsite(site.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(site.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                            Text("📍 ${site.location.locationName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                        }
                                        Text("View Details →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()
                        Text(
                            text = "Registration Window: ${lottery.registrationStart} to ${lottery.registrationEnd}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Booking Window: ${lottery.bookingStart} to ${lottery.bookingEnd}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Maximum Winners: ${lottery.maxWinners}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Waitlist Entrants Enrolled: ${lottery.waitingListEntrantIds.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Waitlist capacity: ${lottery.waitlistCapacity?.toString() ?: "Unlimited"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (lottery.ownerId == profile.deviceId) {
                            Button(
                                onClick = { onViewWaitlist(lottery.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("View Waitlist") }
                        }
                        val lotteryStatus = getLotteryStatus(lottery, associatedCampsites.firstOrNull())
                        val statusTextColor = when (lotteryStatus) {
                            "Active" -> Color(0xFF2E7D32)
                            "Upcoming" -> Color(0xFF1976D2)
                            "Booking" -> Color(0xFF7B1FA2)
                            "Finished" -> Color(0xFF555555)
                            else -> Color(0xFF9E9E9E)
                        }
                        Text(
                            text = "Lottery Status: $lotteryStatus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        when (lotteryStatus) {
                            "Upcoming" -> lottery.registrationStartMillis?.let { openingTime ->
                                Text("Lottery opening in:")
                                Text(formatLotteryCountdown(openingTime, countdownNow))
                            }
                            "Active" -> lottery.registrationEndMillis?.let { drawingTime ->
                                Text("Lottery drawing in:")
                                Text(formatLotteryCountdown(drawingTime, countdownNow))
                            }
                            "Booking" -> {
                                val reservedEntrants = reservations.asSequence()
                                    .filter { it.campsiteId in lottery.campsiteIds && it.status == ReservationStatus.ACTIVE }
                                    .map { it.entrantId }
                                    .distinct()
                                    .count()
                                Text("People who have reserved: $reservedEntrants")
                                if (lottery.ownerId == profile.deviceId && lottery.isDrawn) {
                                    Button(
                                        onClick = { repository.redrawLottery(lottery.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Redraw") }
                                }
                            }
                            else -> Unit
                        }

                        var showEditLotteryDialog by remember { mutableStateOf(false) }

                        if (lottery.ownerId == profile.deviceId || profile.isAdmin) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showEditLotteryDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Lottery", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Lottery")
                            }
                        }

                        if (showEditLotteryDialog) {
                            EditLotteryDialog(
                                lottery = lottery,
                                repository = repository,
                                onDismiss = { showEditLotteryDialog = false },
                                onSuccess = { showEditLotteryDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// CREATE LOTTERY DIALOG
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

// =========================================================================
// EDIT LOTTERY DIALOG
// =========================================================================
/**
 * Dialog allowing Campsite Owners or Admins to edit an existing lottery's configuration.
 * 
 * Configurable fields include lottery name, max winners, waitlist capacity, assigned campsites,
 * registration window, and booking window. Enforces registration date editing constraints based
 * on lottery status (Upcoming allows editing start & end; Active allows editing end date only),
 * and validates that booking start is at least 1 week after registration close.
 * 
 * @param lottery The [Lottery] entity being edited.
 * @param repository The data repository used to save lottery updates.
 * @param onDismiss Callback invoked when the dialog is dismissed or cancelled.
 * @param onSuccess Callback invoked when the lottery edit is completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLotteryDialog(
    lottery: Lottery,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val lotteries by repository.lotteries.collectAsState()

    var lotteryName by remember(lottery) { mutableStateOf(lottery.name) }
    var selectedCampsiteIds by remember(lottery) { mutableStateOf(lottery.campsiteIds) }
    var maxWinnersText by remember(lottery) { mutableStateOf(lottery.maxWinners.toString()) }
    var waitlistCapacityText by remember(lottery) { mutableStateOf(lottery.waitlistCapacity?.toString() ?: "") }
    var regStartMillis by remember(lottery) { mutableStateOf(lottery.registrationStartMillis) }
    var regEndMillis by remember(lottery) { mutableStateOf(lottery.registrationEndMillis) }
    var bookingStartMillis by remember(lottery) { mutableStateOf(lottery.bookingStartMillis) }
    var bookingEndMillis by remember(lottery) { mutableStateOf(lottery.bookingEndMillis) }

    var showRegRangeDialog by remember { mutableStateOf(false) }
    var showRegEndSingleDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter { it.ownerId == profile.deviceId }.ifEmpty {
        campsites.filter { lottery.campsiteIds.contains(it.id) }
    }
    // Only display campsites that are available or currently assigned to this lottery
    val availableOwnerCampsites = ownerCampsites.filter { site ->
        selectedCampsiteIds.contains(site.id) || !lotteries.any { lot ->
            lot.id != lottery.id && lot.campsiteIds.contains(site.id) && !lot.isDrawn && getLotteryStatus(lot, site) != "Finished" && getLotteryStatus(lot, site) != "N/A"
        }
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
    val totalMaxGuests = activeCampsites.sumOf { it.maxOccupancy }

    val status = getLotteryStatus(lottery, activeCampsites.firstOrNull())

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    Text("Lottery Updated", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Edit Lottery Configuration", fontWeight = FontWeight.Bold)
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
                        "Lottery configuration for \"${lotteryName.trim()}\" has been successfully updated.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Changes are saved and live in the system.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Update the lottery settings below. Required fields and date spacing rules will be verified before saving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = lotteryName,
                        onValueChange = { lotteryName = it },
                        label = { Text("Lottery Name (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxWinnersText,
                        onValueChange = { maxWinnersText = it },
                        label = { Text("Number of Lottery Winners (Required)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waitlistCapacityText,
                        onValueChange = { waitlistCapacityText = it },
                        label = { Text("Waitlist capacity (optional)") },
                        supportingText = { Text("Leave blank for unlimited entrants") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assigned Campsite(s):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    availableOwnerCampsites.forEach { site ->
                        val isSelected = selectedCampsiteIds.contains(site.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCampsiteIds = if (isSelected) {
                                        if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                    } else {
                                        selectedCampsiteIds + site.id
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedCampsiteIds = if (checked) {
                                        selectedCampsiteIds + site.id
                                    } else {
                                        if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${site.name} (Max Guests: ${site.maxOccupancy})", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Total Max Guest Capacity: $totalMaxGuests", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Registration Window: ${formatDateMillis(regStartMillis)} to ${formatDateMillis(regEndMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    when (status) {
                        "Upcoming" -> {
                            OutlinedButton(
                                onClick = { showRegRangeDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Registration Start & End Dates")
                            }
                        }
                        "Active" -> {
                            OutlinedButton(
                                onClick = { showRegEndSingleDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Registration End Date Only")
                            }
                            Text(
                                "(Registration is currently Active. Open date cannot be modified.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                "(Registration period has closed. Registration dates cannot be modified.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Booking Window: ${formatDateMillis(bookingStartMillis)} to ${formatDateMillis(bookingEndMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = { showBookingDateDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Booking Window Dates")
                    }

                    if (feedbackMsg.isNotBlank()) {
                        Text(
                            text = feedbackMsg,
                            color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccessState) {
                Button(onClick = onSuccess) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        val parsedWinners = maxWinnersText.toIntOrNull()
                        val parsedCapacity = if (waitlistCapacityText.isBlank()) null else waitlistCapacityText.toIntOrNull()

                        when {
                            lotteryName.isBlank() -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Lottery name cannot be empty."
                            }
                            parsedWinners == null || parsedWinners <= 0 -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Number of lottery winners must be greater than 0."
                            }
                            parsedWinners > totalMaxGuests -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Number of winners ($parsedWinners) cannot exceed the total maximum guest capacity ($totalMaxGuests) of the selected campsite(s)."
                            }
                            waitlistCapacityText.isNotBlank() && (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
                            }
                            regStartMillis == null || regEndMillis == null -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Please select both a start and closing date/time."
                            }
                            bookingStartMillis == null || bookingEndMillis == null -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Please select both a start and end date for the booking window."
                            }
                            regEndMillis!! <= regStartMillis!! -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Registration close date must be strictly after the open date."
                            }
                            bookingEndMillis!! <= bookingStartMillis!! -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Booking end date must be strictly after the start date."
                            }
                            bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Booking period must start at least 1 week after the registration period."
                            }
                            status == "Upcoming" && regStartMillis!! < System.currentTimeMillis() - 86400000L -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Registration open date must be set in the future."
                            }
                            status == "Active" && regEndMillis!! < System.currentTimeMillis() - 86400000L -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Registration close date must be set in the future."
                            }
                            else -> {
                                feedbackIsError = false
                                val updated = lottery.copy(
                                    name = lotteryName.trim(),
                                    campsiteIds = selectedCampsiteIds,
                                    maxWinners = parsedWinners,
                                    waitlistCapacity = parsedCapacity,
                                    registrationStartMillis = regStartMillis,
                                    registrationEndMillis = regEndMillis,
                                    registrationStart = formatDateMillis(regStartMillis),
                                    registrationEnd = formatDateMillis(regEndMillis),
                                    bookingStartMillis = bookingStartMillis,
                                    bookingEndMillis = bookingEndMillis,
                                    bookingStart = formatDateMillis(bookingStartMillis),
                                    bookingEnd = formatDateMillis(bookingEndMillis)
                                )
                                repository.updateLottery(updated) { result ->
                                    result.fold(
                                        onSuccess = {
                                            isSuccessState = true
                                        },
                                        onFailure = { err ->
                                            feedbackIsError = true
                                            feedbackMsg = "Error updating lottery: ${err.message}"
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            if (!isSuccessState) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showRegRangeDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = regStartMillis,
            initialSelectedEndDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regStartMillis = dateState.selectedStartDateMillis
                    regEndMillis = dateState.selectedEndDateMillis
                    showRegRangeDialog = false
                }) { Text("Save Dates") }
            },
            dismissButton = { TextButton(onClick = { showRegRangeDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }

    if (showRegEndSingleDialog) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegEndSingleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regEndMillis = dateState.selectedDateMillis
                    showRegEndSingleDialog = false
                }) { Text("Save End Date") }
            },
            dismissButton = { TextButton(onClick = { showRegEndSingleDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(state = dateState, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showBookingDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = bookingStartMillis,
            initialSelectedEndDateMillis = bookingEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showBookingDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    bookingStartMillis = dateState.selectedStartDateMillis
                    bookingEndMillis = dateState.selectedEndDateMillis
                    showBookingDateDialog = false
                }) { Text("Save Dates") }
            },
            dismissButton = { TextButton(onClick = { showBookingDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}

// =========================================================================
/**
 * Dialog allowing Owners to configure and initiate a lottery registration period for an owned campsite.
 * 
 * @param campsite The [Campsite] to associate the lottery with.
 * @param repository The data repository for executing the creation logic.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onSuccess Callback invoked when the lottery is successfully created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLotteryDialog(
    campsite: Campsite,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    var lotteryName by remember { mutableStateOf("${campsite.name} Lottery") }
    var selectedCampsiteIds by remember { mutableStateOf(listOf(campsite.id)) }
    var maxWinnersText by remember { mutableStateOf("3") }
    var waitlistCapacityText by remember { mutableStateOf("") }
    var regStartMillis by remember { mutableStateOf<Long?>(null) }
    var regEndMillis by remember { mutableStateOf<Long?>(null) }
    var bookingStartMillis by remember { mutableStateOf<Long?>(null) }
    var bookingEndMillis by remember { mutableStateOf<Long?>(null) }
    var showRegDateDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter { it.ownerId == profile.deviceId }.ifEmpty { listOf(campsite) }
    // Only display campsites that are not currently part of an active/undrawn lottery
    val availableOwnerCampsites = ownerCampsites.filter { site ->
        site.id == campsite.id || !lotteries.any { lot ->
            lot.campsiteIds.contains(site.id) && !lot.isDrawn && getLotteryStatus(lot, site) != "Finished" && getLotteryStatus(lot, site) != "N/A"
        }
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
    val totalMaxGuests = activeCampsites.sumOf { it.maxOccupancy }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure & Initiate Lottery") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Specify lottery name, registration & booking windows, assigned campsites, and winner count for ${campsite.name}.", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = lotteryName,
                    onValueChange = { lotteryName = it },
                    label = { Text("Lottery Name (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxWinnersText,
                    onValueChange = { maxWinnersText = it },
                    label = { Text("Number of Lottery Winners (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waitlistCapacityText,
                    onValueChange = { waitlistCapacityText = it },
                    label = { Text("Waitlist capacity (optional)") },
                    supportingText = { Text("Leave blank for unlimited entrants") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assigned Campsite(s):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                availableOwnerCampsites.forEach { site ->
                    val isSelected = selectedCampsiteIds.contains(site.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCampsiteIds = if (isSelected) {
                                    if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                } else {
                                    selectedCampsiteIds + site.id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedCampsiteIds = if (checked) {
                                    selectedCampsiteIds + site.id
                                } else {
                                    if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${site.name} (Max Guests: ${site.maxOccupancy})", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Total Max Guest Capacity: $totalMaxGuests", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(4.dp))
                Text(
                    "Registration Window: ${formatDateMillis(regStartMillis)} to ${formatDateMillis(regEndMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { showRegDateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Registration Open & Close Dates")
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Booking Window: ${formatDateMillis(bookingStartMillis)} to ${formatDateMillis(bookingEndMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { showBookingDateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Booking Window Dates")
                }

                if (feedbackMsg.isNotBlank()) {
                    Text(
                        text = feedbackMsg,
                        color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hasActive = lotteries.any { it.campsiteIds.contains(campsite.id) && getLotteryStatus(it) != "Finished" }
                    val parsedWinners = maxWinnersText.toIntOrNull()
                    val parsedCapacity = if (waitlistCapacityText.isBlank()) null
                        else waitlistCapacityText.toIntOrNull()

                    when {
                        hasActive -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: An active lottery already exists for this campsite."
                        }
                        lotteryName.isBlank() -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Lottery name cannot be empty."
                        }
                        parsedWinners == null || parsedWinners <= 0 -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Number of lottery winners must be greater than 0."
                        }
                        parsedWinners > totalMaxGuests -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Number of winners ($parsedWinners) cannot exceed the total maximum guest capacity ($totalMaxGuests) of the selected campsite(s)."
                        }
                        waitlistCapacityText.isNotBlank() &&
                            (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
                        }
                        regStartMillis == null || regEndMillis == null -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Please select both a start and closing date/time."
                        }
                        bookingStartMillis == null || bookingEndMillis == null -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Please select both a start and end date for the booking window."
                        }
                        regEndMillis!! <= regStartMillis!! -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Registration close date must be strictly after the open date."
                        }
                        bookingEndMillis!! <= bookingStartMillis!! -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Booking end date must be strictly after the start date."
                        }
                        bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Booking period must start at least 1 week after the registration period."
                        }
                        regStartMillis!! < System.currentTimeMillis() - 86400000 -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Registration open date must be set in the future."
                        }
                        else -> {
                            feedbackIsError = false
                            repository.createLottery(
                                Lottery(
                                    name = lotteryName.trim(),
                                    campsiteIds = selectedCampsiteIds,
                                    ownerId = profile.deviceId,
                                    maxWinners = parsedWinners,
                                    waitlistCapacity = parsedCapacity,
                                    registrationStartMillis = regStartMillis,
                                    registrationEndMillis = regEndMillis,
                                    registrationStart = formatDateMillis(regStartMillis),
                                    registrationEnd = formatDateMillis(regEndMillis),
                                    bookingStartMillis = bookingStartMillis,
                                    bookingEndMillis = bookingEndMillis,
                                    bookingStart = formatDateMillis(bookingStartMillis),
                                    bookingEnd = formatDateMillis(bookingEndMillis)
                                )
                            )
                            onSuccess("Lottery registration period and winners saved successfully!")
                        }
                    }
                }
            ) {
                Text("Publish Lottery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showRegDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = regStartMillis,
            initialSelectedEndDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regStartMillis = dateState.selectedStartDateMillis
                    regEndMillis = dateState.selectedEndDateMillis
                    showRegDateDialog = false
                }) { Text("Save Dates") }
            },
            dismissButton = { TextButton(onClick = { showRegDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }

    if (showBookingDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = bookingStartMillis,
            initialSelectedEndDateMillis = bookingEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showBookingDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    bookingStartMillis = dateState.selectedStartDateMillis
                    bookingEndMillis = dateState.selectedEndDateMillis
                    showBookingDateDialog = false
                }) { Text("Save Dates") }
            },
            dismissButton = { TextButton(onClick = { showBookingDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}

// =========================================================================
// PLACEHOLDER SCREEN FOR TEAMMATES TO PLUG IN THEIR STORIES
// =========================================================================
/**
 * A generic placeholder screen component used for indicating missing functionality
 * or screens meant to be implemented by other teammates.
 * 
 * @param title The title representing the feature.
 * @param userStoriesCovered The agile user stories related to this placeholder.
 */
@Composable
fun TeammatePlaceholderScreen(title: String, userStoriesCovered: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Teammate Story Slot: $userStoriesCovered", color = Color.Gray)
            }
        }
    }
}

/**
 * Displays the administrator registry view for browsing all platform campsites.
 *
 * This screen enforces role-based access control, ensuring only verified administrators
 * can access the directory. It provides robust data management capabilities including
 * text-based search (by name, location, or owner ID), multi-criteria filtering
 * (by status, active lottery association, and creation date range via a dialog picker),
 * and chronological sorting. The UI utilizes virtual scrolling to efficiently handle
 * large datasets and provides explicit visual feedback for empty or unmatched search states.
 *
 * @param repository The data repository providing state flows for campsites, lotteries, and the current user profile.
 * @param onSelectCampsite Callback triggered with the target campsite's ID when an admin taps a card for moderation or review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCampsitesScreen(
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit
) {
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<CampsiteStatus?>(null) }
    var requireActiveLottery by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(true) }

    // NEW: State for Date Created Filter
    var showDateFilterDialog by remember { mutableStateOf(false) }
    var dateFilterStart by remember { mutableStateOf<Long?>(null) }
    var dateFilterEnd by remember { mutableStateOf<Long?>(null) }

    if (profile.role != UserRole.ADMIN) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Access Denied: Administrator credentials required.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    // UPDATED: Filter logic now checks the dateCreatedMillis
    val filteredAndSorted = campsites.filter { site ->
        val matchesSearch = site.name.contains(searchQuery, ignoreCase = true) ||
                site.location.locationName.contains(searchQuery, ignoreCase = true) ||
                site.ownerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || site.status == selectedStatusFilter
        val matchesLottery = !requireActiveLottery || lotteries.any { it.campsiteIds.contains(site.id) }

        // Check if the campsite's creation date falls within the selected filter range
        val matchesDate = if (dateFilterStart != null && dateFilterEnd != null) {
            site.dateCreatedMillis in dateFilterStart!!..dateFilterEnd!!
        } else true

        matchesSearch && matchesStatus && matchesLottery && matchesDate
    }.let { list ->
        if (sortDescending) list.sortedByDescending { it.dateCreatedMillis }
        else list.sortedBy { it.dateCreatedMillis }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEDE8F8)).padding(16.dp)
    ) {
        Text("Admin: Campsite Registry", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, location, or owner ID...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == null && !requireActiveLottery && dateFilterStart == null,
                    onClick = {
                        selectedStatusFilter = null
                        requireActiveLottery = false
                        dateFilterStart = null
                        dateFilterEnd = null
                    },
                    label = { Text("All") }
                )

                // NEW: Date Created Filter Chip
                FilterChip(
                    selected = dateFilterStart != null,
                    onClick = { showDateFilterDialog = true },
                    label = { Text(if (dateFilterStart != null) "Date Filtered" else "Filter Date") }
                )

                FilterChip(
                    selected = requireActiveLottery,
                    onClick = { requireActiveLottery = !requireActiveLottery },
                    label = { Text("Has Lottery") }
                )
                CampsiteStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status.name) }
                    )
                }
            }

            IconButton(onClick = { sortDescending = !sortDescending }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Sort by Date",
                    tint = if (sortDescending) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredAndSorted.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank() && selectedStatusFilter == null && !requireActiveLottery && dateFilterStart == null)
                    "No campsites registered in the platform."
                else
                    "No campsites match search/filter criteria.",
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredAndSorted) { site ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectCampsite(site.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(site.name, fontWeight = FontWeight.Bold)

                            val statusColor = when(site.status) {
                                CampsiteStatus.ACTIVE -> Color(0xFF2E7D32)
                                CampsiteStatus.DRAFT -> Color(0xFF757575)
                                CampsiteStatus.FLAGGED -> Color(0xFFD32F2F)
                            }
                            Text(site.status.name, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                        Text("Owner ID: ${site.ownerId}")
                        Text("📍 ${site.location.locationName} · Capacity: ${site.maxOccupancy}")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to manage/moderate →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // NEW: Dialog popup for selecting the date range
    if (showDateFilterDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = dateFilterStart,
            initialSelectedEndDateMillis = dateFilterEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDateFilterDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dateFilterStart = dateState.selectedStartDateMillis
                    // Add roughly 24 hours to the end date so it includes the full closing day
                    dateFilterEnd = dateState.selectedEndDateMillis?.plus(86399999)
                    showDateFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dateFilterStart = null
                    dateFilterEnd = null
                    showDateFilterDialog = false
                }) { Text("Clear") }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}

/**
 * Displays the administrator directory for browsing all registered profiles.
 * Implements search, role-based filtering, and date sorting with virtual scrolling
 * to handle high data volumes gracefully.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfilesScreen(
    repository: AppRepository,
    onSelectProfile: (String) -> Unit
) {
    val profilesList by repository.profiles.collectAsState(initial = emptyList())
    val currentProfile by repository.currentProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<AccountStatus?>(null) }
    var sortDescending by remember { mutableStateOf(true) }

    var showDateFilterDialog by remember { mutableStateOf(false) }
    var dateFilterStart by remember { mutableStateOf<Long?>(null) }
    var dateFilterEnd by remember { mutableStateOf<Long?>(null) }

    // Acceptance Criteria: Only users with verified administrator credentials can access[cite: 12]
    if (currentProfile.role != UserRole.ADMIN) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Access Denied: Administrator credentials required.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    // Apply Search, Filter, and Sort criteria
    val filteredAndSorted = profilesList.filter { user ->
        val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.deviceId.contains(searchQuery, ignoreCase = true)

        val matchesRole = selectedRoleFilter == null || user.role == selectedRoleFilter
        val matchesStatus = selectedStatusFilter == null || user.accountStatus == selectedStatusFilter

        val matchesDate = if (dateFilterStart != null && dateFilterEnd != null) {
            user.registrationDateMillis in dateFilterStart!!..dateFilterEnd!!
        } else true

        matchesSearch && matchesRole && matchesStatus && matchesDate
    }.let { list ->
        if (sortDescending) list.sortedByDescending { it.registrationDateMillis }
        else list.sortedBy { it.registrationDateMillis }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEDE8F8)).padding(16.dp)) {
        Text("Admin: User Directory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // Search by name, email, or user identifier
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, email, or ID...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Filter options for role, status, and creation date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == null && selectedStatusFilter == null && dateFilterStart == null,
                    onClick = {
                        selectedRoleFilter = null
                        selectedStatusFilter = null
                        dateFilterStart = null
                        dateFilterEnd = null
                    },
                    label = { Text("All") }
                )

                FilterChip(
                    selected = dateFilterStart != null,
                    onClick = { showDateFilterDialog = true },
                    label = { Text(if (dateFilterStart != null) "Date Filtered" else "Filter Date") }
                )

                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRoleFilter == role,
                        onClick = { selectedRoleFilter = role },
                        label = { Text(role.name) }
                    )
                }

                AccountStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status.name) }
                    )
                }
            }

            IconButton(onClick = { sortDescending = !sortDescending }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Sort by Registration Date",
                    tint = if (sortDescending) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Empty state feedback for no matches
        if (filteredAndSorted.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank() && selectedRoleFilter == null && selectedStatusFilter == null && dateFilterStart == null)
                    "No profiles registered in the platform."
                else
                    "No profiles match search/filter criteria.",
                modifier = Modifier.padding(16.dp)
            )
        }

        // Virtual scrolling for large user bases
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredAndSorted) { user ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    // Navigation on click[cite: 12]
                    modifier = Modifier.fillMaxWidth().clickable { onSelectProfile(user.deviceId) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Key identity details[cite: 12]
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.name.ifBlank { "Unknown User" }, fontWeight = FontWeight.Bold)

                            val statusColor = when(user.accountStatus) {
                                AccountStatus.ACTIVE -> Color(0xFF2E7D32)
                                AccountStatus.INACTIVE -> Color(0xFF757575)
                                AccountStatus.SUSPENDED -> Color(0xFFD32F2F)
                            }
                            Text(user.accountStatus.name, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                        Text("ID: ${user.deviceId}")
                        if (user.email.isNotBlank()) Text("Email: ${user.email}")
                        Text("Role: ${user.role.name} · Registered: ${formatDateMillis(user.registrationDateMillis)}")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap for detailed account overview →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Date Range Picker Dialog for filtering by creation date
    if (showDateFilterDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = dateFilterStart,
            initialSelectedEndDateMillis = dateFilterEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDateFilterDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dateFilterStart = dateState.selectedStartDateMillis
                    dateFilterEnd = dateState.selectedEndDateMillis?.plus(86399999) // Add 24h to include the full end day
                    showDateFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dateFilterStart = null
                    dateFilterEnd = null
                    showDateFilterDialog = false
                }) { Text("Clear") }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}

// =========================================================================
// POLICY AND GUIDELINES SCREEN
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyAndGuidelinesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Policy & Guidelines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFEDE8F8))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicyCard(
                title = "1. Community Guidelines & General Policy",
                content = "Welcome to Solace Campsite. Our platform connects outdoor enthusiasts with premium public and private campsites. All users are expected to treat nature, campsite owners, and fellow entrants with respect. Harassment, fraudulent bookings, or misuse of promotional codes will result in immediate suspension."
            )

            PolicyCard(
                title = "2. Campsite Usage Rules",
                content = "• Quiet Hours: Strictly enforced between 11:00 PM and 7:00 AM.\n" +
                        "• Campfires: Permitted only in designated fire pits. Never leave a fire unattended.\n" +
                        "• Wildlife Safety: Keep all food in bear-proof lockers or secure vehicles. Do not feed wildlife.\n" +
                        "• Cleanliness: Pack out all garbage. Leave your campsite as clean as or cleaner than you found it."
            )

            PolicyCard(
                title = "3. Cancellation & Refund Policies",
                content = "• Standard Cancellation: Full refund if cancelled at least 7 days prior to check-in. 50% refund if cancelled 3 to 6 days prior.\n" +
                        "• Weather & Fire Bans: 100% full refund issued automatically in the event of an official government fire ban, forest fire warning, or severe weather emergency affecting your dates."
            )

            PolicyCard(
                title = "4. Lottery Terms & Conditions",
                content = "• Waitlist Entry: Entrants may join active campsite lotteries during the specified registration window.\n" +
                        "• Winner Selection: Winners are drawn randomly and notified via in-app notifications.\n" +
                        "• Acceptance Window: Winners must accept their reservation invitation within 48 hours, or the spot will be offered to runner-up entrants.\n" +
                        "• Capacity Limits: Some lotteries enforce waitlist capacity caps to ensure fair access."
            )
        }
    }
}

@Composable
fun PolicyCard(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}