/**
 * LotteryDetailsScreen.kt
 *
 * Purpose: Composable screen displaying comprehensive details of a lottery, owner controls,
 * manual draw/redraw triggers, scratch-to-reveal winner unveiling, and testing fast-forwarding.
 *
 * Issues / Outstanding Issues:
 * - Manual redraw is enabled only when unfulfilled winner slots remain after initial draw.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.components.ScratchRevealCanvas
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.ErrorRed
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral100
import com.example.solace_temp.ui.theme.Neutral300
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
import com.example.solace_temp.ui.theme.StatusActiveBg
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.StatusPill
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceCardColors
import com.example.solace_temp.ui.theme.solaceCardElevation
import com.example.solace_temp.ui.theme.statusColors
import kotlinx.coroutines.delay

private fun formatLotteryCountdown(targetMillis: Long, nowMillis: Long): String {
    val left = (targetMillis - nowMillis).coerceAtLeast(0L)
    val days = left / 86_400_000L
    val hours = (left / 3_600_000L) % 24
    val minutes = (left / 60_000L) % 60
    val seconds = (left / 1_000L) % 60
    return if (days > 0) {
        "${days}d ${hours}h ${minutes}m"
    } else if (hours > 0) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}

/**
 * Fast-forwards the lottery registration schedule for testing purposes.
 *
 * Sets the lottery registration end timestamp to 1 minute from [nowMillis] so campsite
 * owners can immediately test the drawing workflow. Simultaneously updates the registration
 * date display strings to reflect the new schedule, while leaving the campsite booking
 * availability dates intact as originally configured.
 *
 * @param lottery The [Lottery] to be updated for expedited testing.
 * @param nowMillis The current timestamp in milliseconds, defaults to [System.currentTimeMillis].
 * @return An updated [Lottery] instance with accelerated registration schedule and preserved booking dates.
 */
fun fastForwardLotteryForTesting(
    lottery: Lottery,
    nowMillis: Long = System.currentTimeMillis()
): Lottery {
    val newEndMillis = nowMillis + 60_000L
    val newStartMillis = if ((lottery.registrationStartMillis ?: 0L) > nowMillis) {
        nowMillis - 60_000L
    } else {
        lottery.registrationStartMillis ?: (nowMillis - 60_000L)
    }

    return lottery.copy(
        registrationStartMillis = newStartMillis,
        registrationEndMillis = newEndMillis,
        registrationStart = formatDateMillis(newStartMillis),
        registrationEnd = formatDateMillis(newEndMillis),
        isDrawn = false
    )
}

