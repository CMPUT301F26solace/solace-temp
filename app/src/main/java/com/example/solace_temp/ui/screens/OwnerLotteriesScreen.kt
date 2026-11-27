/**
 * OwnerLotteriesScreen.kt
 * 
 * Purpose: Composable screen for campsite owners to view and manage waiting list lotteries across their registered campsites.
 * 
 * Issues / Outstanding Issues:
 * - Displays only lotteries associated with campsites owned by the currently logged-in owner profile.
 */
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository

// =========================================================================
// OWNER LOTTERIES SCREEN
// =========================================================================
/**
 * Composable screen for campsite owners to view a list of their managed lotteries.
 * 
 * @param repository The data repository for fetching lotteries and profile data.
 * @param onSelectLottery Callback invoked when a lottery item is tapped, receiving the lottery ID.
 * @param onOpenCalendar Callback invoked to navigate to the owner calendar screen.
 * @param onOpenReservations Callback invoked to navigate to the owner reservations screen.
 */
@Composable
fun OwnerLotteriesScreen(
    repository: AppRepository,
    onSelectLottery: (String) -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenReservations: () -> Unit = {}
) {
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()

    // Show only lotteries whose campsites actually belong to this owner.
    val ownedCampsiteIds = campsites.filter { it.ownerId == profile.deviceId }
        .map { it.id }.toSet()
    val ownedLotteries = lotteries.filter { lottery ->
        lottery.ownerId == profile.deviceId &&
            lottery.campsiteIds.isNotEmpty() &&
            lottery.campsiteIds.all { it in ownedCampsiteIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8EEF8))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Manage Lotteries",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "View and manage waiting list lotteries across your registered campsites.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        // Quick Action Buttons for Owner Management
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onOpenCalendar,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_lottery_calendar"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Calendar")
            }
            Button(
                onClick = onOpenReservations,
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_lottery_reservations"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reservations")
            }
        }

        if (ownedLotteries.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "No Lotteries",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No lotteries configured yet.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Create a lottery from any of your campsite details pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            ownedLotteries.forEach { lottery ->
                val associatedCampsites = campsites.filter { lottery.campsiteIds.contains(it.id) }
                val campsiteNames = associatedCampsites.joinToString(", ") { it.name }.ifBlank { "Assigned Campsites" }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLottery(lottery.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
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
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            val status = getLotteryStatus(lottery, associatedCampsites.firstOrNull())
                            val badgeColor = when (status) {
                                "Active" -> Color(0xFF2E7D32)
                                "Upcoming" -> Color(0xFF1976D2)
                                "Booking" -> Color(0xFF7B1FA2)
                                "Finished" -> Color(0xFF555555)
                                else -> Color(0xFF9E9E9E)
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

                        Text(
                            text = "📍 $campsiteNames",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Registration Window", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.registrationStart} to ${lottery.registrationEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Booking Window", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.bookingStart} to ${lottery.bookingEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Waitlist Entrants", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = "${lottery.waitingListEntrantIds.size} Enrolled",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val now = System.currentTimeMillis()
                        val isPastDeadline = (lottery.registrationEndMillis ?: Long.MAX_VALUE) <= now
                        if (isPastDeadline && !lottery.isDrawn) {
                            Button(
                                onClick = { repository.drawLotteryWinners(lottery.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Draw Winners", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Draw Winners Now")
                            }
                        }

                        // Room for more future stuff
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Max Winners: ${lottery.maxWinners}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Tap for Details & Draw Controls →",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
