package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

sealed class AppScreen(val route: String) {
    object EntrantNotifications : AppScreen("entrant_notifications")
    object EntrantReservations : AppScreen("entrant_reservations")
    object CampsiteReservation : AppScreen("campsite_reservation/{campsiteId}/{notificationId}") {
        fun createRoute(campsiteId: String, notificationId: String) = "campsite_reservation/$campsiteId/$notificationId"
    }
    object OwnerCalendar : AppScreen("owner_calendar")
    object OwnerReservations : AppScreen("owner_reservations")
}

// =========================================================================
// PLACEHOLDER SCREEN FOR TEAMMATES TO PLUG IN THEIR STORIES
// =========================================================================
/**
 * A generic placeholder screen component used for indicating missing functionality
 * or screens meant to be implemented by other teammates.
 * 
 * @param title The title representing the feature.
 * @param userStoriesCovered The agile user stories related to this placeholder.
 */
@Composable
fun TeammatePlaceholderScreen(title: String, userStoriesCovered: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDE8F8))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Teammate Story Slot: $userStoriesCovered", color = Color.Gray)
            }
        }
    }
}
