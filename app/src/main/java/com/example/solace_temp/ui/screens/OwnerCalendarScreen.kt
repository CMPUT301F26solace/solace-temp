package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Read-only calendar of booked and available dates for an owner's campsites. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerCalendarScreen(repository: AppRepository, onBack: () -> Unit) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val ownedSites = campsites.filter { it.ownerId == profile.deviceId }
    var selectedCampsiteId by remember { mutableStateOf<String?>(null) }
    val campsite = ownedSites.find { it.id == selectedCampsiteId } ?: ownedSites.firstOrNull()
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
    }
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    val startOffset = month.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val booked = campsite?.let { campsiteBookingsByDay(it.id, reservations) }.orEmpty()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Booked Dates") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("owner_calendar"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (campsite == null) {
                Text("You haven't registered any campsites yet.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ownedSites, key = { it.id }) { site ->
                        if (site.id == campsite.id) {
                            Button(onClick = { selectedCampsiteId = site.id; selectedDate = null }) {
                                Text(site.name)
                            }
                        } else {
                            OutlinedButton(onClick = { selectedCampsiteId = site.id; selectedDate = null }) {
                                Text(site.name)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { monthOffset--; selectedDate = null }) { Text("Previous") }
                    Text(monthTitle, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = { monthOffset++; selectedDate = null }) { Text("Next") }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                val cells = (0 until startOffset).map { null } +
                    (1..daysInMonth).map { it }
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (week + List(7 - week.size) { null }).forEach { day ->
                            Box(modifier = Modifier.weight(1f).height(54.dp).padding(2.dp)) {
                                if (day != null) {
                                    val date = String.format(Locale.US, "%04d-%02d-%02d",
                                        month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1, day)
                                    val status = calendarDayStatus(campsite, date, booked)
                                    val background = when (status) {
                                        CalendarDayStatus.BOOKED -> Color(0xFFFFCDD2)
                                        CalendarDayStatus.BLOCKED -> Color(0xFFE0E0E0)
                                        CalendarDayStatus.AVAILABLE -> Color(0xFFE8F5E9)
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(background, RoundedCornerShape(8.dp))
                                            .clickable { selectedDate = date }
                                            .testTag("calendar_day_$date"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(day.toString(), color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Booked (pink) · Blocked (gray) · Available (green)",
                    style = MaterialTheme.typography.bodySmall)
                selectedDate?.let { date ->
                    val status = calendarDayStatus(campsite, date, booked)
                    Card(modifier = Modifier.fillMaxWidth().testTag("calendar_day_details")) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(date, fontWeight = FontWeight.Bold)
                            Text(status.name.lowercase().replaceFirstChar { it.uppercase() })
                            booked[date].orEmpty().forEach { booking ->
                                Text("Entrant ID: ${booking.entrantId}")
                                Text("Reservation ID: ${booking.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}
