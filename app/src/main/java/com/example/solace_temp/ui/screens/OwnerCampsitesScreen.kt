/**
 * OwnerCampsitesScreen.kt
 * 
 * Purpose: Composable screen displaying a campsite owner's registered properties with quick management actions and lottery creation triggers.
 * 
 * Issues / Outstanding Issues:
 * - Deleting a campsite cleans up associated lotteries and reservations.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository

/**
 * Composable screen for campsite owners to view a list of their owned campsites.
 * Handles the case where the owner has no campsites yet.
 * 
 * @param repository The data repository for fetching campsite data.
 * @param onSelectCampsite Callback invoked when a campsite is tapped, receiving the campsite ID.
 * @param onCreateNew Callback invoked to navigate to the create campsite screen.
 * @param onOpenCalendar Callback invoked to navigate to the owner calendar screen.
 * @param onOpenReservations Callback invoked to navigate to the owner reservations screen.
 */
@Composable
fun OwnerCampsitesScreen(
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit,
    onCreateNew: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenReservations: () -> Unit = {}
) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    
    // Filter to only include campsites owned by this user
    val myCampsites = campsites.filter { it.ownerId == profile.deviceId }
    val ownerBackgroundBlue = Color(0xFFE8EEF8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ownerBackgroundBlue)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My Registered Campsites", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "View your registered campsites, monitor active lottery statuses, and manage bookings.",
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
                    .testTag("btn_owner_calendar"),
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
                    .testTag("btn_owner_reservations"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reservations")
            }
        }

        Spacer(Modifier.height(4.dp))

        if (myCampsites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "You haven't registered any campsites yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add a Campsite")
                    }
                }
            }
        } else {
            myCampsites.forEach { site ->
                val activeLottery = lotteries.find { it.campsiteIds.contains(site.id) && getLotteryStatus(it, site) != "Finished" && getLotteryStatus(it, site) != "N/A" }
                val status = getLotteryStatus(activeLottery, site)
                val ratingLabel = if (site.rating > 0.0) "★ ${site.rating}" else "Not yet rated"

                val (statusBg, statusTextColor, badgeColor) = when (status) {
                    "Active" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF2E7D32))
                    "Upcoming" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), Color(0xFF1976D2))
                    "Booking" -> Triple(Color(0xFFF3E5F5), Color(0xFF7B1FA2), Color(0xFF7B1FA2))
                    else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), Color(0xFF9E9E9E))
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCampsite(site.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Row: Thumbnail on left, Info on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Placeholder Image Box
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE3E8EF)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!site.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = site.imageUrl,
                                        contentDescription = "Campsite Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = "Placeholder Image", tint = Color.Gray, modifier = Modifier.size(36.dp))
                                }
                            }

                            // Campsite info column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = site.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$${site.pricePerNight.toInt()}/night",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFE11D48),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "📍 ${site.location.locationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Max ${site.maxOccupancy} guests · $ratingLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 2.dp))

                        // Lottery Status & Extra Space for Lottery Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = statusBg,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val lotteryPrefix = if (activeLottery != null && activeLottery.campsiteIds.size > 1) "Multi Lottery:" else "Lottery:"
                                Text(
                                    text = if (activeLottery != null) "$lotteryPrefix ${activeLottery.name}" else "Lottery Status",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor,
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (activeLottery != null) {
                                Text(
                                    text = "Registration Window: ${activeLottery.registrationStart} to ${activeLottery.registrationEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Booking Window: ${activeLottery.bookingStart} to ${activeLottery.bookingEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Enrolled: ${activeLottery.waitingListEntrantIds.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Max Winners: ${activeLottery.maxWinners}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Configure a registration period for this campsite from details view.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Tap to view details & QR code →",
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
