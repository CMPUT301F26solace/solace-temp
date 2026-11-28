/**
 * AdminNotificationLogsScreen.kt
 *
 * Purpose: Provides a central notification log viewer (US 03.07.01, US 03.11.01) for
 * verified administrators. Displays system-wide logs of dispatched platform notifications
 * (owner broadcasts, lottery draw alerts, system updates, reservation alerts) with metadata,
 * delivery status, notification channels, search/filter controls, and thread breakdown dialogs.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
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
import com.example.solace_temp.models.NotificationChannel
import com.example.solace_temp.models.NotificationDeliveryStatus
import com.example.solace_temp.models.NotificationType
import com.example.solace_temp.models.SystemNotificationLog
import com.example.solace_temp.models.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationLogsScreen(
    repository: AppRepository,
    onBack: () -> Unit = {}
) {
    val profile by repository.currentProfile.collectAsState()
    val allNotifLogs by repository.systemNotificationLogs.collectAsState()
    val retrievalError by repository.notificationLogsError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<NotificationType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<NotificationDeliveryStatus?>(null) }
    var selectedSenderRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL_TIME) }

    var selectedLogDetail by remember { mutableStateOf<SystemNotificationLog?>(null) }

    // Access Restriction: Only authenticated users with verified administrator permissions
    val isAuthorizedAdmin = profile.isAdmin || profile.role == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Platform Notification Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .testTag("admin_notif_logs_access_denied"),
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
                            text = "This central notification log viewer is restricted exclusively to authenticated users with verified administrator permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@Scaffold
        }

        // Filter notification logs based on search query, type, delivery status, role, and date range
        val filteredLogs = remember(
            allNotifLogs, searchQuery, selectedTypeFilter, selectedStatusFilter, selectedSenderRoleFilter, selectedDateRange
        ) {
            val now = System.currentTimeMillis()
            val minTimestamp = when (selectedDateRange) {
                DateRangeFilter.ALL_TIME -> 0L
                DateRangeFilter.LAST_7_DAYS -> now - 7 * 86400000L
                DateRangeFilter.LAST_30_DAYS -> now - 30 * 86400000L
            }

            allNotifLogs.filter { log ->
                val matchesQuery = searchQuery.isBlank() ||
                    log.senderId.contains(searchQuery, ignoreCase = true) ||
                    log.senderName.contains(searchQuery, ignoreCase = true) ||
                    log.campsiteId.contains(searchQuery, ignoreCase = true) ||
                    log.campsiteName.contains(searchQuery, ignoreCase = true) ||
                    log.title.contains(searchQuery, ignoreCase = true) ||
                    log.messageBody.contains(searchQuery, ignoreCase = true) ||
                    log.recipientIds.any { it.contains(searchQuery, ignoreCase = true) }

                val matchesType = selectedTypeFilter == null || log.type == selectedTypeFilter
                val matchesStatus = selectedStatusFilter == null || log.deliveryStatus == selectedStatusFilter
                val matchesRole = selectedSenderRoleFilter == null || log.senderRole == selectedSenderRoleFilter
                val matchesDate = log.timestampMillis >= minTimestamp

                matchesQuery && matchesType && matchesStatus && matchesRole && matchesDate
            }.sortedByDescending { it.timestampMillis }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error Feedback Notice
            if (retrievalError != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().testTag("admin_notif_logs_error")
                ) {
                    Text(
                        text = "Error retrieving notification logs: $retrievalError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notif_log_search"),
                placeholder = { Text("Search by user ID, campsite ID, title, or keywords...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            var typeDropdownExpanded by remember { mutableStateOf(false) }
            var statusDropdownExpanded by remember { mutableStateOf(false) }
            var dateDropdownExpanded by remember { mutableStateOf(false) }

            // Single-line Dropdown Filters Bar (Leaves majority of screen for actual logs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification Type Dropdown
                Box {
                    FilterChip(
                        selected = selectedTypeFilter != null,
                        onClick = { typeDropdownExpanded = true },
                        label = {
                            val typeText = when (selectedTypeFilter) {
                                NotificationType.LOTTERY_DRAW -> "Lottery Draw"
                                NotificationType.OWNER_BROADCAST -> "Broadcast"
                                NotificationType.SYSTEM_UPDATE -> "System Update"
                                NotificationType.RESERVATION_ALERT -> "Reservation Alert"
                                null -> "All"
                            }
                            Text("Type: $typeText ▾")
                        },
                        modifier = Modifier.testTag("filter_notif_type_dropdown")
                    )
                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Types") },
                            onClick = {
                                selectedTypeFilter = null
                                typeDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_notif_type_all")
                        )
                        DropdownMenuItem(
                            text = { Text("Lottery Draw") },
                            onClick = {
                                selectedTypeFilter = NotificationType.LOTTERY_DRAW
                                typeDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_notif_type_LOTTERY_DRAW")
                        )
                        DropdownMenuItem(
                            text = { Text("Broadcast") },
                            onClick = {
                                selectedTypeFilter = NotificationType.OWNER_BROADCAST
                                typeDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_notif_type_OWNER_BROADCAST")
                        )
                        DropdownMenuItem(
                            text = { Text("System Update") },
                            onClick = {
                                selectedTypeFilter = NotificationType.SYSTEM_UPDATE
                                typeDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_notif_type_SYSTEM_UPDATE")
                        )
                        DropdownMenuItem(
                            text = { Text("Reservation Alert") },
                            onClick = {
                                selectedTypeFilter = NotificationType.RESERVATION_ALERT
                                typeDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_notif_type_RESERVATION_ALERT")
                        )
                    }
                }

                // Delivery Status Dropdown
                Box {
                    FilterChip(
                        selected = selectedStatusFilter != null,
                        onClick = { statusDropdownExpanded = true },
                        label = { Text("Delivery: ${selectedStatusFilter?.name ?: "All"} ▾") },
                        modifier = Modifier.testTag("filter_delivery_dropdown")
                    )
                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Statuses") },
                            onClick = {
                                selectedStatusFilter = null
                                statusDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("filter_delivery_all")
                        )
                        NotificationDeliveryStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    selectedStatusFilter = status
                                    statusDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("filter_delivery_${status.name}")
                            )
                        }
                    }
                }

                // Date Range Dropdown
                Box {
                    FilterChip(
                        selected = selectedDateRange != DateRangeFilter.ALL_TIME,
                        onClick = { dateDropdownExpanded = true },
                        label = { Text("Range: ${selectedDateRange.label} ▾") },
                        modifier = Modifier.testTag("filter_date_dropdown")
                    )
                    DropdownMenu(
                        expanded = dateDropdownExpanded,
                        onDismissRequest = { dateDropdownExpanded = false }
                    ) {
                        DateRangeFilter.entries.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.label) },
                                onClick = {
                                    selectedDateRange = range
                                    dateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Showing ${filteredLogs.size} system notification records",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Scalable LazyColumn showing notification log records or Empty State
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .testTag("admin_notif_logs_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "No Logs",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No notification logs match your search or filter criteria.",
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
                    items(filteredLogs, key = { it.id }) { log ->
                        NotificationLogCard(
                            log = log,
                            onClick = { selectedLogDetail = log }
                        )
                    }
                }
            }
        }
    }

    // Full Content & Recipient Breakdown Dialog
    selectedLogDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogDetail = null },
            title = {
                Column {
                    Text(log.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Dispatched: ${formatTimestamp(log.timestampMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Full Message Content:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.messageBody,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "Recipient & Delivery Breakdown:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Target Group: ${log.targetAudienceGroup}", style = MaterialTheme.typography.bodySmall)
                    Text("Channel: ${log.channel.name}", style = MaterialTheme.typography.bodySmall)
                    Text("Delivery Status: ${log.deliveryStatus.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Sender: ${log.senderName} (${log.senderRole.name} - ID: ${log.senderId})", style = MaterialTheme.typography.bodySmall)
                    if (log.campsiteName.isNotBlank()) {
                        Text("Associated Campsite: ${log.campsiteName} (ID: ${log.campsiteId})", style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = "Recipient Identifiers (${log.recipientIds.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (log.recipientIds.isEmpty()) "None recorded" else log.recipientIds.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun NotificationLogCard(
    log: SystemNotificationLog,
    onClick: () -> Unit
) {
    val statusColor = when (log.deliveryStatus) {
        NotificationDeliveryStatus.SENT -> Color(0xFF2E7D32)
        NotificationDeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
        NotificationDeliveryStatus.PENDING -> Color(0xFFE65100)
    }

    val typeLabel = when (log.type) {
        NotificationType.LOTTERY_DRAW -> "Lottery Draw"
        NotificationType.OWNER_BROADCAST -> "Broadcast"
        NotificationType.SYSTEM_UPDATE -> "System Update"
        NotificationType.RESERVATION_ALERT -> "Reservation Alert"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notif_log_item_${log.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row 1: Title & Type Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Header Row 2: Delivery Status Badge (left-aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Status: ${log.deliveryStatus.name} · ${log.channel.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Contextual Metadata: Sender & Target Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sender: ${log.senderName.ifBlank { log.senderId }} (${log.senderRole.name})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID: ${log.senderId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Target: ${log.targetAudienceGroup}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                    if (log.campsiteName.isNotBlank()) {
                        Text(
                            text = "📍 ${log.campsiteName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Body Preview & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.messageBody,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(log.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Text(
                text = "Tap to view full content & recipient breakdown →",
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
