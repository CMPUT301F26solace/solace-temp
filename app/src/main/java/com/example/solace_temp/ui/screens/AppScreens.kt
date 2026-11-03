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
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
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
    if (lottery.isDrawn) return "Finished"
    val now = System.currentTimeMillis()
    val regStart = lottery.registrationStartMillis ?: 0L
    val regEnd = lottery.registrationEndMillis ?: Long.MAX_VALUE
    val bookingStart = lottery.bookingStartMillis ?: campsite?.bookingStartDateMillis ?: regEnd
    val bookingEnd = lottery.bookingEndMillis ?: campsite?.bookingEndDateMillis ?: Long.MAX_VALUE

    return when {
        now < regStart -> "Upcoming"
        now in regStart..regEnd -> "Active"
        now in bookingStart..bookingEnd || now in (regEnd + 1)..bookingEnd -> "Booking"
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

    val qrBitmap = remember(campsite.qrCodePayload) {
        QrCodeGeneratorUtil.generateQrBitmap(campsite.qrCodePayload)
    }
    val activeLottery = lotteries.find { it.campsiteIds.contains(campsite.id) && getLotteryStatus(it, campsite) != "Finished" && getLotteryStatus(it, campsite) != "N/A" }
    val isInWaitlist = activeLottery?.waitingListEntrantIds?.contains(profile.deviceId) == true

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
                        if (campsite.imageUrl != null) {
                            // In a real app we'd use AsyncImage here, placeholder for now
                            Icon(Icons.Default.Image, contentDescription = "Campsite Image", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
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
                        Text(campsite.description, style = MaterialTheme.typography.bodyMedium)

                        // Amenities
                        if (campsite.amenities.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            campsite.amenities.forEach { amenity ->
                                Text("• $amenity", style = MaterialTheme.typography.bodyMedium)
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
                
                if (activeLottery != null) {
                    val lotteryStatus = getLotteryStatus(activeLottery, campsite)
                    val isRegistrationOpen = lotteryStatus == "Active"

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val regStart = activeLottery.registrationStartMillis ?: 0L
                            val regEnd = activeLottery.registrationEndMillis ?: Long.MAX_VALUE
                            if (now < regStart) {
                                entrantFeedback = "Error: Registration has not opened yet. Opens on ${activeLottery.registrationStart.ifBlank { formatDateMillis(regStart) }}."
                            } else if (now > regEnd) {
                                entrantFeedback = "Error: Registration period has closed."
                            } else {
                                repository.joinWaitingList(activeLottery.id, profile.deviceId)
                                entrantFeedback = "Successfully joined waiting list!"
                            }
                        },
                        enabled = isRegistrationOpen && !isInWaitlist,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isInWaitlist) "Already on Waiting List" else "+ Join Waiting List")
                    }

                    OutlinedButton(
                        onClick = { repository.leaveWaitingList(activeLottery.id, profile.deviceId) },
                        enabled = isInWaitlist,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Leave Waiting List")
                    }
                }
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
                        var showCreateLotteryDialog by remember { mutableStateOf(false) }
                        var ownerLotteryFeedback by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FilledIconButton(onClick = { /* TODO */ }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
                                    .background(Color(0xFFE3E8EF), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (site.imageUrl != null) {
                                    Icon(Icons.Default.Image, contentDescription = "Campsite Image", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
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
                                Text(
                                    text = if (activeLottery != null) "Lottery: ${activeLottery.name}" else "Lottery Status",
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
                                maxOccupancy = parsedOccupancy
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
fun ProfileAndRoleScreen(repository: AppRepository) {
    val profile by repository.currentProfile.collectAsState()
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone) }
    var receiveNotifs by remember(profile.receiveNotifications) { mutableStateOf(profile.receiveNotifications) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Role Switcher Card (For Demoing Entrant / Owner / Admin seamlessly)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Active Role Mode (US 03.11.01)", fontWeight = FontWeight.Bold)
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

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (Optional)") }, modifier = Modifier.fillMaxWidth())

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
                        repository.updateProfile(
                            profile.copy(name = name, email = email, phone = phone, receiveNotifications = receiveNotifs)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                Text(
                    text = "📱 Device ID (US 01.09.01): ${profile.deviceId}",
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

    // Filter lotteries for campsites owned by this user (or all if admin)
    val ownedLotteries = lotteries.filter { lottery ->
        profile.isAdmin ||
        lottery.ownerId == profile.deviceId ||
        campsites.any { campsite -> lottery.campsiteIds.contains(campsite.id) && campsite.ownerId == profile.deviceId }
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
    onSelectCampsite: (String) -> Unit
) {
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
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
                    }
                }

                // Placeholder card for future lottery draw controls and management features
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Lottery Sampling & Draw Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Advanced sampling algorithms, winner draw triggers, and notification management will appear here in future updates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// CREATE LOTTERY DIALOG
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
    var regStartMillis by remember { mutableStateOf<Long?>(null) }
    var regEndMillis by remember { mutableStateOf<Long?>(null) }
    var bookingStartMillis by remember { mutableStateOf<Long?>(null) }
    var bookingEndMillis by remember { mutableStateOf<Long?>(null) }
    var showRegDateDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter { it.ownerId == profile.deviceId }.ifEmpty { listOf(campsite) }
    val activeCampsites = ownerCampsites.filter { selectedCampsiteIds.contains(it.id) }
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

                Text("Assigned Campsite(s):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                ownerCampsites.forEach { site ->
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