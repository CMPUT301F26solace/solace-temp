/**
 * EntrantReservationsScreen.kt
 *
 * Purpose: Screen displaying a list of active and upcoming campsite reservations
 * for the logged-in entrant, including empty state handling (US 01.06.01).
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrantReservationsScreen(
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val profile by repository.currentProfile.collectAsState()
    val reservations by repository.reservations.collectAsState()

    // Filter reservations for logged-in user with active status
    val userReservations = reservations.filter {
        it.entrantId == profile.deviceId && it.status == ReservationStatus.ACTIVE
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Active Reservations", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (userReservations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active reservations found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userReservations) { reservation ->
                    ReservationCard(reservation = reservation)
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    modifier: Modifier = Modifier
) {
    val dateRange = if (reservation.bookedDates.isNotEmpty()) {
        "${reservation.bookedDates.first()} - ${reservation.bookedDates.last()}"
    } else {
        "Dates not specified"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.campsiteName.ifEmpty { "Campsite Reservation" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = reservation.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = "Dates: $dateRange",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Confirmation ID: ${reservation.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}