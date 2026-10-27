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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    onBack: () -> Unit
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
    val activeLottery = lotteries.find { it.campsiteIds.contains(campsite.id) }
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

                        // Active Booking Period Display for Owner
                        Spacer(Modifier.height(16.dp))
                        Text("Active Booking Period", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatDateMillis(campsite.bookingStartDateMillis)} to ${formatDateMillis(campsite.bookingEndDateMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
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
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { repository.joinWaitingList(activeLottery.id, profile.deviceId) },
                        enabled = !isInWaitlist,
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
                                FilledIconButton(onClick = { /* TODO */ }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
    
    // Filter to only include campsites owned by this user
    val myCampsites = campsites.filter { it.ownerId == profile.deviceId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
    ) {
        Text("My Registered Campsites", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (myCampsites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(myCampsites) { site ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCampsite(site.id) }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFE3E8EF), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (site.imageUrl != null) {
                                    // Normally an AsyncImage, just a placeholder icon here
                                    Icon(Icons.Default.Image, contentDescription = "Thumbnail", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = "No Image", tint = Color.Gray)
                                }
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("$${site.pricePerNight.toInt()}/night", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("📍 ${site.location.locationName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                Text("Max ${site.maxOccupancy} guests", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
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
    var parkLocation by remember { mutableStateOf("Jasper National Park, AB") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("65.0") }
    var maxOccupancy by remember { mutableStateOf("8") }

    var bookingStartMillis by remember { mutableStateOf<Long?>(null) }
    var bookingEndMillis by remember { mutableStateOf<Long?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var successCreatedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(successCreatedId) {
        if (successCreatedId != null) {
            delay(1500)
            onCreated(successCreatedId!!)
        }
    }

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
            "Register your campsite, set lottery rules, and auto-generate a unique QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        val textFieldShape = RoundedCornerShape(16.dp)
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Campsite Name") }, modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)
        OutlinedTextField(value = parkLocation, onValueChange = { parkLocation = it }, label = { Text("Location (e.g., Banff, Jasper)") }, modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description & Amenities") }, modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)
        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price per Night ($)") }, modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)
        OutlinedTextField(value = maxOccupancy, onValueChange = { maxOccupancy = it }, label = { Text("Maximum Occupancy") }, modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)

        Spacer(Modifier.height(8.dp))
        Text("Booking Period: ${formatDateMillis(bookingStartMillis)} to ${formatDateMillis(bookingEndMillis)}", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { showDateDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Set Booking Dates")
        }

        if (showDateDialog) {
            val dateState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = bookingStartMillis,
                initialSelectedEndDateMillis = bookingEndMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDateDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        bookingStartMillis = dateState.selectedStartDateMillis
                        bookingEndMillis = dateState.selectedEndDateMillis
                        showDateDialog = false
                    }) { Text("Save Dates") }
                },
                dismissButton = { TextButton(onClick = { showDateDialog = false }) { Text("Cancel") } }
            ) {
                DateRangePicker(state = dateState, modifier = Modifier.weight(1f))
            }
        }

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
                if (bookingStartMillis == null || bookingEndMillis == null) {
                    feedbackIsError = true
                    feedbackMsg = "Error: Please define a booking period."
                    return@Button
                }
                if (bookingEndMillis!! <= bookingStartMillis!!) {
                    feedbackIsError = true
                    feedbackMsg = "Error: End date must be strictly after the start date."
                    return@Button
                }
                val today = System.currentTimeMillis() - 86400000
                if (bookingStartMillis!! < today) {
                    feedbackIsError = true
                    feedbackMsg = "Error: Booking start date must be in the future."
                    return@Button
                }

                if (name.isNotBlank()) {
                    feedbackIsError = false
                    feedbackMsg = "Campsite saved successfully with booking period!"
                    val created = repository.createCampsite(
                        Campsite(
                            ownerId = profile.deviceId,
                            name = name,
                            description = description.ifBlank { "Scenic Alberta campsite." },
                            location = Geolocation(locationName = parkLocation),
                            pricePerNight = price.toDoubleOrNull() ?: 50.0,
                            maxOccupancy = maxOccupancy.toIntOrNull() ?: 4,
                            bookingStartDateMillis = bookingStartMillis,
                            bookingEndDateMillis = bookingEndMillis
                        )
                    )
                    successCreatedId = created.id
                } else {
                    feedbackIsError = true
                    feedbackMsg = "Error: Campsite name cannot be empty."
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Generate QR Code & Publish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
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