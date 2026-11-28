/**
 * LotteryDialogs.kt
 *
 * Purpose: Clean #FFFFFF sheet/card modals for creating and editing lottery configurations,
 * featuring grouped form sections, custom animated selection toggles, clear window validation
 * states, and crimson primary CTA buttons.
 *
 * Issues / Outstanding Issues:
 * - Enforces minimum 1-week gap between registration close and booking period start.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.ErrorRed
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral300
import com.example.solace_temp.ui.theme.Neutral50
import com.example.solace_temp.ui.theme.Neutral500
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.Neutral700
import com.example.solace_temp.ui.theme.Neutral900
import com.example.solace_temp.ui.theme.SolaceDivider
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceRedSoft
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusActiveBg
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.WarningAmber
import com.example.solace_temp.ui.theme.WarningAmberBg
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceTactileScale

private enum class WindowValidationState {
    MISSING,
    INVALID,
    VALID
}

@Composable
private fun lotteryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CardSurface,
    unfocusedContainerColor = CardSurface,
    focusedBorderColor = SolaceRed,
    unfocusedBorderColor = HairlineBorder,
    focusedLabelColor = SolaceRed,
    unfocusedLabelColor = Neutral600,
    focusedTextColor = Neutral900,
    unfocusedTextColor = Neutral900
)

/**
 * Custom animated campsite selection row with GPU-composited thumb translation and press scale.
 */
