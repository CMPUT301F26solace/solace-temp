/**
 * AdminLogsTabScreen.kt
 *
 * Purpose: Unified tab container for administrative logs review, providing top tab bar navigation
 * between Communication Audit Logs (US 03.08.01, US 03.12.01) and System Notification Logs (US 03.07.01, US 03.11.01).
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.solace_temp.data.AppRepository

@Composable
fun AdminLogsTabScreen(
    repository: AppRepository,
    onBack: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Communication Logs", "Notification Logs")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth().testTag("admin_logs_tab_row")
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                    modifier = Modifier.testTag("admin_logs_tab_$index")
                )
            }
        }

        when (selectedTabIndex) {
            0 -> AdminMessageLogsScreen(repository = repository, onBack = onBack)
            1 -> AdminNotificationLogsScreen(repository = repository, onBack = onBack)
        }
    }
}
