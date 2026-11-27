/**
 * AdminProfilesScreen.kt
 * 
 * Purpose: Provides an administrative moderation view for searching, filtering, and updating user account statuses (US 03.02.01).
 * 
 * Issues / Outstanding Issues:
 * - Suspending a profile does not automatically revoke active session tokens in non-Firebase dev environments.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.AccountStatus
import com.example.solace_temp.models.UserRole

/**
 * Displays the administrator directory for browsing all registered profiles.
 * Implements search, role-based filtering, and date sorting with virtual scrolling
 * to handle high data volumes gracefully.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfilesScreen(
    repository: AppRepository,
    onSelectProfile: (String) -> Unit
) {
    val profilesList by repository.profiles.collectAsState(initial = emptyList())
    val currentProfile by repository.currentProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<AccountStatus?>(null) }
    var sortDescending by remember { mutableStateOf(true) }

    var showDateFilterDialog by remember { mutableStateOf(false) }
    var dateFilterStart by remember { mutableStateOf<Long?>(null) }
    var dateFilterEnd by remember { mutableStateOf<Long?>(null) }

    // Acceptance Criteria: Only users with verified administrator credentials can access
    if (!currentProfile.isAdmin || currentProfile.role != UserRole.ADMIN) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Access Denied: Administrator credentials required.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    // Apply Search, Filter, and Sort criteria
    val filteredAndSorted = profilesList.filter { user ->
        val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.deviceId.contains(searchQuery, ignoreCase = true)

        val matchesRole = selectedRoleFilter == null || user.role == selectedRoleFilter
        val matchesStatus = selectedStatusFilter == null || user.accountStatus == selectedStatusFilter

        val matchesDate = if (dateFilterStart != null && dateFilterEnd != null) {
            user.registrationDateMillis in dateFilterStart!!..dateFilterEnd!!
        } else true

        matchesSearch && matchesRole && matchesStatus && matchesDate
    }.let { list ->
        if (sortDescending) list.sortedByDescending { it.registrationDateMillis }
        else list.sortedBy { it.registrationDateMillis }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEDE8F8)).padding(16.dp)) {
        Text("Admin: User Directory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // Search by name, email, or user identifier
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, email, or ID...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Filter options for role, status, and creation date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == null && selectedStatusFilter == null && dateFilterStart == null,
                    onClick = {
                        selectedRoleFilter = null
                        selectedStatusFilter = null
                        dateFilterStart = null
                        dateFilterEnd = null
                    },
                    label = { Text("All") }
                )

                FilterChip(
                    selected = dateFilterStart != null,
                    onClick = { showDateFilterDialog = true },
                    label = { Text(if (dateFilterStart != null) "Date Filtered" else "Filter Date") }
                )

                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRoleFilter == role,
                        onClick = { selectedRoleFilter = role },
                        label = { Text(role.name) }
                    )
                }

                AccountStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status.name) }
                    )
                }
            }

            IconButton(onClick = { sortDescending = !sortDescending }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Sort by Registration Date",
                    tint = if (sortDescending) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Empty state feedback for no matches
        if (filteredAndSorted.isEmpty()) {
            Text(
                text = if (searchQuery.isBlank() && selectedRoleFilter == null && selectedStatusFilter == null && dateFilterStart == null)
                    "No profiles registered in the platform."
                else
                    "No profiles match search/filter criteria.",
                modifier = Modifier.padding(16.dp)
            )
        }

        // Virtual scrolling for large user bases
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredAndSorted) { user ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    // Navigation on click
                    modifier = Modifier.fillMaxWidth().clickable { onSelectProfile(user.deviceId) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Key identity details
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.name.ifBlank { "Unknown User" }, fontWeight = FontWeight.Bold)

                            val statusColor = when(user.accountStatus) {
                                AccountStatus.ACTIVE -> Color(0xFF2E7D32)
                                AccountStatus.INACTIVE -> Color(0xFF757575)
                                AccountStatus.SUSPENDED -> Color(0xFFD32F2F)
                            }
                            Text(user.accountStatus.name, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                        Text("ID: ${user.deviceId}")
                        if (user.email.isNotBlank()) Text("Email: ${user.email}")
                        Text("Role: ${user.role.name} · Registered: ${formatDateMillis(user.registrationDateMillis)}")
                        Spacer(Modifier.height(4.dp))
                        Text("Tap for detailed account overview →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Date Range Picker Dialog for filtering by creation date
    if (showDateFilterDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = dateFilterStart,
            initialSelectedEndDateMillis = dateFilterEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDateFilterDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    dateFilterStart = dateState.selectedStartDateMillis
                    dateFilterEnd = dateState.selectedEndDateMillis?.plus(86399999) // Add 24h to include the full end day
                    showDateFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dateFilterStart = null
                    dateFilterEnd = null
                    showDateFilterDialog = false
                }) { Text("Clear") }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}
