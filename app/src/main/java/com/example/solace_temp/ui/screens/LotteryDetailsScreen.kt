/**
 * LotteryDetailsScreen.kt
 * 
 * Purpose: Composable screen displaying comprehensive details of a lottery, owner controls, manual draw/redraw triggers, and testing fast-forwarding.
 * 
 * Issues / Outstanding Issues:
 * - Manual redraw is enabled only when unfulfilled winner slots remain after initial draw.
 */
package com.example.solace_temp.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.example.solace_temp.models.UserRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.ReservationStatus
import kotlinx.coroutines.delay

private fun formatLotteryCountdown(targetMillis: Long, nowMillis: Long): String {
    val left = (targetMillis - nowMillis).coerceAtLeast(0L)
    val days = left / 86_400_000L
    val hours = (left / 3_600_000L) % 24
    val minutes = (left / 60_000L) % 60
    val seconds = (left / 1_000L) % 60
    return if (days > 0) {
        "${days}d ${hours}h ${minutes}m"
    } else if (hours > 0) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}

/**
 * Fast-forwards the lottery registration schedule for testing purposes.
 *
 * Sets the lottery registration end timestamp to 1 minute from [nowMillis] so campsite
 * owners can immediately test the drawing workflow. Simultaneously updates the registration
 * date display strings to reflect the new schedule, while leaving the campsite booking
 * availability dates intact as originally configured.
 *
 * @param lottery The [Lottery] to be updated for expedited testing.
 * @param nowMillis The current timestamp in milliseconds, defaults to [System.currentTimeMillis].
 * @return An updated [Lottery] instance with accelerated registration schedule and preserved booking dates.
 */
fun fastForwardLotteryForTesting(
    lottery: Lottery,
    nowMillis: Long = System.currentTimeMillis()
): Lottery {
    val newEndMillis = nowMillis + 60_000L
    val newStartMillis = if ((lottery.registrationStartMillis ?: 0L) > nowMillis) {
        nowMillis - 60_000L
    } else {
        lottery.registrationStartMillis ?: (nowMillis - 60_000L)
    }

    return lottery.copy(
        registrationStartMillis = newStartMillis,
        registrationEndMillis = newEndMillis,
        registrationStart = formatDateMillis(newStartMillis),
        registrationEnd = formatDateMillis(newEndMillis),
        isDrawn = false
    )
}

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
                    delay(1_000)
                    countdownNow = System.currentTimeMillis()
                }
            }
            LaunchedEffect(lottery.id, countdownNow) {
                val end = lottery.registrationEndMillis
                if (end != null && end <= countdownNow && !lottery.isDrawn) {
                    repository.drawLotteryWinners(lottery.id)
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

                        val isOwnerOrAdmin = lottery.ownerId == profile.deviceId || (profile.isAdmin && profile.role == UserRole.ADMIN)
                        val isPastDeadline = (lottery.registrationEndMillis ?: Long.MAX_VALUE) <= countdownNow

                        if (isOwnerOrAdmin && isPastDeadline && !lottery.isDrawn) {
                            Button(
                                onClick = { repository.drawLotteryWinners(lottery.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Draw Winners", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Draw Winners Now")
                            }
                        }

                        if (isOwnerOrAdmin) {
                            OutlinedButton(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val updated = fastForwardLotteryForTesting(lottery, now)
                                    repository.updateLottery(updated)
                                    countdownNow = System.currentTimeMillis()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = "Test Timer", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("End Lottery in 1 Min (Test)")
                            }
                        }

                        if (lottery.ownerId == profile.deviceId || (profile.isAdmin && profile.role == UserRole.ADMIN)) {
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
