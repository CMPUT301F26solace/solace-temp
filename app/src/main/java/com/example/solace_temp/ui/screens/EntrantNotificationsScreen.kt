/**
 * EntrantNotificationsScreen.kt
 *
 * Purpose: Renders the list of notifications for the current entrant, including winning
 * lottery offers with accept/decline actions and non-selected status updates.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.AppNotification

/**
 * Screen displaying lottery win and loss notifications for the entrant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrantNotificationsScreen(
    repository: AppRepository,
    onAcceptReservation: (campsiteId: String, notificationId: String) -> Unit,
    onDeclineReservation: (notificationId: String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val notifications by repository.notifications.collectAsState()

    // Filter notifications directed to the logged-in user
    val userNotifications = notifications.filter {
        it.recipientId.isBlank() || it.recipientId == profile.deviceId
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications", fontWeight = FontWeight.Bold) }) }
    ) { innerPadding ->
        if (userNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notifications yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(userNotifications) { item ->
                    NotificationCard(
                        notification = item,
                        onAccept = { onAcceptReservation(item.campsiteId, item.id) },
                        onDecline = { onDeclineReservation(item.id) }
                    )
                }
            }
        }
    }
}

/**
 * Single card displaying notification details and optional winning action buttons.
 */
@Composable
private fun NotificationCard(
    notification: AppNotification,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (notification.isLotteryWin)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )

            // Show accept/decline action buttons if user won the lottery
            if (notification.isLotteryWin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Accept Reservation")
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}