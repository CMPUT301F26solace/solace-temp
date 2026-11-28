/**
 * EntrantReservationsScreen.kt
 *
 * Purpose: Screen displaying active and cancelled campsite reservations for the
 * logged-in entrant (US 01.06.01), including reservation cancellation, confirmation
 * notification status feedback, and downloadable CSV export.
 */
package com.example.solace_temp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.CancellationCsvExporter
import com.example.solace_temp.models.CancellationOutcome
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.isUpcomingAndCancellable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrantReservationsScreen(
    repository: AppRepository,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val profile by repository.currentProfile.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val context = LocalContext.current

    // Toggle between Active reservations (default for US 01.06.01) and Cancelled history for CSV downloads
    var showCancelled by remember { mutableStateOf(false) }

    // State controlling cancellation confirmation and post-cancellation feedback dialogs
    var reservationPendingCancel by remember { mutableStateOf<Reservation?>(null) }
    var isCancelling by remember { mutableStateOf(false) }
    var completedCancellation by remember { mutableStateOf<CancellationOutcome?>(null) }

    // Holds the reservation whose CSV is currently being saved via the Android file picker
    var reservationPendingCsvExport by remember { mutableStateOf<Reservation?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Standard Android Storage Access Framework intent to export the CSV to local device storage
    val csvCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { destinationUri: Uri? ->
        val target = reservationPendingCsvExport
        reservationPendingCsvExport = null

        // User dismissed the system file picker without choosing a location
        if (destinationUri == null || target == null) return@rememberLauncherForActivityResult

        val writeResult = CancellationCsvExporter.writeToUri(context, destinationUri, target)
        coroutineScope.launch {
            writeResult.onSuccess {
                snackbarHostState.showSnackbar(
                    message = "Cancellation record exported as CSV."
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    message = "Failed to export CSV file: ${error.localizedMessage ?: "Storage write error."}"
                )
            }
        }
    }

    /** Launches the Android file intent safely and reports an error if no file handler is available. */
    fun triggerCsvExport(reservation: Reservation) {
        reservationPendingCsvExport = reservation
        val fileName = CancellationCsvExporter.suggestedFileName(reservation)
        runCatching {
            csvCreateLauncher.launch(fileName)
        }.onFailure { error ->
            reservationPendingCsvExport = null
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Unable to open device storage for CSV export: ${error.localizedMessage ?: "No file picker available."}"
                )
            }
        }
    }


    // Partition logged-in entrant's reservations into Active and Cancelled lists
    val activeReservations = reservations.filter {
        it.entrantId == profile.deviceId && it.status == ReservationStatus.ACTIVE
    }
    val cancelledReservations = reservations
        .filter { it.entrantId == profile.deviceId && it.status == ReservationStatus.CANCELLED }
        .sortedByDescending { it.cancelledAtMillis ?: 0L }

    val displayedReservations = if (showCancelled) cancelledReservations else activeReservations

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showCancelled) "Cancelled Reservations" else "My Active Reservations",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips allowing entrants to switch between Active bookings and Cancelled records
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showCancelled,
                    onClick = { showCancelled = false },
                    label = { Text("Active (${activeReservations.size})") }
                )
                FilterChip(
                    selected = showCancelled,
                    onClick = { showCancelled = true },
                    label = { Text("Cancelled (${cancelledReservations.size})") }
                )
            }

            if (displayedReservations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showCancelled) "No cancelled reservations found." else "No active reservations found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayedReservations, key = { it.id }) { reservation ->
                        ReservationCard(
                            reservation = reservation,
                            onCancelClick = if (reservation.isUpcomingAndCancellable()) {
                                { reservationPendingCancel = reservation }
                            } else null,
                            onExportCsvClick = if (reservation.status == ReservationStatus.CANCELLED) {
                                { triggerCsvExport(reservation) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Step 1: Confirmation prompt before cancelling an active reservation
    val targetToCancel = reservationPendingCancel
    if (targetToCancel != null) {
        val dateRange = formatReservationDateRange(targetToCancel.bookedDates)
        AlertDialog(
            onDismissRequest = {
                if (!isCancelling) reservationPendingCancel = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Cancel Warning",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Cancel Reservation?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Are you sure you want to cancel your stay at \"${targetToCancel.campsiteName.ifBlank { "Campsite Reservation" }}\"?")
                    Text(
                        text = "Dates: $dateRange\nConfirmation ID: ${targetToCancel.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "You will receive an in-app cancellation confirmation notification and can download a CSV record.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCancelling = true
                        repository.cancelReservation(targetToCancel.id) { result ->
                            isCancelling = false
                            reservationPendingCancel = null
                            result.onSuccess { outcome ->
                                completedCancellation = outcome
                                // Surface notification delivery failure prominently if it occurred
                                if (!outcome.notificationDelivered) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = outcome.notificationError
                                                ?: "Reservation cancelled, but notification delivery failed."
                                        )
                                    }
                                }
                            }.onFailure { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = error.localizedMessage ?: "Failed to cancel reservation."
                                    )
                                }
                            }
                        }
                    },
                    enabled = !isCancelling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isCancelling) "Cancelling..." else "Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { reservationPendingCancel = null },
                    enabled = !isCancelling
                ) {
                    Text("Keep Booking")
                }
            }
        )
    }

    // Step 2: Post-cancellation confirmation dialog with notification status and immediate CSV export
    val outcome = completedCancellation
    if (outcome != null) {
        val cancelledSite = outcome.cancelledReservation
        val dateRange = formatReservationDateRange(cancelledSite.bookedDates)
        AlertDialog(
            onDismissRequest = { completedCancellation = null },
            icon = {
                Icon(
                    imageVector = if (outcome.notificationDelivered) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Cancellation Confirmed",
                    tint = if (outcome.notificationDelivered) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Reservation Cancelled", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${cancelledSite.campsiteName.ifBlank { "Campsite Reservation" }}\nDates: $dateRange\nConfirmation ID: ${cancelledSite.id}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (outcome.notificationDelivered) {
                        Text(
                            text = "A cancellation confirmation notification has been saved to your Notifications log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        // Graceful error feedback when notification delivery fails
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = outcome.notificationError
                                    ?: "Your reservation was cancelled, but we could not deliver the confirmation notification. Please download your CSV record below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        completedCancellation = null
                        triggerCsvExport(cancelledSite)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Download CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { completedCancellation = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    onCancelClick: (() -> Unit)? = null,
    onExportCsvClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateRange = formatReservationDateRange(reservation.bookedDates)
    val isCancelled = reservation.status == ReservationStatus.CANCELLED

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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (isCancelled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = reservation.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isCancelled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
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

            // Action button: Cancel for active bookings, Download CSV for cancelled bookings
            if (onCancelClick != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancel Reservation",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel Reservation")
                    }
                }
            } else if (onExportCsvClick != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onExportCsvClick) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Download CSV",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Download CSV")
                    }
                }
            }
        }
    }
}

/** Helper to format a reservation's booked night dates into a display range. */
private fun formatReservationDateRange(bookedDates: List<String>): String = when {
    bookedDates.isEmpty() -> "Dates not specified"
    bookedDates.size == 1 -> bookedDates.first()
    else -> "${bookedDates.first()} - ${bookedDates.last()}"
}