package com.example.solace_temp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.components.StackedParallaxCard
import kotlinx.coroutines.launch
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral200
import com.example.solace_temp.ui.theme.Neutral300
import com.example.solace_temp.ui.theme.Neutral400
import com.example.solace_temp.ui.theme.Neutral50
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

private enum class OwnerCampsiteFilter(val label: String) {
    ALL("All"),
    ACTIVE_LOTTERY("Active Lottery"),
    NO_LOTTERY("No Lottery")
}

private data class OwnerCampsiteEntry(
    val site: Campsite,
    val activeLottery: Lottery?,
    val status: String,
    val ratingLabel: String
)

/**
 * Architectural topographic fallback placeholder for campsites without a cover photo.
 * Uses [Modifier.drawWithCache] so contour paths and grid coordinates allocate only on resize.
 */
@Composable
internal fun TopographicImageFallback(
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    label: String = "No Image Available",
    subtitle: String? = null
) {
    Box(
        modifier = modifier
            .background(Neutral50)
            .drawWithCache {
                val w = size.width
                val h = size.height

                val gridStroke = Stroke(width = 1.dp.toPx())
                val contourStroke = Stroke(width = 1.25.dp.toPx())
                val fineContourStroke = Stroke(width = 0.85.dp.toPx())

                // Architectural coordinate grid lines
                val gridPath = Path().apply {
                    val cols = 4
                    val rows = 3
                    for (c in 1 until cols) {
                        val x = w * c / cols
                        moveTo(x, 0f)
                        lineTo(x, h)
                    }
                    for (r in 1 until rows) {
                        val y = h * r / rows
                        moveTo(0f, y)
                        lineTo(w, y)
                    }
                }

                // Organic topographic elevation contour rings
                val outerContour = Path().apply {
                    moveTo(-w * 0.05f, h * 0.72f)
                    cubicTo(w * 0.22f, h * 0.42f, w * 0.48f, h * 0.82f, w * 0.78f, h * 0.34f)
                    cubicTo(w * 0.90f, h * 0.16f, w * 1.02f, h * 0.28f, w * 1.08f, h * 0.45f)
                }
                val midContour = Path().apply {
                    moveTo(-w * 0.05f, h * 0.52f)
                    cubicTo(w * 0.25f, h * 0.20f, w * 0.52f, h * 0.62f, w * 0.84f, h * 0.14f)
                    cubicTo(w * 0.95f, h * 0.04f, w * 1.04f, h * 0.12f, w * 1.08f, h * 0.22f)
                }
                val innerContour = Path().apply {
                    moveTo(w * 0.14f, h * 0.96f)
                    cubicTo(w * 0.38f, h * 0.68f, w * 0.66f, h * 0.94f, w * 0.96f, h * 0.58f)
                }

                onDrawBehind {
                    drawPath(gridPath, color = HairlineBorder, style = gridStroke)
                    drawPath(outerContour, color = Neutral300, style = contourStroke)
                    drawPath(midContour, color = Neutral300.copy(alpha = 0.75f), style = fineContourStroke)
                    drawPath(innerContour, color = Neutral200, style = fineContourStroke)

                    // Subtle architectural crosshair at center
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val tick = 8.dp.toPx()
                    drawLine(
                        color = Neutral400.copy(alpha = 0.55f),
                        start = Offset(cx - tick, cy),
                        end = Offset(cx + tick, cy),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Neutral400.copy(alpha = 0.55f),
                        start = Offset(cx, cy - tick),
                        end = Offset(cx, cy + tick),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Surface(
                shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                color = CardSurface.copy(alpha = 0.92f),
                border = BorderStroke(SolaceTokens.HairlineWidth, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = SolaceTokens.SpaceLG,
                        vertical = SolaceTokens.SpaceMD
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXXS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terrain,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Neutral700,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Neutral500
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Terrain,
                contentDescription = "Placeholder Image",
                tint = Neutral500,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

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

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(OwnerCampsiteFilter.ALL) }
    val listState = rememberLazyListState()
    val reduceMotion = LocalReduceMotion.current
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    val myCampsites = remember(campsites, profile.deviceId) {
        campsites.filter { it.ownerId == profile.deviceId }
    }

    val allEntries = remember(myCampsites, lotteries) {
        myCampsites.map { site ->
            val activeLottery = lotteries.find {
                it.campsiteIds.contains(site.id) &&
                    getLotteryStatus(it, site) != "Finished" &&
                    getLotteryStatus(it, site) != "N/A"
            }
            val status = getLotteryStatus(activeLottery, site)
            val ratingLabel = if (site.rating > 0.0) "★ ${site.rating}" else "Not yet rated"
            OwnerCampsiteEntry(
                site = site,
                activeLottery = activeLottery,
                status = status,
                ratingLabel = ratingLabel
            )
        }
    }

    val inLotteryCount = remember(allEntries) {
        allEntries.count { it.activeLottery != null }
    }
    val totalCapacity = remember(myCampsites) {
        myCampsites.sumOf { it.maxOccupancy }
    }

    val filteredEntries = remember(allEntries, searchQuery, selectedFilter) {
        val query = searchQuery.trim()
        allEntries.filter { entry ->
            val matchesQuery = query.isEmpty() ||
                entry.site.name.contains(query, ignoreCase = true) ||
                entry.site.location.locationName.contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                OwnerCampsiteFilter.ALL -> true
                OwnerCampsiteFilter.ACTIVE_LOTTERY -> entry.activeLottery != null
                OwnerCampsiteFilter.NO_LOTTERY -> entry.activeLottery == null
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
        item(key = "owner_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)) {
                Text(
                    text = "My Registered Campsites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Neutral900
                )
                Text(
                    text = "Monitor active lottery windows, occupancy capacity, and access passes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral600
                )
                Spacer(Modifier.height(SolaceTokens.SpaceXS))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Button(
                        onClick = onOpenCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_owner_calendar"),
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
                    Button(
                        onClick = onOpenReservations,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_owner_reservations"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolaceRed,
                            contentColor = Color.White
                        ),
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

        item(key = "owner_kpis", contentType = "kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
            ) {
                OwnerKpiCard(
                    label = "Properties",
                    value = myCampsites.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                OwnerKpiCard(
                    label = "In Lotteries",
                    value = inLotteryCount.toString(),
                    accentColor = SolaceRed,
                    modifier = Modifier.weight(1f)
                )
                OwnerKpiCard(
                    label = "Capacity",
                    value = totalCapacity.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "owner_search_filters", contentType = "search_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search registered properties or parks...",
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
                        .testTag("owner_campsites_search")
                )

                // Bounded 3-chip filter bar — standard Row avoids LazyRow overhead.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                ) {
                    OwnerCampsiteFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                            color = if (selected) Neutral900 else CardSurface,
                            border = if (selected) null else solaceCardBorder(),
                            modifier = Modifier
                                .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                                .clickable { selectedFilter = filter }
                                .testTag("owner_filter_${filter.name}")
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

        if (myCampsites.isEmpty()) {
            item(key = "owner_empty_state", contentType = "empty_state") {
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
                        TopographicImageFallback(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(SolaceInnerCardShape)
                                .border(SolaceTokens.HairlineWidth, HairlineBorder, SolaceInnerCardShape),
                            showLabel = false
                        )
                        Text(
                            text = "You haven't registered any campsites yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral700,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onCreateNew,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(SolaceTokens.SpaceSM))
                            Text("Add a Campsite")
                        }
                    }
                }
            }
        } else if (filteredEntries.isEmpty()) {
            item(key = "owner_filter_empty", contentType = "empty_filter_state") {
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
                            text = "No matching campsites found.",
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral900
                        )
                        Text(
                            text = "Adjust your search query or reset the lottery filter.",
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
                key = { _, entry -> entry.site.id },
                contentType = { _, _ -> "registered_campsite_card" }
            ) { index, entry ->
                StackedParallaxCard(
                    index = index,
                    cardCount = filteredEntries.size,
                    stickyTopOffset = SolaceTokens.SpaceLG,
                    reduceMotion = reduceMotion
                ) {
                    OwnerCampsiteFeedCard(
                        entry = entry,
                        onSelectCampsite = onSelectCampsite
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
                    .testTag("btn_owner_campsites_scroll_top")
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
private fun OwnerKpiCard(
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
private fun OwnerCampsiteFeedCard(
    entry: OwnerCampsiteEntry,
    onSelectCampsite: (String) -> Unit
) {
    val site = entry.site
    val activeLottery = entry.activeLottery
    val status = entry.status
    val (statusBg, statusTextColor) = statusColors(status)

    Card(
        shape = SolaceCardShape,
        colors = solaceCardColors(),
        border = solaceCardBorder(),
        elevation = solaceCardElevation(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCampsite(site.id) }
    ) {
        Column(
            modifier = Modifier
                .padding(SolaceTokens.SpaceXL)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(SolaceTokens.RadiusLG))
                        .border(
                            SolaceTokens.HairlineWidth,
                            HairlineBorder,
                            RoundedCornerShape(SolaceTokens.RadiusLG)
                        ),
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
                        TopographicImageFallback(
                            modifier = Modifier.fillMaxSize(),
                            showLabel = false
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
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
                            color = Neutral900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(SolaceTokens.SpaceSM))
                        TabularText(
                            text = "$${site.pricePerNight.toInt()}/night",
                            style = MaterialTheme.typography.titleSmall,
                            color = SolaceRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "📍 ${site.location.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TabularText(
                        text = "Max ${site.maxOccupancy} guests · ${entry.ratingLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral600
                    )
                }
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
                    val lotteryPrefix = if (activeLottery != null && activeLottery.campsiteIds.size > 1) {
                        "Multi Lottery:"
                    } else {
                        "Lottery:"
                    }
                    Text(
                        text = if (activeLottery != null) "$lotteryPrefix ${activeLottery.name}" else "Lottery Status",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                    StatusPill(status = status)
                }

                if (activeLottery != null) {
                    TabularText(
                        text = "Registration Window: ${activeLottery.registrationStart} to ${activeLottery.registrationEnd}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral700,
                        maxLines = 2
                    )
                    TabularText(
                        text = "Booking Window: ${activeLottery.bookingStart} to ${activeLottery.bookingEnd}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral700,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(SolaceTokens.SpaceXXS))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TabularText(
                            text = "Enrolled: ${activeLottery.waitingListEntrantIds.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Neutral900
                        )
                        TabularText(
                            text = "Max Winners: ${activeLottery.maxWinners}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Neutral900
                        )
                    }
                } else {
                    Text(
                        text = "Configure a registration period for this campsite from details view.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral600
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
                    color = SolaceRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

