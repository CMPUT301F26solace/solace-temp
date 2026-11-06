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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteReservationScreen(
    campsiteId: String,
    notificationId: String,
    onReservationConfirmed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startDate by remember { mutableStateOf("2026-07-01") }
    var endDate by remember { mutableStateOf("2026-07-07") }
    var isSubmitting by remember { mutableStateOf(false) }

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
                onValueChange = { startDate = it },
                label = { Text("Check-in Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("Check-out Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        isSubmitting = true
                        onReservationConfirmed()
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