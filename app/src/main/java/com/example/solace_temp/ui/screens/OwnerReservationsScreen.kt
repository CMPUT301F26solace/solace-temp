/**
 * OwnerReservationsScreen.kt
 * 
 * Purpose: Composable screen allowing campsite owners to view, inspect, and manage active and upcoming
 * confirmed reservations across their registered campsites.
 * 
 * Issues / Outstanding Issues:
 * - Reservation cancellation and status modifications by owners are currently handled via admin intervention.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.Neutral700
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.OwnerBackground
import com.example.solace_temp.ui.theme.SolaceCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceCardColors
import com.example.solace_temp.ui.theme.solaceCardElevation

/** Owner's active and upcoming confirmed campsite reservations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerReservationsScreen(repository: AppRepository, onBack: () -> Unit) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val mine = activeOwnerReservations(profile.deviceId, campsites, reservations)
    var selected by remember { mutableStateOf<Reservation?>(null) }

    Scaffold(
        containerColor = OwnerBackground,
        topBar = {
            TopAppBar(
                title = { Text("Active Reservations", color = Neutral900) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = Neutral900),
                        modifier = Modifier.testTag("owner_reservations_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OwnerBackground,
                    titleContentColor = Neutral900,
                    navigationIconContentColor = Neutral900
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OwnerBackground)
                .padding(padding)
                .padding(16.dp)
                .testTag("owner_reservations"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Current and upcoming bookings",
                style = MaterialTheme.typography.titleMedium,
                color = Neutral900
            )
            if (mine.isEmpty()) {
                Text(
                    "No active reservations for your campsites.",
                    color = Neutral700
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(mine, key = { it.id }) { booking ->
                        val site = campsites.firstOrNull { it.id == booking.campsiteId }
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .testTag("reservation_${booking.id}")
                                .clickable { selected = booking },
                            shape = SolaceCardShape,
                            colors = solaceCardColors(),
                            border = solaceCardBorder(),
                            elevation = solaceCardElevation()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    site?.name ?: booking.campsiteName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                                Text("Entrant ID: ${booking.entrantId}", color = Neutral700)
                                Text("Booked dates: ${reservationDateRange(booking)}", color = Neutral700)
                                Text("Status: Confirmed", color = Neutral700)
                                Text(
                                    "Tap for details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolaceRed
                                )
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
            containerColor = CardSurface,
            titleContentColor = Neutral900,
            textContentColor = Neutral700,
            title = { Text("Reservation Details", color = Neutral900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Campsite: ${campsites.firstOrNull { it.id == booking.campsiteId }?.name ?: booking.campsiteName}")
                    Text("Entrant ID: ${booking.entrantId}")
                    Text("Booked dates: ${reservationDateRange(booking)}")
                    Text("Status: ${booking.status}")
                    Text("Reservation ID: ${booking.id}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selected = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = SolaceRed)
                ) { Text("Close") }
            }
        )
    }
}
