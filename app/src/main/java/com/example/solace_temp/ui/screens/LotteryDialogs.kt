/**
 * LotteryDialogs.kt
 * 
 * Purpose: Dialog composables for creating and editing lottery configurations, validating date windows, winner counts, and waitlist capacity limits.
 * 
 * Issues / Outstanding Issues:
 * - Enforces minimum 1-week gap between registration close and booking period start.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery

// =========================================================================
// EDIT LOTTERY DIALOG
// =========================================================================
/**
 * Dialog allowing Campsite Owners or Admins to edit an existing lottery's configuration.
 * 
 * Configurable fields include lottery name, max winners, waitlist capacity, assigned campsites,
 * registration window, and booking window. Enforces registration date editing constraints based
 * on lottery status (Upcoming allows editing start & end; Active allows editing end date only),
 * and validates that booking start is at least 1 week after registration close.
 * 
 * @param lottery The [Lottery] entity being edited.
 * @param repository The data repository used to save lottery updates.
 * @param onDismiss Callback invoked when the dialog is dismissed or cancelled.
 * @param onSuccess Callback invoked when the lottery edit is completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLotteryDialog(
    lottery: Lottery,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val lotteries by repository.lotteries.collectAsState()

    var lotteryName by remember(lottery) { mutableStateOf(lottery.name) }
    var selectedCampsiteIds by remember(lottery) { mutableStateOf(lottery.campsiteIds) }
    var maxWinnersText by remember(lottery) { mutableStateOf(lottery.maxWinners.toString()) }
    var waitlistCapacityText by remember(lottery) { mutableStateOf(lottery.waitlistCapacity?.toString() ?: "") }
    var regStartMillis by remember(lottery) { mutableStateOf(lottery.registrationStartMillis) }
    var regEndMillis by remember(lottery) { mutableStateOf(lottery.registrationEndMillis) }
    var bookingStartMillis by remember(lottery) { mutableStateOf(lottery.bookingStartMillis) }
    var bookingEndMillis by remember(lottery) { mutableStateOf(lottery.bookingEndMillis) }

    var showRegRangeDialog by remember { mutableStateOf(false) }
    var showRegEndSingleDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter {
        (profile.deviceId.isNotBlank() && it.ownerId == profile.deviceId) ||
            lotteries.find { lot -> lot.id == lottery.id }?.ownerId == it.ownerId ||
            lottery.campsiteIds.contains(it.id)
    }.ifEmpty {
        campsites.filter { lottery.campsiteIds.contains(it.id) }
    }
    // Only display campsites that are available or currently assigned to this lottery
    val availableOwnerCampsites = ownerCampsites.filter { site ->
        selectedCampsiteIds.contains(site.id) || !lotteries.any { lot ->
            lot.id != lottery.id && lot.campsiteIds.contains(site.id) && getLotteryStatus(lot, site) != "Finished" && getLotteryStatus(lot, site) != "N/A"
        }
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
    val totalMaxGuests = activeCampsites.sumOf { it.maxOccupancy }

    val status = getLotteryStatus(lottery, activeCampsites.firstOrNull())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (isSuccessState) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    Text("Lottery Updated", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Edit Lottery Configuration", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (isSuccessState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Lottery configuration for \"${lotteryName.trim()}\" has been successfully updated.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Changes are saved and live in the system.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Update the lottery settings below. Required fields and date spacing rules will be verified before saving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                    OutlinedTextField(
                        value = waitlistCapacityText,
                        onValueChange = { waitlistCapacityText = it },
                        label = { Text("Waitlist capacity (optional)") },
                        supportingText = { Text("Leave blank for unlimited entrants") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assigned Campsite(s):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    availableOwnerCampsites.forEach { site ->
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

                    when (status) {
                        "Upcoming" -> {
                            OutlinedButton(
                                onClick = { showRegRangeDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Registration Start & End Dates")
                            }
                        }
                        "Active" -> {
                            OutlinedButton(
                                onClick = { showRegEndSingleDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Registration End Date Only")
                            }
                            Text(
                                "(Registration is currently Active. Open date cannot be modified.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                "(Registration period has closed. Registration dates cannot be modified.)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            }
        },
        confirmButton = {
            if (isSuccessState) {
                Button(onClick = onSuccess) { Text("Done") }
            } else {
                Button(
                    onClick = {
                        val parsedWinners = maxWinnersText.toIntOrNull()
                        val parsedCapacity = if (waitlistCapacityText.isBlank()) null else waitlistCapacityText.toIntOrNull()

                        when {
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
                            waitlistCapacityText.isNotBlank() && (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
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
                            status == "Upcoming" && regStartMillis!! < System.currentTimeMillis() - 86400000L -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Registration open date must be set in the future."
                            }
                            status == "Active" && regEndMillis!! < System.currentTimeMillis() - 86400000L -> {
                                feedbackIsError = true
                                feedbackMsg = "Error: Registration close date must be set in the future."
                            }
                            else -> {
                                feedbackIsError = false
                                val updated = lottery.copy(
                                    name = lotteryName.trim(),
                                    campsiteIds = selectedCampsiteIds,
                                    maxWinners = parsedWinners,
                                    waitlistCapacity = parsedCapacity,
                                    registrationStartMillis = regStartMillis,
                                    registrationEndMillis = regEndMillis,
                                    registrationStart = formatDateMillis(regStartMillis),
                                    registrationEnd = formatDateMillis(regEndMillis),
                                    bookingStartMillis = bookingStartMillis,
                                    bookingEndMillis = bookingEndMillis,
                                    bookingStart = formatDateMillis(bookingStartMillis),
                                    bookingEnd = formatDateMillis(bookingEndMillis)
                                )
                                repository.updateLottery(updated) { result ->
                                    result.fold(
                                        onSuccess = {
                                            isSuccessState = true
                                        },
                                        onFailure = { err ->
                                            feedbackIsError = true
                                            feedbackMsg = "Error updating lottery: ${err.message}"
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            if (!isSuccessState) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showRegRangeDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = regStartMillis,
            initialSelectedEndDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regStartMillis = dateState.selectedStartDateMillis
                    regEndMillis = dateState.selectedEndDateMillis
                    showRegRangeDialog = false
                }) { Text("Save Dates") }
            },
            dismissButton = { TextButton(onClick = { showRegRangeDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }

    if (showRegEndSingleDialog) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegEndSingleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regEndMillis = dateState.selectedDateMillis
                    showRegEndSingleDialog = false
                }) { Text("Save End Date") }
            },
            dismissButton = { TextButton(onClick = { showRegEndSingleDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(state = dateState, modifier = Modifier.fillMaxWidth())
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
    var maxWinnersText by remember { mutableStateOf("3") }
    var waitlistCapacityText by remember { mutableStateOf("") }
    var regStartMillis by remember { mutableStateOf<Long?>(null) }
    var regEndMillis by remember { mutableStateOf<Long?>(null) }
    var bookingStartMillis by remember { mutableStateOf<Long?>(null) }
    var bookingEndMillis by remember { mutableStateOf<Long?>(null) }
    var showRegDateDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter {
        (profile.deviceId.isNotBlank() && it.ownerId == profile.deviceId) || it.ownerId == campsite.ownerId
    }.ifEmpty { listOf(campsite) }
    // Only display campsites that are available for creating a lottery
    val availableOwnerCampsites = ownerCampsites.filter { isCampsiteAvailableForLottery(it, lotteries) }
    var selectedCampsiteIds by remember(availableOwnerCampsites) {
        mutableStateOf(
            if (availableOwnerCampsites.any { it.id == campsite.id }) {
                listOf(campsite.id)
            } else {
                availableOwnerCampsites.take(1).map { it.id }
            }
        )
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
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
                OutlinedTextField(
                    value = waitlistCapacityText,
                    onValueChange = { waitlistCapacityText = it },
                    label = { Text("Waitlist capacity (optional)") },
                    supportingText = { Text("Leave blank for unlimited entrants") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Assigned Campsite(s):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (availableOwnerCampsites.isEmpty()) {
                    Text(
                        "No campsites are currently available for creating a lottery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    availableOwnerCampsites.forEach { site ->
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
                    val hasActive = selectedCampsiteIds.isEmpty() || selectedCampsiteIds.any { selectedId ->
                        val site = campsites.find { it.id == selectedId } ?: Campsite(id = selectedId)
                        !isCampsiteAvailableForLottery(site, lotteries)
                    }
                    val parsedWinners = maxWinnersText.toIntOrNull()
                    val parsedCapacity = if (waitlistCapacityText.isBlank()) null
                        else waitlistCapacityText.toIntOrNull()

                    when {
                        availableOwnerCampsites.isEmpty() || selectedCampsiteIds.isEmpty() -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Please select at least one available campsite."
                        }
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
                        waitlistCapacityText.isNotBlank() &&
                            (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                            feedbackIsError = true
                            feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
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
                                    waitlistCapacity = parsedCapacity,
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
