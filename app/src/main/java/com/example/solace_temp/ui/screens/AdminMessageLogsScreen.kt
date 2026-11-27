/**
 * AdminMessageLogsScreen.kt
 *
 * Purpose: Provides a dedicated administrative communication audit log view (US 03.08.01, US 03.12.01)
 * allowing verified administrators to review logs of direct messages sent between campsite owners
 * and reserving entrants. Implements access restriction, metadata display, thread inspection,
 * search/filter capabilities, admin audit logging, and lazy loading performance.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.MessageDeliveryStatus
import com.example.solace_temp.models.OwnerEntrantMessage
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMessageLogsScreen(
    repository: AppRepository,
    onBack: () -> Unit = {}
) {
    val profile by repository.currentProfile.collectAsState()
    val allMessages by repository.ownerEntrantMessages.collectAsState()
    val auditLogs by repository.adminAuditLogs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ReservationStatus?>(null) }
    var selectedCampsiteFilter by remember { mutableStateOf("All") }
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL_TIME) }

    var selectedThreadMessage by remember { mutableStateOf<OwnerEntrantMessage?>(null) }

    // Access Restriction: Only authenticated users with verified administrator permissions
    val isAuthorizedAdmin = profile.isAdmin && profile.role == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communication Audit Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!isAuthorizedAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
                    .testTag("admin_logs_access_denied"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Access Denied",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Access Denied: Admin Permissions Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This owner-to-entrant communication audit log view is restricted exclusively to authenticated users with verified administrator permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@Scaffold
        }

        // Filter messages based on search query, campsite, reservation status, and date range
        val filteredMessages = remember(
            allMessages, searchQuery, selectedStatusFilter, selectedCampsiteFilter, selectedDateRange
        ) {
            val now = System.currentTimeMillis()
            val minTimestamp = when (selectedDateRange) {
                DateRangeFilter.ALL_TIME -> 0L
                DateRangeFilter.LAST_7_DAYS -> now - 7 * 86400000L
                DateRangeFilter.LAST_30_DAYS -> now - 30 * 86400000L
            }

            allMessages.filter { msg ->
                val matchesQuery = searchQuery.isBlank() ||
                    msg.ownerId.contains(searchQuery, ignoreCase = true) ||
                    msg.ownerName.contains(searchQuery, ignoreCase = true) ||
                    msg.entrantId.contains(searchQuery, ignoreCase = true) ||
                    msg.entrantName.contains(searchQuery, ignoreCase = true) ||
                    msg.reservationId.contains(searchQuery, ignoreCase = true) ||
                    msg.messageText.contains(searchQuery, ignoreCase = true) ||
                    msg.campsiteName.contains(searchQuery, ignoreCase = true)

                val matchesCampsite = selectedCampsiteFilter == "All" || msg.campsiteName.equals(selectedCampsiteFilter, ignoreCase = true)
                val matchesStatus = selectedStatusFilter == null || msg.reservationStatus == selectedStatusFilter
                val matchesDate = msg.timestampMillis >= minTimestamp

                matchesQuery && matchesCampsite && matchesStatus && matchesDate
            }.sortedByDescending { it.timestampMillis }
        }

        val availableCampsites = remember(allMessages) {
            listOf("All") + allMessages.map { it.campsiteName }.distinct().filter { it.isNotBlank() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Admin Compliance & Audit Trail Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Audited",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 Admin Audit Active",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Access to communication logs is audited (${auditLogs.size} review access logs recorded).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_log_search"),
                placeholder = { Text("Search by owner, entrant, reservation ID, or keyword...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Reservation Status Filter Chips (Split into two rows so CANCELLED, PAST, etc. are on a row below)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Status Filter:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All") },
                        modifier = Modifier.testTag("filter_status_all")
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ReservationStatus.ACTIVE,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ReservationStatus.ACTIVE) null else ReservationStatus.ACTIVE },
                        label = { Text("ACTIVE") },
                        modifier = Modifier.testTag("filter_status_ACTIVE")
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ReservationStatus.INVITED,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ReservationStatus.INVITED) null else ReservationStatus.INVITED },
                        label = { Text("INVITED") },
                        modifier = Modifier.testTag("filter_status_INVITED")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == ReservationStatus.CANCELLED,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ReservationStatus.CANCELLED) null else ReservationStatus.CANCELLED },
                        label = { Text("CANCELLED") },
                        modifier = Modifier.testTag("filter_status_CANCELLED")
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ReservationStatus.PAST,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ReservationStatus.PAST) null else ReservationStatus.PAST },
                        label = { Text("PAST") },
                        modifier = Modifier.testTag("filter_status_PAST")
                    )
                    FilterChip(
                        selected = selectedStatusFilter == ReservationStatus.REJECTED,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == ReservationStatus.REJECTED) null else ReservationStatus.REJECTED },
                        label = { Text("REJECTED") },
                        modifier = Modifier.testTag("filter_status_REJECTED")
                    )
                }
            }

            // Date Range & Campsite Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Range:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateRangeFilter.entries.forEach { range ->
                    FilterChip(
                        selected = selectedDateRange == range,
                        onClick = { selectedDateRange = range },
                        label = { Text(range.label) }
                    )
                }
            }

            Text(
                text = "Showing ${filteredMessages.size} message log entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Communication Logs List or Empty State
            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("admin_logs_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "No Logs",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No communication logs found matching your search or filter criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { msg ->
                        MessageLogCard(
                            message = msg,
                            onClick = {
                                selectedThreadMessage = msg
                                repository.logAdminAuditAccess(
                                    adminId = profile.deviceId,
                                    adminName = profile.name,
                                    reservationId = msg.reservationId,
                                    ownerId = msg.ownerId,
                                    entrantId = msg.entrantId
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Thread Conversation Detail Dialog
    selectedThreadMessage?.let { selectedMsg ->
        val threadMessages = remember(allMessages, selectedMsg) {
            allMessages.filter {
                it.reservationId == selectedMsg.reservationId ||
                (it.ownerId == selectedMsg.ownerId && it.entrantId == selectedMsg.entrantId)
            }.sortedBy { it.timestampMillis }
        }

        AlertDialog(
            onDismissRequest = { selectedThreadMessage = null },
            title = {
                Column {
                    Text("Full Conversation History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Reservation #${selectedMsg.reservationId} · ${selectedMsg.campsiteName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 Log review recorded for admin Audit Trail.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(threadMessages) { msg ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "From: ${msg.ownerName.ifBlank { msg.ownerId }}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatTimestamp(msg.timestampMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = "To: ${msg.entrantName.ifBlank { msg.entrantId }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = msg.messageText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedThreadMessage = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MessageLogCard(
    message: OwnerEntrantMessage,
    onClick: () -> Unit
) {
    val statusColor = when (message.reservationStatus) {
        ReservationStatus.ACTIVE -> Color(0xFF2E7D32)
        ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.error
        ReservationStatus.PAST -> Color(0xFF757575)
        ReservationStatus.INVITED -> Color(0xFF1976D2)
        ReservationStatus.REJECTED -> Color(0xFFC62828)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("message_log_item_${message.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row 1: Campsite Name & Reservation ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 ${message.campsiteName.ifBlank { "Campsite" }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Reservation #${message.reservationId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Header Row 2: Status Badge on its own row, left-aligned so it isn't shoved to the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = message.reservationStatus.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Sender (Owner) and Recipient (Entrant) Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Owner: ${message.ownerName.ifBlank { message.ownerId }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID: ${message.ownerId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Entrant: ${message.entrantName.ifBlank { message.entrantId }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "ID: ${message.entrantId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Message Preview & Delivery Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTimestamp(message.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    val deliveryLabel = when (message.status) {
                        MessageDeliveryStatus.READ -> "✓✓ READ"
                        MessageDeliveryStatus.DELIVERED -> "✓ DELIVERED"
                        MessageDeliveryStatus.SENT -> "SENT"
                    }
                    Text(
                        text = deliveryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (message.status == MessageDeliveryStatus.READ) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            Text(
                text = "Tap to view full thread history →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
}
