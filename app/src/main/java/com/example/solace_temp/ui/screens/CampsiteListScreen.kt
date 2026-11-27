package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.CampsiteFilter

// =========================================================================
// CAMPSITE LIST SCREEN (Used by Entrant Search & Owner My Campsites)
// =========================================================================
/**
 * Composable screen to display a list of campsites with location, date, and guest filters.
 *
 * @param title The title text for this screen.
 * @param repository The data repository for fetching campsite data.
 * @param onSelectCampsite Callback invoked when a campsite is tapped, receiving the campsite ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteListScreen(
    title: String,
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit
) {
    val campsites by repository.campsites.collectAsState()
    var locationQuery by remember { mutableStateOf("") }
    var guestCountInput by remember { mutableStateOf("") }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val minGuests = guestCountInput.toIntOrNull()
    val hasActiveFilters = locationQuery.isNotBlank() ||
            minGuests != null ||
            startDateMillis != null ||
            endDateMillis != null

    val filtered = remember(campsites, locationQuery, startDateMillis, endDateMillis, minGuests) {
        CampsiteFilter.filter(
            campsites = campsites,
            locationQuery = locationQuery,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            minGuests = minGuests
        )
    }

    if (showDatePicker) {
        val dateRangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDateMillis,
            initialSelectedEndDateMillis = endDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDateMillis = dateRangeState.selectedStartDateMillis
                        endDateMillis = dateRangeState.selectedEndDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Apply Dates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangeState,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (hasActiveFilters) {
                TextButton(
                    onClick = {
                        locationQuery = ""
                        guestCountInput = ""
                        startDateMillis = null
                        endDateMillis = null
                    },
                    modifier = Modifier.testTag("clear_filters_button")
                ) {
                    Text("Clear Filters", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = locationQuery,
            onValueChange = { locationQuery = it },
            label = { Text("Location or Alberta park...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("filter_location_input"),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(56.dp)
                    .testTag("filter_date_button")
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Select Dates", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                val dateLabel = when {
                    startDateMillis != null && endDateMillis != null ->
                        "${formatDateMillis(startDateMillis)} - ${formatDateMillis(endDateMillis)}"
                    startDateMillis != null -> formatDateMillis(startDateMillis)
                    else -> "Any Dates"
                }
                Text(text = dateLabel, maxLines = 1)
            }

            OutlinedTextField(
                value = guestCountInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 2) {
                        guestCountInput = input
                    }
                },
                label = { Text("Guests") },
                modifier = Modifier
                    .weight(0.8f)
                    .testTag("filter_guests_input"),
                singleLine = true
            )
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_filter_message")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (hasActiveFilters) {
                            "No campsites match your selected filters."
                        } else {
                            "No campsites available."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    if (hasActiveFilters) {
                        Text(
                            text = "Try adjusting your location, dates, or guest count, or clear all filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { site ->
                    val ratingLabel = if (site.rating > 0.0) {
                        "★ ${site.rating}"
                    } else {
                        "Not yet rated"
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCampsite(site.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("$${site.pricePerNight.toInt()}/night", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "📍 ${site.location.locationName} · Max ${site.maxOccupancy} guests · $ratingLabel"
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Tap to view details & QR code →", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
