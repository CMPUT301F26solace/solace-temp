package com.example.solace_temp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.components.StackedParallaxCard
import kotlinx.coroutines.launch
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral500
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.Neutral700
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.OwnerBackground
import com.example.solace_temp.ui.theme.SolaceCardShape
import com.example.solace_temp.ui.theme.SolaceDivider
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusPill
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceCardColors
import com.example.solace_temp.ui.theme.solaceCardElevation
import com.example.solace_temp.ui.theme.statusColors

private enum class OwnerLotteryFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    UPCOMING("Upcoming"),
    BOOKING("Booking"),
    FINISHED("Finished")
}

private data class OwnerLotteryEntry(
    val lottery: Lottery,
    val campsiteNames: String,
    val status: String
)

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

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(OwnerLotteryFilter.ALL) }
    val listState = rememberLazyListState()
    val filterScrollState = rememberScrollState()
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    // Show only lotteries whose campsites actually belong to this owner.
    val ownedCampsitesById = remember(campsites, profile.deviceId) {
        campsites.filter { it.ownerId == profile.deviceId }.associateBy { it.id }
    }
    val ownedLotteries = remember(lotteries, ownedCampsitesById, profile.deviceId) {
        lotteries.filter { lottery ->
            lottery.ownerId == profile.deviceId &&
                lottery.campsiteIds.isNotEmpty() &&
                lottery.campsiteIds.all { it in ownedCampsitesById }
        }
    }

    val allEntries = remember(ownedLotteries, ownedCampsitesById) {
        ownedLotteries.map { lottery ->
            val associatedCampsites = lottery.campsiteIds.mapNotNull { ownedCampsitesById[it] }
            val campsiteNames = associatedCampsites
                .joinToString(", ") { it.name }
                .ifBlank { "Assigned Campsites" }
            val status = getLotteryStatus(lottery, associatedCampsites.firstOrNull())
            OwnerLotteryEntry(
                lottery = lottery,
                campsiteNames = campsiteNames,
                status = status
            )
        }
    }

    val activeNowCount = remember(allEntries) {
        allEntries.count { it.status == "Active" }
    }
    val inBookingCount = remember(allEntries) {
        allEntries.count { it.status == "Booking" }
    }
    val totalWaitlistCount = remember(ownedLotteries) {
        ownedLotteries.sumOf { it.waitingListEntrantIds.size }
    }

    val filteredEntries = remember(allEntries, searchQuery, selectedFilter) {
        val query = searchQuery.trim()
        allEntries.filter { entry ->
            val matchesQuery = query.isEmpty() ||
                entry.lottery.name.contains(query, ignoreCase = true) ||
                entry.campsiteNames.contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                OwnerLotteryFilter.ALL -> true
                OwnerLotteryFilter.ACTIVE -> entry.status == "Active"
                OwnerLotteryFilter.UPCOMING -> entry.status == "Upcoming"
                OwnerLotteryFilter.BOOKING -> entry.status == "Booking"
                OwnerLotteryFilter.FINISHED -> entry.status == "Finished"
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OwnerBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = SolaceTokens.SpaceLG,
                vertical = SolaceTokens.SpaceLG
            ),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
        ) {
        item(key = "lottery_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)) {
                Text(
                    text = "LOTTERY HQ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SolaceRed
                )
                Text(
                    text = "Manage Lotteries",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral900
                )
                Text(
                    text = "View and manage waiting list lotteries across your registered campsites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral600
                )
                Spacer(Modifier.height(SolaceTokens.SpaceSM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Button(
                        onClick = onOpenCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_lottery_calendar"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolaceRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(SolaceTokens.SpaceSM))
                        Text("Calendar", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onOpenReservations,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_lottery_reservations"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CardSurface,
                            contentColor = Neutral900
                        ),
                        border = solaceCardBorder(),
                        shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(SolaceTokens.SpaceSM))
                        Text("Reservations", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item(key = "lottery_kpis", contentType = "kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
            ) {
                LotteryKpiCard(
                    label = "Active Now",
                    value = activeNowCount.toString(),
                    accentColor = SolaceRed,
                    modifier = Modifier.weight(1f)
                )
                LotteryKpiCard(
                    label = "In Booking",
                    value = inBookingCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                LotteryKpiCard(
                    label = "Waitlist",
                    value = totalWaitlistCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "lottery_search_filters", contentType = "search_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search lotteries or campsites...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral500
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Neutral500
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Neutral600
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface,
                        focusedBorderColor = SolaceRed,
                        unfocusedBorderColor = HairlineBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_lotteries_search")
                )

                // Bounded 5-pill filter bar — standard Row with horizontalScroll avoids LazyRow overhead.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                ) {
                    OwnerLotteryFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                            color = if (selected) Neutral900 else CardSurface,
                            border = if (selected) null else solaceCardBorder(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                                .clickable { selectedFilter = filter }
                                .testTag("lottery_filter_${filter.name}")
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) Color.White else Neutral700,
                                modifier = Modifier.padding(
                                    horizontal = SolaceTokens.SpaceMD,
                                    vertical = SolaceTokens.SpaceSM
                                )
                            )
                        }
                    }
                }
            }
        }

        if (ownedLotteries.isEmpty()) {
            item(key = "lottery_empty_state", contentType = "empty_state") {
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SolaceTokens.SpaceXXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "No Lotteries",
                            tint = SolaceRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No lotteries configured yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Create a lottery from any of your campsite details pages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (filteredEntries.isEmpty()) {
            item(key = "lottery_filter_empty", contentType = "empty_filter_state") {
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SolaceTokens.SpaceXXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                    ) {
                        Text(
                            text = "No matching lotteries found.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Neutral900
                        )
                        Text(
                            text = "Adjust your search query or reset the status filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral600,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = filteredEntries,
                key = { _, entry -> entry.lottery.id },
                contentType = { _, _ -> "owner_lottery_card" }
            ) { index, entry ->
                StackedParallaxCard(
                    index = index,
                    cardCount = filteredEntries.size,
                    stickyTopOffset = SolaceTokens.SpaceLG,
                    reduceMotion = reduceMotion
                ) {
                    OwnerLotteryFeedCard(
                        entry = entry,
                        onSelectLottery = onSelectLottery,
                        onDrawWinners = { repository.drawLotteryWinners(entry.lottery.id) }
                    )
                }
            }
        }
    }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(SolaceTokens.SpaceLG)
        ) {
            Surface(
                shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                color = Neutral900,
                shadowElevation = SolaceTokens.ElevationHigh,
                modifier = Modifier
                    .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                    .testTag("btn_owner_lotteries_scroll_top")
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = SolaceTokens.SpaceMD,
                        vertical = SolaceTokens.SpaceSM
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Top",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LotteryKpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Neutral900
) {
    Surface(
        modifier = modifier,
        shape = SolaceInnerCardShape,
        color = CardSurface,
        border = solaceCardBorder(),
        shadowElevation = SolaceTokens.ElevationLow
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = SolaceTokens.SpaceMD,
                vertical = SolaceTokens.SpaceMD
            ),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXXS)
        ) {
            TabularText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OwnerLotteryFeedCard(
    entry: OwnerLotteryEntry,
    onSelectLottery: (String) -> Unit,
    onDrawWinners: () -> Unit
) {
    val lottery = entry.lottery
    val status = entry.status
    val (statusBg, statusTextColor) = statusColors(status)
    val now = System.currentTimeMillis()
    val isPastDeadline = (lottery.registrationEndMillis ?: Long.MAX_VALUE) <= now

    Card(
        shape = SolaceCardShape,
        colors = solaceCardColors(),
        border = solaceCardBorder(),
        elevation = solaceCardElevation(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectLottery(lottery.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(SolaceTokens.SpaceXL)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXXS)
                ) {
                    Text(
                        text = lottery.name.ifBlank { "Campsite Lottery" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Neutral900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "📍 ${entry.campsiteNames}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                StatusPill(status = status)
            }

            SolaceDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = statusBg,
                        shape = SolaceInnerCardShape
                    )
                    .border(
                        width = SolaceTokens.HairlineWidth,
                        color = statusTextColor.copy(alpha = 0.20f),
                        shape = SolaceInnerCardShape
                    )
                    .padding(SolaceTokens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registration Window",
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral600
                    )
                    TabularText(
                        text = "${lottery.waitingListEntrantIds.size} Enrolled",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor
                    )
                }
                TabularText(
                    text = "${lottery.registrationStart} to ${lottery.registrationEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Neutral900,
                    maxLines = 2
                )
                Spacer(Modifier.height(SolaceTokens.SpaceXXS))
                Text(
                    text = "Booking Window",
                    style = MaterialTheme.typography.labelSmall,
                    color = Neutral600
                )
                TabularText(
                    text = "${lottery.bookingStart} to ${lottery.bookingEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Neutral900,
                    maxLines = 2
                )
            }

            if (isPastDeadline && !lottery.isDrawn) {
                Button(
                    onClick = onDrawWinners,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolaceRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Draw Winners",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                    Text("Draw Winners Now", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabularText(
                    text = "Max Winners: ${lottery.maxWinners}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Neutral700
                )
                Text(
                    text = "Tap for Details & Draw Controls →",
                    style = MaterialTheme.typography.labelMedium,
                    color = SolaceRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
