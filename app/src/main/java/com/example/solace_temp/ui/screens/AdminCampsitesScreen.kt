/**
 * AdminCampsitesScreen.kt
 * 
 * Purpose: Provides an administrative management view for inspecting, filtering, and moderating registered campsites across the system (US 03.01.01).
 * 
 * Issues / Outstanding Issues:
 * - Campsite deletion requires explicit confirmation override if active reservations or open lotteries exist.
 */
package com.example.solace_temp.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.CampsiteRemovalDependencies
import com.example.solace_temp.models.CampsiteStatus
import com.example.solace_temp.models.UserRole
import kotlinx.coroutines.launch

/**
 * Displays the administrator registry view for browsing and moderating all platform campsites
 * (US 03.04.01, US 03.01.01).
 *
 * Enforces verified administrator privileges, supports multi-criteria search/filtering/sorting,
 * and provides a safe campsite removal workflow with dependency inspection, explicit override
 * confirmation for active reservations or open lotteries, and Snackbar completion feedback.
 *
 * @param repository The data repository exposing campsite, lottery, and profile state flows.
 * @param onSelectCampsite Callback triggered with a campsite ID when an admin taps a card to inspect it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCampsitesScreen(
    repository: AppRepository,
    onSelectCampsite: (String) -> Unit
) {
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()

    // Search, filter, and sort UI state
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<CampsiteStatus?>(null) }
    var requireActiveLottery by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(true) }

    // Date range filter dialog state
    var showDateFilterDialog by remember { mutableStateOf(false) }
    var dateFilterStart by remember { mutableStateOf<Long?>(null) }
    var dateFilterEnd by remember { mutableStateOf<Long?>(null) }

    // Administrative removal dialog and feedback state (US 03.01.01)
    var campsitePendingRemoval by remember { mutableStateOf<Campsite?>(null) }
    var pendingDependencies by remember { mutableStateOf<CampsiteRemovalDependencies?>(null) }
    var isRemoving by remember { mutableStateOf(false) }

    // Snackbar state for visual confirmation or error feedback after removal
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Role-based access guard: requires both verified admin privilege flag and active ADMIN role
    if (!profile.isAdmin || profile.role != UserRole.ADMIN) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Access Denied: Administrator credentials required.",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Apply search, status, lottery association, creation date range, and chronological sorting
    val filteredAndSorted = campsites.filter { site ->
        val matchesSearch = site.name.contains(searchQuery, ignoreCase = true) ||
                site.location.locationName.contains(searchQuery, ignoreCase = true) ||
                site.ownerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || site.status == selectedStatusFilter
        val matchesLottery = !requireActiveLottery || lotteries.any { it.campsiteIds.contains(site.id) }

        val matchesDate = if (dateFilterStart != null && dateFilterEnd != null) {
            site.dateCreatedMillis in dateFilterStart!!..dateFilterEnd!!
        } else true

        matchesSearch && matchesStatus && matchesLottery && matchesDate
    }.let { list ->
        if (sortDescending) list.sortedByDescending { it.dateCreatedMillis }
        else list.sortedBy { it.dateCreatedMillis }
    }

    // Scaffold provides a dedicated SnackbarHost for post-deletion confirmation feedback
    Scaffold(
        containerColor = Color(0xFFEDE8F8),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Admin: Campsite Registry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, location, or owner ID...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Horizontal filter chips and chronological sort toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null && !requireActiveLottery && dateFilterStart == null,
                        onClick = {
                            selectedStatusFilter = null
                            requireActiveLottery = false
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

                    FilterChip(
                        selected = requireActiveLottery,
                        onClick = { requireActiveLottery = !requireActiveLottery },
                        label = { Text("Has Lottery") }
                    )

                    CampsiteStatus.entries.forEach { status ->
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
                        contentDescription = "Sort by Date",
                        tint = if (sortDescending) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Empty state feedback for empty registry vs. unmatched filters
            if (filteredAndSorted.isEmpty()) {
                Text(
                    text = if (searchQuery.isBlank() && selectedStatusFilter == null && !requireActiveLottery && dateFilterStart == null)
                        "No campsites registered in the platform."
                    else
                        "No campsites match search/filter criteria.",
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Virtualized list of registered campsites
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredAndSorted, key = { it.id }) { site ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCampsite(site.id) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(site.name, fontWeight = FontWeight.Bold)

                                val statusColor = when (site.status) {
                                    CampsiteStatus.ACTIVE -> Color(0xFF2E7D32)
                                    CampsiteStatus.DRAFT -> Color(0xFF757575)
                                    CampsiteStatus.FLAGGED -> Color(0xFFD32F2F)
                                }
                                Text(site.status.name, color = statusColor, fontWeight = FontWeight.Bold)
                            }

                            Text("Owner ID: ${site.ownerId}")
                            Text("📍 ${site.location.locationName} · Capacity: ${site.maxOccupancy}")
                            Spacer(Modifier.height(8.dp))

                            // Action row: navigation hint on left, verified-admin removal trigger on right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap to manage/moderate →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TextButton(
                                    onClick = {
                                        // Inspect active reservations, open lotteries, media, and QR codes before prompting
                                        pendingDependencies = repository.getCampsiteRemovalDependencies(site.id)
                                        campsitePendingRemoval = site
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove ${site.name}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation and dependency override dialog for administrative campsite removal (US 03.01.01)
    val targetSite = campsitePendingRemoval
    val deps = pendingDependencies
    if (targetSite != null && deps != null) {
        AdminRemoveCampsiteDialog(
            campsite = targetSite,
            dependencies = deps,
            isRemoving = isRemoving,
            onDismiss = {
                campsitePendingRemoval = null
                pendingDependencies = null
            },
            onConfirmRemoval = { forceOverride ->
                isRemoving = true
                val removedName = targetSite.name
                repository.removeCampsiteAsAdmin(
                    campsiteId = targetSite.id,
                    forceOverride = forceOverride
                ) { result ->
                    isRemoving = false
                    campsitePendingRemoval = null
                    pendingDependencies = null

                    // Show visual confirmation or error feedback via Snackbar
                    coroutineScope.launch {
                        result.onSuccess {
                            snackbarHostState.showSnackbar(
                                message = "Campsite \"$removedName\" was successfully removed."
                            )
                        }.onFailure { error ->
                            snackbarHostState.showSnackbar(
                                message = error.message ?: "Failed to remove campsite."
                            )
                        }
                    }
                }
            }
        )
    }

    // Dialog popup for selecting the creation date range filter
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
                    // Add roughly 24 hours to the end date so it includes the full closing day
                    dateFilterEnd = dateState.selectedEndDateMillis?.plus(86399999)
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