/**
 * Composable screen displaying detailed information about a specific lottery.
 *
 * @param lotteryId The unique ID of the lottery.
 * @param repository The data repository for fetching lottery data.
 * @param onBack Callback invoked when navigating back.
 * @param onSelectCampsite Callback invoked when an associated campsite card is tapped.
 * @param onViewWaitlist Callback invoked when the owner opens the lottery waitlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryDetailsScreen(
    lotteryId: String,
    repository: AppRepository,
    onBack: () -> Unit,
    onSelectCampsite: (String) -> Unit,
    onViewWaitlist: (String) -> Unit
) {
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val profiles by repository.profiles.collectAsState()
    val reservations by repository.reservations.collectAsState()
    val reduceMotion = LocalReduceMotion.current
    val lottery = remember(lotteries, lotteryId) {
        lotteries.find { it.id == lotteryId }
    }
    val scrollState = rememberScrollState()
    val isScrolled by remember {
        derivedStateOf { scrollState.value > 24 }
    }

    Scaffold(
        containerColor = OwnerBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = lottery?.name ?: "Lottery Details",
                            style = MaterialTheme.typography.titleLarge,
                            color = Neutral900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Neutral900
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OwnerBackground,
                        titleContentColor = Neutral900
                    )
                )
                if (isScrolled) {
                    SolaceDivider()
                }
            }
        }
    ) { padding ->
        if (lottery == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(OwnerBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Error: Lottery data not found.",
                        style = MaterialTheme.typography.titleMedium,
                        color = ErrorRed
                    )
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = SolaceRed),
                        shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val associatedCampsites = remember(campsites, lottery.campsiteIds) {
                campsites.filter { lottery.campsiteIds.contains(it.id) }
            }
            val latestLottery by rememberUpdatedState(lottery)
            var countdownNow by remember(lottery.id) { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(lottery.id) {
                while (true) {
                    val current = latestLottery
                    val end = current.registrationEndMillis
                    if (end != null && end <= countdownNow && !current.isDrawn) {
                        repository.drawLotteryWinners(current.id)
                    }
                    delay(1_000)
                    countdownNow = System.currentTimeMillis()
                }
            }

            val lotteryStatus = remember(lottery, associatedCampsites) {
                getLotteryStatus(lottery, associatedCampsites.firstOrNull())
            }
            val (statusBg, statusFg) = statusColors(lotteryStatus)
            val isOwnerOrAdmin = lottery.ownerId == profile.deviceId || (profile.isAdmin && profile.role == UserRole.ADMIN)
            val isPastDeadline by remember(lottery.registrationEndMillis) {
                derivedStateOf { (lottery.registrationEndMillis ?: Long.MAX_VALUE) <= countdownNow }
            }
            var showEditLotteryDialog by remember { mutableStateOf(false) }
            var isWinnersRevealed by rememberSaveable(lottery.id) { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(OwnerBackground)
                    .verticalScroll(scrollState)
                    .padding(horizontal = SolaceTokens.SpaceLG, vertical = SolaceTokens.SpaceSM),
                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
            ) {
                // Primary Dossier Card
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
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
                                    text = "LOTTERY DOSSIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Neutral500
                                )
                                Text(
                                    text = lottery.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }
                            Spacer(Modifier.width(SolaceTokens.SpaceSM))
                            StatusPill(status = lotteryStatus)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)) {
                            Text(
                                text = "Associated Campsites:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                            if (associatedCampsites.isEmpty()) {
                                Text(
                                    "None",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral600
                                )
                            } else {
                                associatedCampsites.forEach { site ->
                                    Surface(
                                        shape = SolaceInnerCardShape,
                                        color = Neutral50,
                                        border = solaceCardBorder(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SolaceInnerCardShape)
                                            .clickable { onSelectCampsite(site.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(SolaceTokens.SpaceMD)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    site.name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Neutral900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "📍 ${site.location.locationName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Neutral600,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                            Text(
                                                "View Details →",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SolaceRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SolaceDivider()

                        // Windows & Capacity Strip
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = statusBg,
                            border = BorderStroke(
                                SolaceTokens.HairlineWidth,
                                statusFg.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                            ) {
                                TabularText(
                                    text = "Registration Window: ${lottery.registrationStart} to ${lottery.registrationEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral900,
                                    maxLines = 2
                                )
                                TabularText(
                                    text = "Booking Window: ${lottery.bookingStart} to ${lottery.bookingEnd}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral900,
                                    maxLines = 2
                                )
                                SolaceDivider(Modifier.padding(vertical = SolaceTokens.SpaceXS))
                                TabularText(
                                    text = "Maximum Winners: ${lottery.maxWinners}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Neutral900
                                )
                                TabularText(
                                    text = "Waitlist Entrants Enrolled: ${lottery.waitingListEntrantIds.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Neutral900
                                )
                                TabularText(
                                    text = "Waitlist capacity: ${lottery.waitlistCapacity?.toString() ?: "Unlimited"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral700
                                )
                                Text(
                                    text = "Lottery Status: $lotteryStatus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusFg,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Countdown / Booking Metrics
                        when (lotteryStatus) {
                            "Upcoming" -> lottery.registrationStartMillis?.let { openingTime ->
                                LotteryCountdownCard(
                                    label = "Lottery opening in:",
                                    targetMillis = openingTime,
                                    accentColor = Neutral900,
                                    nowProvider = { countdownNow }
                                )
                            }
                            "Active" -> lottery.registrationEndMillis?.let { drawingTime ->
                                LotteryCountdownCard(
                                    label = "Lottery drawing in:",
                                    targetMillis = drawingTime,
                                    accentColor = SolaceRed,
                                    nowProvider = { countdownNow }
                                )
                            }
                            "Booking" -> {
                                val reservedEntrants = reservations.asSequence()
                                    .filter { it.campsiteId in lottery.campsiteIds && it.status == ReservationStatus.ACTIVE }
                                    .map { it.entrantId }
                                    .distinct()
                                    .count()
                                Surface(
                                    shape = SolaceInnerCardShape,
                                    color = Neutral50,
                                    border = solaceCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TabularText(
                                        text = "People who have reserved: $reservedEntrants",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Neutral900,
                                        modifier = Modifier.padding(SolaceTokens.SpaceMD)
                                    )
                                }
                                if (lottery.ownerId == profile.deviceId && lottery.isDrawn) {
                                    Button(
                                        onClick = { repository.redrawLottery(lottery.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SolaceRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                                    ) {
                                        Text("Redraw", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            else -> Unit
                        }

                        if (lottery.ownerId == profile.deviceId) {
                            OutlinedButton(
                                onClick = { onViewWaitlist(lottery.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CardSurface,
                                    contentColor = Neutral900
                                ),
                                border = solaceCardBorder(),
                                shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                            ) {
                                Text("View Waitlist", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (isOwnerOrAdmin && isPastDeadline && !lottery.isDrawn) {
                            Button(
                                onClick = { repository.drawLotteryWinners(lottery.id) },
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

                        if (isOwnerOrAdmin) {
                            OutlinedButton(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val updated = fastForwardLotteryForTesting(lottery, now)
                                    repository.updateLottery(updated)
                                    countdownNow = System.currentTimeMillis()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CardSurface,
                                    contentColor = Neutral700
                                ),
                                border = solaceCardBorder(),
                                shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Test Timer",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                Text("End Lottery in 1 Min (Test)", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (isOwnerOrAdmin) {
                            Button(
                                onClick = { showEditLotteryDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Neutral900,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Lottery",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                Text("Edit Lottery", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Winner Scratch-Reveal Card when the lottery draw has completed
                if (lottery.isDrawn) {
                    val profilesById = remember(profiles) { profiles.associateBy { it.deviceId } }
                    val revealInteraction = remember { MutableInteractionSource() }
                    val isRevealPressed by revealInteraction.collectIsPressedAsState()
                    val revealScale by animateFloatAsState(
                        targetValue = if (isRevealPressed && !reduceMotion) 0.96f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "reveal_winners_scale"
                    )

                    Card(
                        shape = SolaceCardShape,
                        colors = solaceCardColors(),
                        border = solaceCardBorder(),
                        elevation = solaceCardElevation(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(SolaceTokens.SpaceXL)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = SolaceRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "DRAWING RESULTS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Neutral500
                                        )
                                        Text(
                                            text = "Winner Reveal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Neutral900
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(SolaceTokens.RadiusSM),
                                    color = if (isWinnersRevealed) StatusActiveBg else Neutral100
                                ) {
                                    Text(
                                        text = if (isWinnersRevealed) "Revealed" else "Scratch to Reveal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isWinnersRevealed) StatusActiveGreen else Neutral700,
                                        modifier = Modifier.padding(
                                            horizontal = SolaceTokens.SpaceSM,
                                            vertical = SolaceTokens.SpaceXS
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 132.dp)
                                    .clip(SolaceInnerCardShape)
                                    .background(Neutral50)
                                    .border(
                                        SolaceTokens.HairlineWidth,
                                        HairlineBorder,
                                        SolaceInnerCardShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(SolaceTokens.SpaceLG),
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                                ) {
                                    if (lottery.invitedWinnerIds.isEmpty()) {
                                        Text(
                                            text = "No eligible entrants were drawn for this round.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Neutral600,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        lottery.invitedWinnerIds.forEachIndexed { idx, winnerId ->
                                            val winnerProfile = profilesById[winnerId]
                                            val displayName = winnerProfile?.name?.ifBlank { null } ?: winnerId
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(CardSurface, RoundedCornerShape(SolaceTokens.RadiusSM))
                                                    .border(
                                                        SolaceTokens.HairlineWidth,
                                                        HairlineBorder,
                                                        RoundedCornerShape(SolaceTokens.RadiusSM)
                                                    )
                                                    .padding(
                                                        horizontal = SolaceTokens.SpaceMD,
                                                        vertical = SolaceTokens.SpaceSM
                                                    ),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    TabularText(
                                                        text = "#${idx + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SolaceRed
                                                    )
                                                    Text(
                                                        text = displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Neutral900,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                StatusPill(status = "Active")
                                            }
                                        }
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !isWinnersRevealed,
                                    enter = EnterTransition.None,
                                    exit = fadeOut(animationSpec = tween(220)),
                                    modifier = Modifier.matchParentSize()
                                ) {
                                    ScratchRevealCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        overlayColor = Neutral300,
                                        strokeWidth = 64f,
                                        revealThreshold = 0.45f,
                                        onFullyRevealed = { isWinnersRevealed = true }
                                    )
                                }
                            }

                            if (!isWinnersRevealed) {
                                Text(
                                    text = "Scratch the foil overlay above or tap below to unveil drawn winners.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral600,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = { isWinnersRevealed = true },
                                    interactionSource = revealInteraction,
                                    shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                                    border = solaceCardBorder(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CardSurface,
                                        contentColor = Neutral900
                                    ),
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = revealScale
                                            scaleY = revealScale
                                        }
                                        .testTag("btn_reveal_winners_instantly")
                                        .semantics {
                                            contentDescription = "Reveal drawn lottery winners instantly"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                    Text(
                                        text = "Reveal Instantly",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = { isWinnersRevealed = false },
                                    modifier = Modifier.testTag("btn_cover_winners")
                                ) {
                                    Text(
                                        text = "Cover Winner Reveal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Neutral600
                                    )
                                }
                            }
                        }
                    }
                }

                if (showEditLotteryDialog) {
                    EditLotteryDialog(
                        lottery = lottery,
                        repository = repository,
                        onDismiss = { showEditLotteryDialog = false },
                        onSuccess = { showEditLotteryDialog = false }
                    )
                }

                Spacer(Modifier.height(SolaceTokens.SpaceLG))
            }
        }
    }
}

@Composable
private fun LotteryCountdownCard(
    label: String,
    targetMillis: Long,
    accentColor: Color,
    nowProvider: () -> Long
) {
    Surface(
        shape = SolaceInnerCardShape,
        color = Neutral50,
        border = solaceCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SolaceTokens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXXS)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Neutral600
            )
            TabularText(
                text = formatLotteryCountdown(targetMillis, nowProvider()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
