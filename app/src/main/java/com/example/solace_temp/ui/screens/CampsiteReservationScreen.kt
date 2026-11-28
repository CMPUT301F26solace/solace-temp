/**
 * CampsiteReservationScreen.kt
 *
 * Purpose: Screen for winning entrants to select valid dates and confirm campsite reservations.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteReservationScreen(
    campsiteId: String,
    notificationId: String,
    onReservationConfirmed: (startDate: String, endDate: String, onError: (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startDate by remember { mutableStateOf("2027-07-01") }
    var endDate by remember { mutableStateOf("2027-07-07") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Reservation Dates") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Congratulations! Select your check-in and check-out dates to confirm your spot.",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = {
                    startDate = it
                    errorMessage = null
                },
                label = { Text("Check-in Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = {
                    endDate = it
                    errorMessage = null
                },
                label = { Text("Check-out Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val trimmedStart = startDate.trim()
                        val trimmedEnd = endDate.trim()
                        val validationError = validateDateRange(trimmedStart, trimmedEnd)
                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }

                        errorMessage = null
                        isSubmitting = true
                        onReservationConfirmed(trimmedStart, trimmedEnd) { message ->
                            isSubmitting = false
                            errorMessage = message
                        }
                    },
                    enabled = !isSubmitting && startDate.isNotBlank() && endDate.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Booking")
                    }
                }
            }
        }
    }
}

/** Validates ISO date format (YYYY-MM-DD), ensures dates are upcoming, and caps stay length at 60 days. */
private fun validateDateRange(startDate: String, endDate: String): String? {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }
    val start = runCatching { formatter.parse(startDate) }.getOrNull()
        ?: return "Enter a valid check-in date (YYYY-MM-DD)."
    val end = runCatching { formatter.parse(endDate) }.getOrNull()
        ?: return "Enter a valid check-out date (YYYY-MM-DD)."

    val todayIso = formatter.format(Date())
    if (startDate < todayIso) {
        return "Check-in date must be today ($todayIso) or later."
    }
    if (end.before(start)) {
        return "Check-out date cannot be before check-in date."
    }
    val days = ((end.time - start.time) / (24L * 60L * 60L * 1000L)) + 1
    if (days > 60) {
        return "Reservations cannot exceed 60 days."
    }
    return null
}