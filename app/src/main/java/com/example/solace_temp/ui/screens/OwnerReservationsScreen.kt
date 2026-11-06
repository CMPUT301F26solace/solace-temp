package com.example.solace_temp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Reservation

/** Owner's active and upcoming confirmed campsite reservations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerReservationsScreen(repository: AppRepository, onBack: () -> Unit) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val mine = activeOwnerReservations(profile.deviceId, campsites, reservations)
    var selected by remember { mutableStateOf<Reservation?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Active Reservations") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .testTag("owner_reservations"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Current and upcoming bookings", style = MaterialTheme.typography.titleMedium)
            if (mine.isEmpty()) {
                Text("No active reservations for your campsites.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(mine, key = { it.id }) { booking ->
                        val site = campsites.firstOrNull { it.id == booking.campsiteId }
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .testTag("reservation_${booking.id}")
                                .clickable { selected = booking },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(site?.name ?: booking.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("Entrant ID: ${booking.entrantId}")
                                Text("Booked dates: ${reservationDateRange(booking)}")
                                Text("Status: Confirmed")
                                Text("Tap for details", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { booking ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Reservation Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Campsite: ${campsites.firstOrNull { it.id == booking.campsiteId }?.name ?: booking.campsiteName}")
                    Text("Entrant ID: ${booking.entrantId}")
                    Text("Booked dates: ${reservationDateRange(booking)}")
                    Text("Status: ${booking.status}")
                    Text("Reservation ID: ${booking.id}")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}
