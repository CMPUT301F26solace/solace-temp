package com.example.solace_temp.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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

// =========================================================================
// YOUR USER STORY (US 02.02.01): Unique QR Code Generation & Share Helper
// =========================================================================
object QrCodeGeneratorUtil {
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

    fun shareQrPayload(context: Context, campsite: Campsite) {
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

// =========================================================================
// CAMPSITE DETAILS SCREEN (Shared by Entrant & Owner - Includes US 02.02.01)
// =========================================================================
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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Campsite not found.")
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EFFC))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = campsite.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${campsite.pricePerNight.toInt()}/night",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("📍 ${campsite.location.locationName} · Max ${campsite.maxOccupancy} guests · ★ ${campsite.rating}")
                    Spacer(Modifier.height(8.dp))
                    Text(campsite.description)
                }
            }

            // ENTRANT ACTIONS: Join / Leave Waiting List (US 01.01.06, US 01.02.01)
            if (profile.role == UserRole.ENTRANT && activeLottery != null) {
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

            // CAMPSITE OWNER QR CODE SECTION (Your Story: US 02.02.01)
            if (profile.role == UserRole.CAMPSITE_OWNER || profile.role == UserRole.ADMIN) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Promotional QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Unique Link: ${campsite.qrCodePayload}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Campsite QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { QrCodeGeneratorUtil.shareQrPayload(context, campsite) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { site ->
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
                        Text("📍 ${site.location.locationName} · Max ${site.maxOccupancy} guests · ★ ${site.rating}")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to view details & QR code →", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// =========================================================================
// CREATE CAMPSITE SCREEN (Matches Organizer Mockup #5 - US 02.01.01 & 02.02.01)
// =========================================================================
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Campsite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Register your campsite, set lottery rules, and auto-generate a unique QR code.")

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Campsite Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = parkLocation, onValueChange = { parkLocation = it }, label = { Text("Location (e.g., Banff, Jasper)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description & Amenities") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price per Night ($)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = maxOccupancy, onValueChange = { maxOccupancy = it }, label = { Text("Maximum Occupancy") }, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val created = repository.createCampsite(
                        Campsite(
                            ownerId = profile.deviceId,
                            name = name,
                            description = description.ifBlank { "Scenic Alberta campsite." },
                            location = Geolocation(locationName = parkLocation),
                            pricePerNight = price.toDoubleOrNull() ?: 50.0,
                            maxOccupancy = maxOccupancy.toIntOrNull() ?: 4
                        )
                    )
                    onCreated(created.id)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate QR Code & Publish")
        }
    }
}

// =========================================================================
// PROFILE & ROLE SWITCHER SCREEN (Matches Mockup #2 + US 01.09.01 + US 03.11.01)
// =========================================================================
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