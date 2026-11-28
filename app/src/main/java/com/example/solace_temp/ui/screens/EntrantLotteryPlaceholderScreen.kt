/**
 * EntrantLotteryPlaceholderScreen.kt
 * 
 * Purpose: Composable screen for entrants to view lottery details, manage waiting list participation, and confirm bookings upon winning.
 * 
 * Issues / Outstanding Issues:
 * - Winning entrant date picker requires selecting dates strictly within the allowed booking window.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.ui.components.ScratchRevealCanvas
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.Neutral300
import com.example.solace_temp.ui.theme.Neutral50
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusActiveBg
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.validation.WaitingListRules
import kotlinx.coroutines.delay

// =========================================================================
// ENTRANT LOTTERY DETAILS PLACEHOLDER SCREEN
// =========================================================================
/**
 * Composable screen for entrants to view lottery details, manage waiting list
 * participation, and select dates to confirm bookings when selected as a winner.
 *
 * @param lotteryId The unique ID of the lottery being inspected.
 * @param repository The repository providing access to lotteries, campsites, and reservations.
 * @param onBack Callback invoked when navigating back to the campsite details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrantLotteryPlaceholderScreen(
    lotteryId: String,
    repository: AppRepository,
    onBack: () -> Unit
) {
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val lottery = lotteries.find { it.id == lotteryId }

    val entrantId = profile.deviceId
    val isWinner = entrantId in (lottery?.invitedWinnerIds ?: emptyList())
    val storedMembership = entrantId in (lottery?.waitingListEntrantIds ?: emptyList())
    var confirmedMembership by remember(lotteryId) { mutableStateOf<Boolean?>(null) }
    val isMember = confirmedMembership ?: storedMembership

    var isSavingWaitlist by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var waitlistFeedback by remember { mutableStateOf<String?>(null) }
    var waitlistFailed by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var showBookingDialog by remember { mutableStateOf(false) }
    var bookingFeedback by remember { mutableStateOf("") }

    LaunchedEffect(storedMembership, confirmedMembership) {
        if (storedMembership == confirmedMembership) {
            confirmedMembership = null
        }
    }

    LaunchedEffect(lotteryId) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    LaunchedEffect(lottery?.id, nowMillis) {
        val lot = lottery ?: return@LaunchedEffect
        val end = lot.registrationEndMillis
        if (end != null && end <= nowMillis && !lot.isDrawn) {
            repository.drawLotteryWinners(lot.id)
        }
    }

    val joinError = lottery?.let {
        WaitingListRules.joinError(it, entrantId, nowMillis)
    }

    fun finishWaitlistAction(result: Result<Unit>, joined: Boolean) {
        isSavingWaitlist = false
        waitlistFailed = result.isFailure
        waitlistFeedback = result.fold(
            onSuccess = {
                confirmedMembership = joined
                if (joined) "Joined the waiting list." else "Left the waiting list."
            },
            onFailure = WaitingListRules::failureMessage
        )
    }

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
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lottery details could not be found.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Back to Campsite") }
                }
            }
        } else {
            val associatedCampsites = campsites.filter { lottery.campsiteIds.contains(it.id) }
            val primaryCampsite = associatedCampsites.firstOrNull()
            val campsiteNames = associatedCampsites.joinToString(", ") { it.name }.ifBlank { "Assigned Campsite" }
            val status = getLotteryStatus(lottery, primaryCampsite)
            val isDrawnOrBooking = lottery.isDrawn || status == "Booking"
            val activeBooking = reservations.find {
                it.campsiteId in lottery.campsiteIds && it.entrantId == entrantId && it.status == ReservationStatus.ACTIVE
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
                // Lottery Information Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
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
                                modifier = Modifier.weight(1f)
                            )
                            val badgeColor = when (status) {
                                "Active" -> Color(0xFF2E7D32)
                                "Booking" -> Color(0xFF7B1FA2)
                                "Upcoming" -> Color(0xFF1976D2)
                                else -> Color(0xFF555555)
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

                        Text("📍 $campsiteNames", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(
                            "Registration Window: ${lottery.registrationStart} to ${lottery.registrationEnd}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Booking Window: ${lottery.bookingStart} to ${lottery.bookingEnd}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Max Winners: ${lottery.maxWinners} · Waitlist capacity: ${lottery.waitlistCapacity?.toString() ?: "Unlimited"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dynamic Entrant Status & Interaction Section
                if (isWinner) {
                    var isWinnerPassRevealed by rememberSaveable(lottery.id) { mutableStateOf(false) }

                    // WINNER VIEW: Shows winning banner, scratch-reveal ticket, and "Select Dates & Book" button
                    Card(
                        shape = RoundedCornerShape(SolaceTokens.RadiusXL),
                        colors = CardDefaults.cardColors(
                            containerColor = CardSurface
                        ),
                        border = solaceCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(SolaceTokens.SpaceXL),
                            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                        ) {
                            Text(
                                text = "🎉 You Won the Lottery!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                            Text(
                                text = "You have been selected in the draw! Scratch your winner pass below or tap instant reveal, then confirm your camping dates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral600
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp)
                                    .clip(SolaceInnerCardShape)
                                    .background(StatusActiveBg)
                                    .border(
                                        SolaceTokens.HairlineWidth,
                                        StatusActiveGreen.copy(alpha = 0.25f),
                                        SolaceInnerCardShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(SolaceTokens.SpaceMD),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    Text(
                                        text = "OFFICIAL WINNER INVITATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusActiveGreen
                                    )
                                    Text(
                                        text = campsiteNames,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Neutral900,
                                        textAlign = TextAlign.Center
                                    )
                                    TabularText(
                                        text = "Booking Window: ${lottery.bookingStart} – ${lottery.bookingEnd}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Neutral600
                                    )
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !isWinnerPassRevealed,
                                    enter = EnterTransition.None,
                                    exit = fadeOut(animationSpec = tween(220)),
                                    modifier = Modifier.matchParentSize()
                                ) {
                                    ScratchRevealCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        overlayColor = Neutral300,
                                        strokeWidth = 64f,
                                        revealThreshold = 0.45f,
                                        onFullyRevealed = { isWinnerPassRevealed = true }
                                    )
                                }
                            }

                            if (!isWinnerPassRevealed) {
                                OutlinedButton(
                                    onClick = { isWinnerPassRevealed = true },
                                    shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                                    border = solaceCardBorder(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Neutral50,
                                        contentColor = Neutral900
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .testTag("btn_entrant_reveal_winner_instantly")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                    Text("Reveal Pass Instantly", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            if (activeBooking != null) {
                                Surface(
                                    shape = SolaceInnerCardShape,
                                    color = Neutral50,
                                    border = solaceCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(SolaceTokens.SpaceMD)) {
                                        Text(
                                            "✓ Confirmed Reservation",
                                            fontWeight = FontWeight.Bold,
                                            color = StatusActiveGreen
                                        )
                                        TabularText(
                                            text = "Booked Nights: ${activeBooking.bookedDates.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showBookingDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolaceRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Select Dates & Book", style = MaterialTheme.typography.labelLarge)
                            }

                            if (bookingFeedback.isNotBlank()) {
                                Text(
                                    text = bookingFeedback,
                                    color = if (bookingFeedback.startsWith("Error")) MaterialTheme.colorScheme.error else StatusActiveGreen,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (isDrawnOrBooking) {
                    // DRAWN / BOOKING VIEW (NOT SELECTED): Explains they still have a chance on waitlist
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Drawing Complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "The lottery drawing has taken place. You have not been selected yet, but you might still have a chance of winning if invited entrants decline their invitation! You can remain on the waiting list for backup redraws or leave at any time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (isMember) "Status: Enrolled on Waiting List" else "Status: Not on Waiting List",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (isMember) {
                                OutlinedButton(
                                    onClick = { showLeaveDialog = true },
                                    enabled = !isSavingWaitlist,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Leave Waiting List")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (!isSavingWaitlist) {
                                            isSavingWaitlist = true
                                            waitlistFeedback = null
                                            repository.joinWaitingListWithResult(lottery.id, entrantId) {
                                                finishWaitlistAction(it, joined = true)
                                            }
                                        }
                                    },
                                    enabled = !isSavingWaitlist && joinError == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSavingWaitlist) "Updating..." else "Join Waiting List")
                                }
                            }

                            waitlistFeedback?.let { msg ->
                                Text(
                                    text = msg,
                                    color = if (waitlistFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    // ACTIVE / REGISTRATION OPEN VIEW: Standard waitlist entry controls
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Waiting List Registration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Join the waiting list during the registration window to enter the randomized drawing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (isMember) {
                                    "✓ You are on this waiting list."
                                } else {
                                    joinError ?: "Accepting entries"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (isMember) {
                                OutlinedButton(
                                    onClick = { showLeaveDialog = true },
                                    enabled = !isSavingWaitlist,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Leave Waiting List")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (!isSavingWaitlist) {
                                            isSavingWaitlist = true
                                            waitlistFeedback = null
                                            repository.joinWaitingListWithResult(lottery.id, entrantId) {
                                                finishWaitlistAction(it, joined = true)
                                            }
                                        }
                                    },
                                    enabled = !isSavingWaitlist && joinError == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isSavingWaitlist) "Updating..." else "Join Waiting List")
                                }
                            }

                            waitlistFeedback?.let { msg ->
                                Text(
                                    text = msg,
                                    color = if (waitlistFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Date Range Picker Dialog for Winners
                if (showBookingDialog) {
                    val dateState = rememberDateRangePickerState()
                    val targetCampsiteId = primaryCampsite?.id ?: lottery.campsiteIds.firstOrNull().orEmpty()
                    val campStart = primaryCampsite?.bookingStartDateMillis ?: lottery.bookingStartMillis ?: 0L
                    val campEnd = primaryCampsite?.bookingEndDateMillis ?: lottery.bookingEndMillis ?: Long.MAX_VALUE

                    DatePickerDialog(
                        onDismissRequest = { showBookingDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selStart = dateState.selectedStartDateMillis
                                val selEnd = dateState.selectedEndDateMillis

                                if (selStart == null || selEnd == null) {
                                    bookingFeedback = "Error: Please select both a check-in and check-out date."
                                } else if (selStart < campStart || selEnd > campEnd) {
                                    bookingFeedback = "Error: Selected dates are outside allowed booking window of ${formatDateMillis(campStart)} to ${formatDateMillis(campEnd)}."
                                } else {
                                    repository.confirmBooking(targetCampsiteId, lottery.id, selStart, selEnd) { err ->
                                        if (err != null) {
                                            bookingFeedback = "Error: $err"
                                        } else {
                                            bookingFeedback = "Success! Your reservation has been confirmed."
                                        }
                                    }
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

                // Leave Confirmation Dialog
                if (showLeaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showLeaveDialog = false },
                        title = { Text("Leave waiting list?") },
                        text = {
                            Text("You will no longer be waiting for a place in this lottery draw.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showLeaveDialog = false
                                    if (!isSavingWaitlist) {
                                        isSavingWaitlist = true
                                        waitlistFeedback = null
                                        repository.leaveWaitingList(lottery.id, entrantId) {
                                            finishWaitlistAction(it, joined = false)
                                        }
                                    }
                                }
                            ) {
                                Text("Leave")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLeaveDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