@Composable
private fun AnimatedCampsiteToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }

    val trackColor by animateColorAsState(
        targetValue = if (checked) SolaceRed else Neutral300,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "toggle_track_color"
    )
    val rowBgColor by animateColorAsState(
        targetValue = if (checked) SolaceRedSoft.copy(alpha = 0.6f) else CardSurface,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "toggle_row_bg"
    )
    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toggle_thumb_x"
    )

    val travelPx = with(LocalDensity.current) { 18.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SolaceTokens.RadiusMD))
            .background(rowBgColor)
            .border(
                width = SolaceTokens.HairlineWidth,
                color = if (checked) SolaceRed.copy(alpha = 0.30f) else HairlineBorder,
                shape = RoundedCornerShape(SolaceTokens.RadiusMD)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = onToggle
            )
            .solaceTactileScale(interactionSource, 0.98f)
            .padding(horizontal = SolaceTokens.SpaceMD, vertical = SolaceTokens.SpaceMD),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
            color = Neutral900,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(SolaceTokens.SpaceSM))
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                .background(trackColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        translationX = thumbProgress * travelPx
                    }
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun WindowStatusSectionCard(
    title: String,
    windowSummary: String,
    validationState: WindowValidationState,
    statusBadgeText: String,
    helperMessage: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val borderColor = when (validationState) {
        WindowValidationState.INVALID -> SolaceRed.copy(alpha = 0.55f)
        WindowValidationState.VALID -> StatusActiveGreen.copy(alpha = 0.35f)
        WindowValidationState.MISSING -> HairlineBorder
    }
    val badgeBg = when (validationState) {
        WindowValidationState.INVALID -> SolaceRedSoft
        WindowValidationState.VALID -> StatusActiveBg
        WindowValidationState.MISSING -> WarningAmberBg
    }
    val badgeFg = when (validationState) {
        WindowValidationState.INVALID -> SolaceRed
        WindowValidationState.VALID -> StatusActiveGreen
        WindowValidationState.MISSING -> WarningAmber
    }

    Surface(
        shape = SolaceInnerCardShape,
        color = Neutral50,
        border = BorderStroke(SolaceTokens.HairlineWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SolaceTokens.SpaceMD),
            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neutral900
                )
                Surface(
                    shape = RoundedCornerShape(SolaceTokens.RadiusSM),
                    color = badgeBg
                ) {
                    Text(
                        text = statusBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeFg,
                        modifier = Modifier.padding(
                            horizontal = SolaceTokens.SpaceSM,
                            vertical = SolaceTokens.SpaceXXS
                        )
                    )
                }
            }

            TabularText(
                text = windowSummary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (validationState == WindowValidationState.INVALID) SolaceRed else Neutral700,
                maxLines = 2
            )

            if (!helperMessage.isNullOrBlank()) {
                Text(
                    text = helperMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (validationState == WindowValidationState.INVALID) ErrorRed else Neutral600
                )
            }

            if (actionLabel != null && onActionClick != null) {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                    border = solaceCardBorder(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardSurface,
                        contentColor = Neutral900
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = SolaceRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =========================================================================
// EDIT LOTTERY DIALOG
// =========================================================================
/**
 * Modal sheet/card allowing Campsite Owners or Admins to edit an existing lottery's configuration.
 *
 * @param lottery The [Lottery] entity being edited.
 * @param repository The data repository used to save lottery updates.
 * @param onDismiss Callback invoked when the dialog is dismissed or cancelled.
 * @param onSuccess Callback invoked when the lottery edit is completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLotteryDialog(
    lottery: Lottery,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val campsites by repository.campsites.collectAsState()
    val lotteries by repository.lotteries.collectAsState()

    var lotteryName by remember(lottery) { mutableStateOf(lottery.name) }
    var selectedCampsiteIds by remember(lottery) { mutableStateOf(lottery.campsiteIds) }
    var maxWinnersText by remember(lottery) { mutableStateOf(lottery.maxWinners.toString()) }
    var waitlistCapacityText by remember(lottery) { mutableStateOf(lottery.waitlistCapacity?.toString() ?: "") }
    var regStartMillis by remember(lottery) { mutableStateOf(lottery.registrationStartMillis) }
    var regEndMillis by remember(lottery) { mutableStateOf(lottery.registrationEndMillis) }
    var bookingStartMillis by remember(lottery) { mutableStateOf(lottery.bookingStartMillis) }
    var bookingEndMillis by remember(lottery) { mutableStateOf(lottery.bookingEndMillis) }

    var showRegRangeDialog by remember { mutableStateOf(false) }
    var showRegEndSingleDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter {
        (profile.deviceId.isNotBlank() && it.ownerId == profile.deviceId) ||
            lotteries.find { lot -> lot.id == lottery.id }?.ownerId == it.ownerId ||
            lottery.campsiteIds.contains(it.id)
    }.ifEmpty {
        campsites.filter { lottery.campsiteIds.contains(it.id) }
    }
    val availableOwnerCampsites = ownerCampsites.filter { site ->
        selectedCampsiteIds.contains(site.id) || !lotteries.any { lot ->
            lot.id != lottery.id && lot.campsiteIds.contains(site.id) &&
                getLotteryStatus(lot, site) != "Finished" &&
                getLotteryStatus(lot, site) != "N/A"
        }
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
    val totalMaxGuests = activeCampsites.sumOf { it.maxOccupancy }
    val status = getLotteryStatus(lottery, activeCampsites.firstOrNull())

    val fieldShape = RoundedCornerShape(SolaceTokens.RadiusMD)
    val fieldColors = lotteryFieldColors()
    val tnumTextStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    val ctaInteraction = remember { MutableInteractionSource() }

    val regValidationState = when {
        regStartMillis == null || regEndMillis == null -> WindowValidationState.MISSING
        regEndMillis!! <= regStartMillis!! -> WindowValidationState.INVALID
        else -> WindowValidationState.VALID
    }
    val bookingValidationState = when {
        bookingStartMillis == null || bookingEndMillis == null -> WindowValidationState.MISSING
        bookingEndMillis!! <= bookingStartMillis!! -> WindowValidationState.INVALID
        regEndMillis != null && bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L ->
            WindowValidationState.INVALID
        else -> WindowValidationState.VALID
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SolaceTokens.SpaceMD, vertical = SolaceTokens.SpaceLG)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(SolaceTokens.RadiusSheet),
            color = CardSurface,
            border = solaceCardBorder(),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sheet handle + header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SolaceTokens.SpaceXL,
                            end = SolaceTokens.SpaceMD,
                            top = SolaceTokens.SpaceMD,
                            bottom = SolaceTokens.SpaceSM
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                            .background(Neutral300)
                    )
                    Spacer(Modifier.height(SolaceTokens.SpaceMD))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSuccessState) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = StatusActiveGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    "Lottery Updated",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "LOTTERY SETTINGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SolaceRed
                                )
                                Text(
                                    "Edit Lottery Configuration",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }
                        }
                        if (!isSuccessState) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Neutral600
                                )
                            }
                        }
                    }
                }

                SolaceDivider()

                if (isSuccessState) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SolaceTokens.SpaceXXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                    ) {
                        Text(
                            "Lottery configuration for \"${lotteryName.trim()}\" has been successfully updated.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Neutral900,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Changes are saved and live in the system.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral600,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(SolaceTokens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
                    ) {
                        Text(
                            "Update the lottery settings below. Required fields and date spacing rules will be verified before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral600
                        )

                        // Section 1: Lottery Details & Capacity
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                            ) {
                                Text(
                                    "LOTTERY DETAILS & CAPACITY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral500
                                )
                                OutlinedTextField(
                                    value = lotteryName,
                                    onValueChange = { lotteryName = it },
                                    label = { Text("Lottery Name (Required)") },
                                    singleLine = true,
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = maxWinnersText,
                                    onValueChange = { maxWinnersText = it },
                                    label = { Text("Number of Lottery Winners (Required)") },
                                    singleLine = true,
                                    textStyle = tnumTextStyle,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = waitlistCapacityText,
                                    onValueChange = { waitlistCapacityText = it },
                                    label = { Text("Waitlist capacity (optional)") },
                                    supportingText = { Text("Leave blank for unlimited entrants") },
                                    singleLine = true,
                                    textStyle = tnumTextStyle,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Section 2: Assigned Campsites
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                            ) {
                                Text(
                                    "Assigned Campsite(s):",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                                availableOwnerCampsites.forEach { site ->
                                    val isSelected = selectedCampsiteIds.contains(site.id)
                                    AnimatedCampsiteToggleRow(
                                        label = "${site.name} (Max Guests: ${site.maxOccupancy})",
                                        checked = isSelected,
                                        onToggle = {
                                            selectedCampsiteIds = if (isSelected) {
                                                if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                            } else {
                                                selectedCampsiteIds + site.id
                                            }
                                        }
                                    )
                                }
                                TabularText(
                                    text = "Total Max Guest Capacity: $totalMaxGuests",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SolaceRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Section 3: Registration Window
                        WindowStatusSectionCard(
                            title = "Registration Window",
                            windowSummary = "Registration Window: ${formatDateMillis(regStartMillis)} to ${formatDateMillis(regEndMillis)}",
                            validationState = regValidationState,
                            statusBadgeText = when (regValidationState) {
                                WindowValidationState.VALID -> "Configured"
                                WindowValidationState.INVALID -> "Check Dates"
                                WindowValidationState.MISSING -> "Required"
                            },
                            helperMessage = when (status) {
                                "Active" -> "Registration is currently Active. Open date cannot be modified."
                                "Upcoming" -> null
                                else -> "Registration period has closed. Registration dates cannot be modified."
                            },
                            actionLabel = when (status) {
                                "Upcoming" -> "Edit Registration Start & End Dates"
                                "Active" -> "Edit Registration End Date Only"
                                else -> null
                            },
                            onActionClick = when (status) {
                                "Upcoming" -> ({ showRegRangeDialog = true })
                                "Active" -> ({ showRegEndSingleDialog = true })
                                else -> null
                            }
                        )

                        // Section 4: Booking Window
                        WindowStatusSectionCard(
                            title = "Booking Window",
                            windowSummary = "Booking Window: ${formatDateMillis(bookingStartMillis)} to ${formatDateMillis(bookingEndMillis)}",
                            validationState = bookingValidationState,
                            statusBadgeText = when (bookingValidationState) {
                                WindowValidationState.VALID -> "Configured"
                                WindowValidationState.INVALID -> "Check Gap"
                                WindowValidationState.MISSING -> "Required"
                            },
                            helperMessage = if (bookingValidationState == WindowValidationState.INVALID) {
                                "Booking must start at least 1 week after registration closes."
                            } else null,
                            actionLabel = "Select Booking Window Dates",
                            onActionClick = { showBookingDateDialog = true }
                        )

                        if (feedbackMsg.isNotBlank()) {
                            Surface(
                                shape = SolaceInnerCardShape,
                                color = if (feedbackIsError) SolaceRedSoft else StatusActiveBg,
                                border = BorderStroke(
                                    SolaceTokens.HairlineWidth,
                                    if (feedbackIsError) SolaceRed.copy(alpha = 0.4f) else StatusActiveGreen.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = feedbackMsg,
                                    color = if (feedbackIsError) ErrorRed else StatusActiveGreen,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(SolaceTokens.SpaceMD)
                                )
                            }
                        }
                    }
                }

                SolaceDivider()

                // Sticky bottom action footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SolaceTokens.SpaceXL, vertical = SolaceTokens.SpaceMD),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSuccessState) {
                        Button(
                            onClick = onSuccess,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Neutral600, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(SolaceTokens.SpaceSM))
                        Button(
                            onClick = {
                                val parsedWinners = maxWinnersText.toIntOrNull()
                                val parsedCapacity = if (waitlistCapacityText.isBlank()) null else waitlistCapacityText.toIntOrNull()

                                when {
                                    lotteryName.isBlank() -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Lottery name cannot be empty."
                                    }
                                    parsedWinners == null || parsedWinners <= 0 -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Number of lottery winners must be greater than 0."
                                    }
                                    parsedWinners > totalMaxGuests -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Number of winners ($parsedWinners) cannot exceed the total maximum guest capacity ($totalMaxGuests) of the selected campsite(s)."
                                    }
                                    waitlistCapacityText.isNotBlank() && (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
                                    }
                                    regStartMillis == null || regEndMillis == null -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Please select both a start and closing date/time."
                                    }
                                    bookingStartMillis == null || bookingEndMillis == null -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Please select both a start and end date for the booking window."
                                    }
                                    regEndMillis!! <= regStartMillis!! -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Registration close date must be strictly after the open date."
                                    }
                                    bookingEndMillis!! <= bookingStartMillis!! -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Booking end date must be strictly after the start date."
                                    }
                                    bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Booking period must start at least 1 week after the registration period."
                                    }
                                    status == "Upcoming" && regStartMillis!! < System.currentTimeMillis() - 86400000L -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Registration open date must be set in the future."
                                    }
                                    status == "Active" && regEndMillis!! < System.currentTimeMillis() - 86400000L -> {
                                        feedbackIsError = true
                                        feedbackMsg = "Error: Registration close date must be set in the future."
                                    }
                                    else -> {
                                        feedbackIsError = false
                                        val updated = lottery.copy(
                                            name = lotteryName.trim(),
                                            campsiteIds = selectedCampsiteIds,
                                            maxWinners = parsedWinners,
                                            waitlistCapacity = parsedCapacity,
                                            registrationStartMillis = regStartMillis,
                                            registrationEndMillis = regEndMillis,
                                            registrationStart = formatDateMillis(regStartMillis),
                                            registrationEnd = formatDateMillis(regEndMillis),
                                            bookingStartMillis = bookingStartMillis,
                                            bookingEndMillis = bookingEndMillis,
                                            bookingStart = formatDateMillis(bookingStartMillis),
                                            bookingEnd = formatDateMillis(bookingEndMillis)
                                        )
                                        repository.updateLottery(updated) { result ->
                                            result.fold(
                                                onSuccess = {
                                                    isSuccessState = true
                                                },
                                                onFailure = { err ->
                                                    feedbackIsError = true
                                                    feedbackMsg = "Error updating lottery: ${err.message}"
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            interactionSource = ctaInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                            modifier = Modifier.solaceTactileScale(ctaInteraction)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showRegRangeDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = regStartMillis,
            initialSelectedEndDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regStartMillis = dateState.selectedStartDateMillis
                    regEndMillis = dateState.selectedEndDateMillis
                    showRegRangeDialog = false
                }) { Text("Save Dates", color = SolaceRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showRegRangeDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }

    if (showRegEndSingleDialog) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegEndSingleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regEndMillis = dateState.selectedDateMillis
                    showRegEndSingleDialog = false
                }) { Text("Save End Date", color = SolaceRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showRegEndSingleDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(state = dateState, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showBookingDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = bookingStartMillis,
            initialSelectedEndDateMillis = bookingEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showBookingDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    bookingStartMillis = dateState.selectedStartDateMillis
                    bookingEndMillis = dateState.selectedEndDateMillis
                    showBookingDateDialog = false
                }) { Text("Save Dates", color = SolaceRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showBookingDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}

// =========================================================================
// CREATE LOTTERY DIALOG / SHEET
// =========================================================================
/**
 * Modal sheet/card allowing Owners to configure and initiate a lottery registration period
 * for an owned campsite.
 *
 * @param campsite The [Campsite] to associate the lottery with.
 * @param repository The data repository for executing the creation logic.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 * @param onSuccess Callback invoked when the lottery is successfully created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLotteryDialog(
    campsite: Campsite,
    repository: AppRepository,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val campsites by repository.campsites.collectAsState()
    var lotteryName by remember { mutableStateOf("${campsite.name} Lottery") }
    var maxWinnersText by remember { mutableStateOf("3") }
    var waitlistCapacityText by remember { mutableStateOf("") }
    var regStartMillis by remember { mutableStateOf<Long?>(null) }
    var regEndMillis by remember { mutableStateOf<Long?>(null) }
    var bookingStartMillis by remember { mutableStateOf<Long?>(null) }
    var bookingEndMillis by remember { mutableStateOf<Long?>(null) }
    var showRegDateDialog by remember { mutableStateOf(false) }
    var showBookingDateDialog by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }

    val ownerCampsites = campsites.filter {
        (profile.deviceId.isNotBlank() && it.ownerId == profile.deviceId) || it.ownerId == campsite.ownerId
    }.ifEmpty { listOf(campsite) }
    val availableOwnerCampsites = ownerCampsites.filter { isCampsiteAvailableForLottery(it, lotteries) }
    var selectedCampsiteIds by remember(availableOwnerCampsites) {
        mutableStateOf(
            if (availableOwnerCampsites.any { it.id == campsite.id }) {
                listOf(campsite.id)
            } else {
                availableOwnerCampsites.take(1).map { it.id }
            }
        )
    }
    val activeCampsites = availableOwnerCampsites.filter { selectedCampsiteIds.contains(it.id) }
    val totalMaxGuests = activeCampsites.sumOf { it.maxOccupancy }

    val fieldShape = RoundedCornerShape(SolaceTokens.RadiusMD)
    val fieldColors = lotteryFieldColors()
    val tnumTextStyle = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    val ctaInteraction = remember { MutableInteractionSource() }

    val regValidationState = when {
        regStartMillis == null || regEndMillis == null -> WindowValidationState.MISSING
        regEndMillis!! <= regStartMillis!! -> WindowValidationState.INVALID
        regStartMillis!! < System.currentTimeMillis() - 86400000L -> WindowValidationState.INVALID
        else -> WindowValidationState.VALID
    }

    val bookingValidationState = when {
        bookingStartMillis == null || bookingEndMillis == null -> WindowValidationState.MISSING
        bookingEndMillis!! <= bookingStartMillis!! -> WindowValidationState.INVALID
        regEndMillis != null && bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L ->
            WindowValidationState.INVALID
        else -> WindowValidationState.VALID
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SolaceTokens.SpaceMD, vertical = SolaceTokens.SpaceLG)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(SolaceTokens.RadiusSheet),
            color = CardSurface,
            border = solaceCardBorder(),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sheet drag handle & header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SolaceTokens.SpaceXL,
                            end = SolaceTokens.SpaceMD,
                            top = SolaceTokens.SpaceMD,
                            bottom = SolaceTokens.SpaceSM
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                            .background(Neutral300)
                    )
                    Spacer(Modifier.height(SolaceTokens.SpaceMD))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEW LOTTERY ROUND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = SolaceRed
                            )
                            Text(
                                text = "Configure & Initiate Lottery",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Neutral600
                            )
                        }
                    }
                }

                SolaceDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(SolaceTokens.SpaceXL),
                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
                ) {
                    Text(
                        "Specify lottery name, registration & booking windows, assigned campsites, and winner count for ${campsite.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral600
                    )

                    // Section 1: Lottery Details & Capacity
                    Surface(
                        shape = SolaceInnerCardShape,
                        color = Neutral50,
                        border = solaceCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(SolaceTokens.SpaceMD),
                            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                        ) {
                            Text(
                                text = "LOTTERY IDENTITY & CAPACITY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Neutral500
                            )
                            OutlinedTextField(
                                value = lotteryName,
                                onValueChange = { lotteryName = it },
                                label = { Text("Lottery Name (Required)") },
                                singleLine = true,
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = maxWinnersText,
                                onValueChange = { maxWinnersText = it },
                                label = { Text("Number of Lottery Winners (Required)") },
                                singleLine = true,
                                textStyle = tnumTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = waitlistCapacityText,
                                onValueChange = { waitlistCapacityText = it },
                                label = { Text("Waitlist capacity (optional)") },
                                supportingText = { Text("Leave blank for unlimited entrants") },
                                singleLine = true,
                                textStyle = tnumTextStyle,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = fieldShape,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Section 2: Assigned Campsites with custom animated selection toggles
                    Surface(
                        shape = SolaceInnerCardShape,
                        color = Neutral50,
                        border = solaceCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(SolaceTokens.SpaceMD),
                            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                        ) {
                            Text(
                                "Assigned Campsite(s):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                            if (availableOwnerCampsites.isEmpty()) {
                                Text(
                                    "No campsites are currently available for creating a lottery.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorRed
                                )
                            } else {
                                availableOwnerCampsites.forEach { site ->
                                    val isSelected = selectedCampsiteIds.contains(site.id)
                                    AnimatedCampsiteToggleRow(
                                        label = "${site.name} (Max Guests: ${site.maxOccupancy})",
                                        checked = isSelected,
                                        onToggle = {
                                            selectedCampsiteIds = if (isSelected) {
                                                if (selectedCampsiteIds.size > 1) selectedCampsiteIds - site.id else selectedCampsiteIds
                                            } else {
                                                selectedCampsiteIds + site.id
                                            }
                                        }
                                    )
                                }
                            }
                            TabularText(
                                text = "Total Max Guest Capacity: $totalMaxGuests",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolaceRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Section 3: Registration Window with clear validation state
                    WindowStatusSectionCard(
                        title = "Registration Window",
                        windowSummary = "Registration Window: ${formatDateMillis(regStartMillis)} to ${formatDateMillis(regEndMillis)}",
                        validationState = regValidationState,
                        statusBadgeText = when (regValidationState) {
                            WindowValidationState.VALID -> "Configured"
                            WindowValidationState.INVALID -> "Check Dates"
                            WindowValidationState.MISSING -> "Required"
                        },
                        helperMessage = when (regValidationState) {
                            WindowValidationState.INVALID -> "Ensure open date is upcoming and close date follows open date."
                            WindowValidationState.MISSING -> "Select when entrants can join the waiting list."
                            WindowValidationState.VALID -> null
                        },
                        actionLabel = "Select Registration Open & Close Dates",
                        onActionClick = { showRegDateDialog = true }
                    )

                    // Section 4: Booking Window with clear validation state
                    WindowStatusSectionCard(
                        title = "Booking Window",
                        windowSummary = "Booking Window: ${formatDateMillis(bookingStartMillis)} to ${formatDateMillis(bookingEndMillis)}",
                        validationState = bookingValidationState,
                        statusBadgeText = when (bookingValidationState) {
                            WindowValidationState.VALID -> "Configured"
                            WindowValidationState.INVALID -> "Needs 7+ Day Gap"
                            WindowValidationState.MISSING -> "Required"
                        },
                        helperMessage = when (bookingValidationState) {
                            WindowValidationState.INVALID -> "Booking period must start at least 1 week after registration closes."
                            WindowValidationState.MISSING -> "Select stay dates available to lottery winners."
                            WindowValidationState.VALID -> null
                        },
                        actionLabel = "Select Booking Window Dates",
                        onActionClick = { showBookingDateDialog = true }
                    )

                    if (feedbackMsg.isNotBlank()) {
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = if (feedbackIsError) SolaceRedSoft else StatusActiveBg,
                            border = BorderStroke(
                                SolaceTokens.HairlineWidth,
                                if (feedbackIsError) SolaceRed.copy(alpha = 0.4f) else StatusActiveGreen.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = feedbackMsg,
                                color = if (feedbackIsError) ErrorRed else StatusActiveGreen,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(SolaceTokens.SpaceMD)
                            )
                        }
                    }
                }

                SolaceDivider()

                // Sticky footer with Cancel & Crimson Publish Lottery CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SolaceTokens.SpaceXL, vertical = SolaceTokens.SpaceMD),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Neutral600, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(SolaceTokens.SpaceSM))
                    Button(
                        onClick = {
                            val hasActive = selectedCampsiteIds.isEmpty() || selectedCampsiteIds.any { selectedId ->
                                val site = campsites.find { it.id == selectedId } ?: Campsite(id = selectedId)
                                !isCampsiteAvailableForLottery(site, lotteries)
                            }
                            val parsedWinners = maxWinnersText.toIntOrNull()
                            val parsedCapacity = if (waitlistCapacityText.isBlank()) null
                            else waitlistCapacityText.toIntOrNull()

                            when {
                                availableOwnerCampsites.isEmpty() || selectedCampsiteIds.isEmpty() -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Please select at least one available campsite."
                                }
                                hasActive -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: An active lottery already exists for this campsite."
                                }
                                lotteryName.isBlank() -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Lottery name cannot be empty."
                                }
                                parsedWinners == null || parsedWinners <= 0 -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Number of lottery winners must be greater than 0."
                                }
                                parsedWinners > totalMaxGuests -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Number of winners ($parsedWinners) cannot exceed the total maximum guest capacity ($totalMaxGuests) of the selected campsite(s)."
                                }
                                waitlistCapacityText.isNotBlank() &&
                                    (parsedCapacity == null || parsedCapacity < parsedWinners) -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Waitlist capacity must be a whole number at least equal to the winner count ($parsedWinners)."
                                }
                                regStartMillis == null || regEndMillis == null -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Please select both a start and closing date/time."
                                }
                                bookingStartMillis == null || bookingEndMillis == null -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Please select both a start and end date for the booking window."
                                }
                                regEndMillis!! <= regStartMillis!! -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Registration close date must be strictly after the open date."
                                }
                                bookingEndMillis!! <= bookingStartMillis!! -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Booking end date must be strictly after the start date."
                                }
                                bookingStartMillis!! < regEndMillis!! + 7 * 24 * 60 * 60 * 1000L -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Booking period must start at least 1 week after the registration period."
                                }
                                regStartMillis!! < System.currentTimeMillis() - 86400000 -> {
                                    feedbackIsError = true
                                    feedbackMsg = "Error: Registration open date must be set in the future."
                                }
                                else -> {
                                    feedbackIsError = false
                                    repository.createLottery(
                                        Lottery(
                                            name = lotteryName.trim(),
                                            campsiteIds = selectedCampsiteIds,
                                            ownerId = profile.deviceId,
                                            maxWinners = parsedWinners,
                                            waitlistCapacity = parsedCapacity,
                                            registrationStartMillis = regStartMillis,
                                            registrationEndMillis = regEndMillis,
                                            registrationStart = formatDateMillis(regStartMillis),
                                            registrationEnd = formatDateMillis(regEndMillis),
                                            bookingStartMillis = bookingStartMillis,
                                            bookingEndMillis = bookingEndMillis,
                                            bookingStart = formatDateMillis(bookingStartMillis),
                                            bookingEnd = formatDateMillis(bookingEndMillis)
                                        )
                                    )
                                    onSuccess("Lottery registration period and winners saved successfully!")
                                }
                            }
                        },
                        interactionSource = ctaInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolaceRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                        modifier = Modifier.solaceTactileScale(ctaInteraction)
                    ) {
                        Text("Publish Lottery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRegDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = regStartMillis,
            initialSelectedEndDateMillis = regEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showRegDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    regStartMillis = dateState.selectedStartDateMillis
                    regEndMillis = dateState.selectedEndDateMillis
                    showRegDateDialog = false
                }) { Text("Save Dates", color = SolaceRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showRegDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }

    if (showBookingDateDialog) {
        val dateState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = bookingStartMillis,
            initialSelectedEndDateMillis = bookingEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showBookingDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    bookingStartMillis = dateState.selectedStartDateMillis
                    bookingEndMillis = dateState.selectedEndDateMillis
                    showBookingDateDialog = false
                }) { Text("Save Dates", color = SolaceRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showBookingDateDialog = false }) { Text("Cancel") } }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DateRangePicker(state = dateState, modifier = Modifier.fillMaxWidth().height(450.dp))
            }
        }
    }
}
