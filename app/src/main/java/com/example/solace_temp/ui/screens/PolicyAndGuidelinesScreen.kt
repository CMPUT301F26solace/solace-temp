/**
 * PolicyAndGuidelinesScreen.kt
 * 
 * Purpose: Composable screen displaying app terms, fair lottery rules, cancellation policies, and user guidelines.
 * 
 * Issues / Outstanding Issues:
 * - Content is static markdown-styled text for easy review.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// =========================================================================
// POLICY AND GUIDELINES SCREEN
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyAndGuidelinesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Policy & Guidelines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFEDE8F8))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PolicyCard(
                title = "1. Community Guidelines & General Policy",
                content = "Welcome to Solace Campsite. Our platform connects outdoor enthusiasts with premium public and private campsites. All users are expected to treat nature, campsite owners, and fellow entrants with respect. Harassment, fraudulent bookings, or misuse of promotional codes will result in immediate suspension."
            )

            PolicyCard(
                title = "2. Campsite Usage Rules",
                content = "• Quiet Hours: Strictly enforced between 11:00 PM and 7:00 AM.\n" +
                        "• Campfires: Permitted only in designated fire pits. Never leave a fire unattended.\n" +
                        "• Wildlife Safety: Keep all food in bear-proof lockers or secure vehicles. Do not feed wildlife.\n" +
                        "• Cleanliness: Pack out all garbage. Leave your campsite as clean as or cleaner than you found it."
            )

            PolicyCard(
                title = "3. Cancellation & Refund Policies",
                content = "• Standard Cancellation: Full refund if cancelled at least 7 days prior to check-in. 50% refund if cancelled 3 to 6 days prior.\n" +
                        "• Weather & Fire Bans: 100% full refund issued automatically in the event of an official government fire ban, forest fire warning, or severe weather emergency affecting your dates."
            )

            PolicyCard(
                title = "4. Lottery Terms & Conditions",
                content = "• Waitlist Entry: Entrants may join active campsite lotteries during the specified registration window.\n" +
                        "• Winner Selection: Winners are drawn randomly and notified via in-app notifications.\n" +
                        "• Acceptance Window: Winners must accept their reservation invitation within 48 hours, or the spot will be offered to runner-up entrants.\n" +
                        "• Capacity Limits: Some lotteries enforce waitlist capacity caps to ensure fair access."
            )
        }
    }
}

@Composable
fun PolicyCard(title: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
