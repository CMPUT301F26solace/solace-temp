package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** An owner-facing, live view of a lottery's current entrants. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerWaitlistScreen(
    lotteryId: String,
    repository: AppRepository,
    onBack: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val loadingError by repository.lotteryLoadError.collectAsState()
    val lottery = lotteries.firstOrNull { it.id == lotteryId }
    val ownedCampsiteIds = campsites.filter { it.ownerId == profile.deviceId }
        .map { it.id }.toSet()
    val isOwner = lottery?.let { selected ->
        selected.ownerId == profile.deviceId &&
            selected.campsiteIds.isNotEmpty() &&
            selected.campsiteIds.all { it in ownedCampsiteIds }
    } ?: false

    Scaffold(topBar = {
        TopAppBar(title = { Text("Lottery Waitlist") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                loadingError != null -> Text(
                    "Could not load the waitlist: $loadingError",
                    color = MaterialTheme.colorScheme.error
                )
                lottery == null -> Text("Lottery not found or still loading.")
                !isOwner -> Text("Only the campsite owner can view this waitlist.")
                else -> {
                    Text(lottery.name, style = MaterialTheme.typography.headlineSmall)
                    Text("Current entrants: ${lottery.waitingListEntrantIds.size}")
                    Text("Capacity: ${lottery.waitlistCapacity?.toString() ?: "Unlimited"}")
                    if (lottery.waitingListEntrantIds.isEmpty()) {
                        Text("No entrants have joined the waitlist yet.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(lottery.waitingListEntrantIds, key = { it }) { entrantId ->
                                val confirmed = reservations.any { booking ->
                                    booking.entrantId == entrantId &&
                                    booking.campsiteId in lottery.campsiteIds &&
                                    booking.status.name == "ACTIVE"
                                }
                                val status = when {
                                    confirmed -> "Confirmed"
                                    entrantId in lottery.invitedWinnerIds -> "Selected"
                                    else -> "Waiting"
                                }
                                val joined = lottery.waitlistJoinedAtMillis[entrantId]
                                val joinedLabel = joined?.let {
                                    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                                        .format(Date(it))
                                } ?: "Not recorded (older entry)"
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Entrant ID: $entrantId")
                                        Text("Joined: $joinedLabel")
                                        Text("Status: $status")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
