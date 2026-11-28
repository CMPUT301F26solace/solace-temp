/**
 * CampsiteDetailsScreen.kt
 *
 * Purpose: Composable screen rendering detailed information for a single campsite, including
 * hero banner, pricing badge, GPS coordinates, rules & cancellation policy, owner details,
 * active lottery status, and an owner scratch-to-reveal QR access pass.
 *
 * Issues / Outstanding Issues:
 * - QR code scanning navigates directly to campsite details based on the deep link payload.
 */
package com.example.solace_temp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.CampsiteRemovalDependencies
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.components.PerspectivePageTransition
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
import com.example.solace_temp.ui.theme.ScrimDark
import com.example.solace_temp.ui.theme.SolaceCardShape
import com.example.solace_temp.ui.theme.SolaceDivider
import com.example.solace_temp.ui.theme.SolaceInnerCardShape
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceRedSoft
import com.example.solace_temp.ui.theme.SolaceTokens
import com.example.solace_temp.ui.theme.StatusActiveBg
import com.example.solace_temp.ui.theme.StatusActiveGreen
import com.example.solace_temp.ui.theme.StatusPill
import com.example.solace_temp.ui.theme.TabularText
import com.example.solace_temp.ui.theme.solaceCardBorder
import com.example.solace_temp.ui.theme.solaceCardColors
import com.example.solace_temp.ui.theme.solaceCardElevation
import com.example.solace_temp.ui.theme.solaceTactileScale
import com.example.solace_temp.ui.theme.statusColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Composable displaying the details of a specific campsite.
 * Used by both Entrants (to inspect details) and Owners (to view/share QR access passes).
 *
 * @param campsiteId The ID of the campsite to display.
 * @param repository The data repository for fetching campsite data.
 * @param onBack Callback invoked when the user navigates back.
 * @param onSelectLottery Optional callback invoked when the active lottery card is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampsiteDetailsScreen(
    campsiteId: String,
    repository: AppRepository,
    onBack: () -> Unit,
    onSelectLottery: ((String) -> Unit)? = null
) {
    val campsites by repository.campsites.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val lotteries by repository.lotteries.collectAsState()
    val profiles by repository.profiles.collectAsState()
    val context = LocalContext.current
    val reduceMotion = LocalReduceMotion.current

    // Perspective entry/exit transition state
    var showPage by remember(campsiteId) { mutableStateOf(false) }
    var isExiting by remember(campsiteId) { mutableStateOf(false) }

    LaunchedEffect(campsiteId) {
        showPage = true
        isExiting = false
    }

    val triggerBack = {
        if (!isExiting) {
            isExiting = true
            showPage = false
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            if (!reduceMotion) {
                delay(200L)
            }
            onBack()
        }
    }

    BackHandler(enabled = !isExiting) {
        triggerBack()
    }

    // State for administrative removal workflow and visual confirmation (US 03.01.01)
    var showAdminRemoveDialog by remember { mutableStateOf(false) }
    var removalDependencies by remember { mutableStateOf<CampsiteRemovalDependencies?>(null) }
    var isRemovingCampsite by remember { mutableStateOf(false) }
    var removedCampsiteName by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val campsite = campsites.find { it.id == campsiteId }

    PerspectivePageTransition(
        showForeground = showPage,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OwnerBackground)
            )
        }
    ) {
        // Display explicit visual confirmation if this admin just removed the campsite
        if (campsite == null && removedCampsiteName != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OwnerBackground)
                    .padding(SolaceTokens.SpaceXXL),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SolaceTokens.SpaceXXL),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Campsite Removed",
                            tint = StatusActiveGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Campsite \"$removedCampsiteName\" was successfully removed.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Associated listings, QR codes, and waitlists have been cleaned up and logged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neutral600,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = triggerBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                        ) {
                            Text("Return to Registry")
                        }
                    }
                }
            }
            return@PerspectivePageTransition
        }

        if (campsite == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OwnerBackground)
                    .padding(SolaceTokens.SpaceXXL),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SolaceTokens.SpaceXXL),
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
                            "Error: Campsite data failed to load.",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErrorRed,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = triggerBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolaceRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(SolaceTokens.RadiusMD)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
            return@PerspectivePageTransition
        }

        // Avoid crashing when the stored QR payload is missing.
        val qrPayload = campsite.qrCodePayload.ifBlank { "solace://campsite/${campsite.id}" }
        val qrBitmap = remember(qrPayload) {
            QrCodeGeneratorUtil.generateQrBitmap(qrPayload)
        }
        val activeLottery = remember(lotteries, campsite) {
            lotteries.find {
                it.campsiteIds.contains(campsite.id) &&
                    getLotteryStatus(it, campsite) != "Finished" &&
                    getLotteryStatus(it, campsite) != "N/A"
            }
        }
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val heroHeight = 252.dp
        val sheetOverlap = 24.dp
        val toolbarHeight = 64.dp
        val heroHeightPx = with(density) { heroHeight.toPx() }
        val collapseDistancePx = with(density) {
            (heroHeight - sheetOverlap - toolbarHeight).toPx()
        }.coerceAtLeast(1f)
        val topHeroScrimBrush = remember {
            Brush.verticalGradient(colors = listOf(ScrimDark, Color.Transparent))
        }

        Scaffold(
            containerColor = OwnerBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(OwnerBackground)
            ) {
                // ── COLLAPSING PARALLAX HERO IMAGE LAYER ────────────────────────────
                // Reads scrollState.value strictly inside graphicsLayer { ... } so
                // scrolling triggers 0 recompositions.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .align(Alignment.TopCenter)
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val scrollY = scrollState.value.toFloat()
                                val progress = (scrollY / collapseDistancePx).coerceIn(0f, 1f)
                                if (reduceMotion) {
                                    translationY = -scrollY
                                    scaleX = 1f
                                    scaleY = 1f
                                } else {
                                    translationY = -scrollY * 0.45f
                                    val zoom = 1f + (progress * 0.08f)
                                    scaleX = zoom
                                    scaleY = zoom
                                }
                                alpha = (1f - ((scrollY - collapseDistancePx) / (heroHeightPx * 0.25f)).coerceIn(0f, 1f))
                            }
                            .background(Neutral100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!campsite.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = campsite.imageUrl,
                                contentDescription = "Campsite Promotional Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            TopographicImageFallback(
                                modifier = Modifier.fillMaxSize(),
                                showLabel = true,
                                label = "No Image Available"
                            )
                        }
                    }

                    // Subtle top scrim over the hero photo that fades out as the toolbar fades in
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                val progress = (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                                alpha = 1f - progress
                            }
                            .background(topHeroScrimBrush)
                    )
                }

                // ── SCROLLING DETAILS SHEET LAYER ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Spacer(Modifier.height(heroHeight - sheetOverlap))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val overlapPx = sheetOverlap.toPx()
                                drawRect(
                                    color = OwnerBackground,
                                    topLeft = Offset(0f, overlapPx),
                                    size = Size(size.width, (size.height - overlapPx).coerceAtLeast(0f))
                                )
                            }
                            .padding(horizontal = SolaceTokens.SpaceLG),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceLG)
                    ) {
                        // ── 1. OVERVIEW CARD ────────────────────────────────────────
                        Card(
                            shape = SolaceCardShape,
                            colors = solaceCardColors(),
                            border = solaceCardBorder(),
                            elevation = solaceCardElevation(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(SolaceTokens.SpaceXL),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = campsite.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Neutral900,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                // Pricing Badge
                                Surface(
                                    shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                                    color = SolaceRedSoft,
                                    border = BorderStroke(
                                        SolaceTokens.HairlineWidth,
                                        SolaceRed.copy(alpha = 0.25f)
                                    )
                                ) {
                                    TabularText(
                                        text = "$${campsite.pricePerNight.toInt()}/night",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SolaceRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = SolaceTokens.SpaceMD,
                                            vertical = SolaceTokens.SpaceXS
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "📍 ${campsite.location.locationName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Neutral700
                            )
                            TabularText(
                                text = "Max ${campsite.maxOccupancy} guests · ★ ${campsite.rating}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral600
                            )

                            if (campsite.tags.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.padding(top = SolaceTokens.SpaceXS),
                                    horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM),
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                                ) {
                                    campsite.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                                            color = Neutral100,
                                            border = solaceCardBorder()
                                        ) {
                                            Text(
                                                text = tag,
                                                modifier = Modifier.padding(
                                                    horizontal = SolaceTokens.SpaceMD,
                                                    vertical = SolaceTokens.SpaceXS
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Neutral700
                                            )
                                        }
                                    }
                                }
                            }

                            SolaceDivider(Modifier.padding(vertical = SolaceTokens.SpaceXS))

                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Neutral900
                            )
                            Text(
                                text = campsite.description.ifBlank { "No description provided." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral700
                            )

                            if (campsite.amenities.isNotEmpty()) {
                                Spacer(Modifier.height(SolaceTokens.SpaceXS))
                                Text(
                                    text = "Amenities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                                campsite.amenities.forEach { amenity ->
                                    Text(
                                        text = "• $amenity",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Neutral700
                                    )
                                }
                            }
                        }
                    }

                // ── 2. GPS COORDINATES CARD ─────────────────────────────────────────
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SolaceTokens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SolaceRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LOCATION & GPS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Neutral500
                            )
                        }
                        Surface(
                            shape = SolaceInnerCardShape,
                            color = Neutral50,
                            border = solaceCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TabularText(
                                text = "🌐 Coordinates: ${campsite.location.latitude}, ${campsite.location.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Neutral900,
                                maxLines = 2,
                                modifier = Modifier.padding(SolaceTokens.SpaceMD)
                            )
                        }
                    }
                }

                // ── 3. RULES & CANCELLATION POLICY CARD ─────────────────────────────
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SolaceTokens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                    ) {
                        Text(
                            text = "CAMPSITE GUIDELINES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Neutral500
                        )
                        Text(
                            text = "Rules & Cancellation Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900
                        )
                        Text(
                            text = campsite.rulesAndPolicy.ifBlank {
                                "Quiet hours 11 PM - 7 AM. Full refund if forest fire warning."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neutral700
                        )
                    }
                }

                // ── 4. OWNER DETAILS & AVAILABILITY CARD ────────────────────────────
                Card(
                    shape = SolaceCardShape,
                    colors = solaceCardColors(),
                    border = solaceCardBorder(),
                    elevation = solaceCardElevation(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SolaceTokens.SpaceXL),
                        verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceSM)
                    ) {
                        Text(
                            text = "Owner Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900
                        )
                        val campsiteOwner = profiles.find { it.deviceId == campsite.ownerId }
                        if (campsiteOwner != null) {
                            Text(
                                text = "Name: ${campsiteOwner.name.ifBlank { "Registered Owner" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral700
                            )
                            if (campsiteOwner.email.isNotBlank()) {
                                Text(
                                    text = "Email: ${campsiteOwner.email}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Neutral700
                                )
                            }
                            if (campsiteOwner.phone.isNotBlank()) {
                                Text(
                                    text = "Phone: ${campsiteOwner.phone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Neutral700
                                )
                            }
                        } else {
                            Text(
                                text = "Owner: Registered Owner (ID: ${campsite.ownerId})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral700
                            )
                        }

                        SolaceDivider(Modifier.padding(vertical = SolaceTokens.SpaceXS))

                        Text(
                            text = "Availability & Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900
                        )
                        if (campsite.availableDates.isEmpty()) {
                            Text(
                                text = "Flexible availability. Select dates below to request booking.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Neutral700
                            )
                        } else {
                            campsite.availableDates.forEach { availableDate ->
                                Text(
                                    text = "• $availableDate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Neutral700
                                )
                            }
                        }
                    }
                }

                // ── 5. ACTIVE LOTTERY STATUS CARD ───────────────────────────────────
                val lotteryStatus = getLotteryStatus(activeLottery, campsite)
                val isLotteryActiveOrBooking = activeLottery != null &&
                    (lotteryStatus == "Active" || lotteryStatus == "Booking")

                if (activeLottery == null || profile.role != UserRole.ENTRANT || isLotteryActiveOrBooking) {
                    val (statusBg, statusFg) = statusColors(lotteryStatus)
                    Card(
                        shape = SolaceCardShape,
                        colors = solaceCardColors(),
                        border = solaceCardBorder(),
                        elevation = solaceCardElevation(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(SolaceTokens.SpaceXL),
                            verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceMD)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Lottery Listing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                                StatusPill(status = lotteryStatus)
                            }

                            Surface(
                                shape = SolaceInnerCardShape,
                                color = statusBg,
                                border = BorderStroke(
                                    SolaceTokens.HairlineWidth,
                                    statusFg.copy(alpha = 0.20f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(SolaceInnerCardShape)
                                    .then(
                                        if (activeLottery != null && onSelectLottery != null) {
                                            Modifier.clickable { onSelectLottery(activeLottery.id) }
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(SolaceTokens.SpaceMD),
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    if (activeLottery != null) {
                                        Text(
                                            text = "Name: ${activeLottery.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Neutral900
                                        )
                                        Text(
                                            text = "Status: $lotteryStatus",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = statusFg,
                                            fontWeight = FontWeight.Bold
                                        )
                                        TabularText(
                                            text = "Registration Window: ${
                                                activeLottery.registrationStart.ifBlank {
                                                    formatDateMillis(activeLottery.registrationStartMillis)
                                                }
                                            } to ${
                                                activeLottery.registrationEnd.ifBlank {
                                                    formatDateMillis(activeLottery.registrationEndMillis)
                                                }
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Neutral700,
                                            maxLines = 2
                                        )
                                        TabularText(
                                            text = "Booking Window: ${
                                                activeLottery.bookingStart.ifBlank {
                                                    formatDateMillis(activeLottery.bookingStartMillis)
                                                }
                                            } to ${
                                                activeLottery.bookingEnd.ifBlank {
                                                    formatDateMillis(activeLottery.bookingEndMillis)
                                                }
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Neutral700,
                                            maxLines = 2
                                        )
                                        if (onSelectLottery != null) {
                                            Spacer(Modifier.height(SolaceTokens.SpaceXXS))
                                            Text(
                                                text = "Tap to view lottery details →",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SolaceRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "No active lottery listing currently scheduled.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Neutral600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 6. OWNER MANAGEMENT & SCRATCH-TO-REVEAL QR ACCESS PASS CARD ─────
                val isAuthorizedOwner = profile.deviceId == campsite.ownerId ||
                    (profile.isAdmin && profile.role == UserRole.ADMIN)

                if ((profile.role == UserRole.CAMPSITE_OWNER || (profile.role == UserRole.ADMIN && profile.isAdmin)) && isAuthorizedOwner) {
                    var showEditCampsiteDialog by remember { mutableStateOf(false) }
                    var showCreateLotteryDialog by remember { mutableStateOf(false) }
                    var ownerLotteryFeedback by remember { mutableStateOf("") }
                    var isQrRevealed by rememberSaveable(campsite.id) { mutableStateOf(false) }

                    val editInteraction = remember { MutableInteractionSource() }
                    val lotteryInteraction = remember { MutableInteractionSource() }
                    val entrantsInteraction = remember { MutableInteractionSource() }
                    val revealInteraction = remember { MutableInteractionSource() }
                    val shareInteraction = remember { MutableInteractionSource() }

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
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXXS)
                            ) {
                                Text(
                                    text = "OWNER CONTROLS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SolaceRed
                                )
                                Text(
                                    text = "Owner Management",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Neutral900
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    FilledIconButton(
                                        onClick = { showEditCampsiteDialog = true },
                                        interactionSource = editInteraction,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = SolaceRedSoft
                                        ),
                                        modifier = Modifier.solaceTactileScale(editInteraction, 0.92f)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Details",
                                            tint = SolaceRed
                                        )
                                    }
                                    Text(
                                        "Edit",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Neutral700
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            val hasActive = !isCampsiteAvailableForLottery(campsite, lotteries)
                                            if (hasActive) {
                                                ownerLotteryFeedback = "Error: An active lottery already exists for this campsite."
                                            } else {
                                                ownerLotteryFeedback = ""
                                                showCreateLotteryDialog = true
                                            }
                                        },
                                        interactionSource = lotteryInteraction,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = SolaceRedSoft
                                        ),
                                        modifier = Modifier.solaceTactileScale(lotteryInteraction, 0.92f)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Create Lottery",
                                            tint = SolaceRed
                                        )
                                    }
                                    Text(
                                        "Lottery",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Neutral700
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(SolaceTokens.SpaceXS)
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            if (activeLottery != null && onSelectLottery != null) {
                                                onSelectLottery(activeLottery.id)
                                            }
                                        },
                                        interactionSource = entrantsInteraction,
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = SolaceRedSoft
                                        ),
                                        modifier = Modifier.solaceTactileScale(entrantsInteraction, 0.92f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = "View Entrants",
                                            tint = SolaceRed
                                        )
                                    }
                                    Text(
                                        "Entrants",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Neutral700
                                    )
                                }
                            }

                            if (showEditCampsiteDialog) {
                                EditCampsiteDialog(
                                    campsite = campsite,
                                    repository = repository,
                                    onDismiss = { showEditCampsiteDialog = false },
                                    onSuccess = { showEditCampsiteDialog = false }
                                )
                            }

                            if (showCreateLotteryDialog) {
                                CreateLotteryDialog(
                                    campsite = campsite,
                                    repository = repository,
                                    onDismiss = { showCreateLotteryDialog = false },
                                    onSuccess = { msg: String ->
                                        ownerLotteryFeedback = msg
                                        showCreateLotteryDialog = false
                                    }
                                )
                            }

                            if (ownerLotteryFeedback.isNotBlank()) {
                                Text(
                                    text = ownerLotteryFeedback,
                                    color = if (ownerLotteryFeedback.startsWith("Error")) ErrorRed else StatusActiveGreen,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            SolaceDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ACCESS PASS PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Neutral500
                                    )
                                    Text(
                                        text = "Promotional QR Code",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Neutral900
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(SolaceTokens.RadiusSM),
                                    color = if (isQrRevealed) StatusActiveBg else Neutral100
                                ) {
                                    Text(
                                        text = if (isQrRevealed) "Revealed" else "Scratch to Reveal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isQrRevealed) StatusActiveGreen else Neutral700,
                                        modifier = Modifier.padding(
                                            horizontal = SolaceTokens.SpaceSM,
                                            vertical = SolaceTokens.SpaceXS
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Unique Link: ${campsite.qrCodePayload}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral600,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(SolaceInnerCardShape)
                                    .background(Neutral50)
                                    .border(
                                        SolaceTokens.HairlineWidth,
                                        HairlineBorder,
                                        SolaceInnerCardShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Campsite QR Code",
                                    modifier = Modifier.size(196.dp)
                                )

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !isQrRevealed,
                                    enter = EnterTransition.None,
                                    exit = fadeOut(animationSpec = tween(220)),
                                    modifier = Modifier.matchParentSize()
                                ) {
                                    ScratchRevealCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        overlayColor = Neutral300,
                                        strokeWidth = 64f,
                                        revealThreshold = 0.45f,
                                        onFullyRevealed = { isQrRevealed = true }
                                    )
                                }
                            }

                            if (!isQrRevealed) {
                                Text(
                                    text = "Scratch the protective foil above or tap below to reveal the access pass.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Neutral600,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = { isQrRevealed = true },
                                    interactionSource = revealInteraction,
                                    shape = RoundedCornerShape(SolaceTokens.RadiusPill),
                                    border = solaceCardBorder(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CardSurface,
                                        contentColor = Neutral900
                                    ),
                                    modifier = Modifier
                                        .solaceTactileScale(revealInteraction)
                                        .testTag("btn_reveal_qr_instantly")
                                        .semantics {
                                            contentDescription = "Reveal QR access pass instantly"
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
                                    onClick = { isQrRevealed = false },
                                    modifier = Modifier.testTag("btn_cover_qr_pass")
                                ) {
                                    Text(
                                        text = "Cover Access Pass",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Neutral600
                                    )
                                }
                            }

                            Button(
                                onClick = { QrCodeGeneratorUtil.shareQrCodeImage(context, campsite, qrBitmap) },
                                interactionSource = shareInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolaceRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(SolaceTokens.RadiusMD),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .solaceTactileScale(shareInteraction)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(SolaceTokens.SpaceSM))
                                Text(
                                    "Share / Save QR Code",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ── 7. ADMINISTRATOR MODERATION SECTION (US 03.01.01) ───────────────
                if (profile.isAdmin && profile.role == UserRole.ADMIN) {
                    Card(
                        shape = SolaceCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(SolaceTokens.SpaceXL)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Administrator Moderation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Removing this campsite will immediately unlist it across the platform, clean up associated assets/waitlists, and log the action.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Neutral700
                            )
                            Button(
                                onClick = {
                                    removalDependencies = repository.getCampsiteRemovalDependencies(campsite.id)
                                    showAdminRemoveDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Campsite",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Remove Registered Campsite")
                            }
                        }
                    }
                }

                        Spacer(Modifier.height(SolaceTokens.SpaceLG))
                    }
                }

                // ── COLLAPSING TOP SCRIM & TOOLBAR OVERLAY ──────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(toolbarHeight)
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                alpha = (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                            }
                            .background(OwnerBackground)
                    )
                    SolaceDivider(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                alpha = (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                            }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = SolaceTokens.SpaceSM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        val progress = (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                                        alpha = 1f - progress
                                    }
                                    .clip(CircleShape)
                                    .background(CardSurface.copy(alpha = 0.88f))
                            )
                            IconButton(onClick = triggerBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Neutral900
                                )
                            }
                        }
                        Spacer(Modifier.width(SolaceTokens.SpaceXS))
                        Text(
                            text = "Campsite Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Neutral900,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                val progress = (scrollState.value.toFloat() / collapseDistancePx).coerceIn(0f, 1f)
                                val titleAlpha = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                alpha = titleAlpha
                                if (!reduceMotion) {
                                    translationY = (1f - titleAlpha) * 10f
                                }
                            }
                        )
                    }
                }
            }
        }

        // Show dependency-aware removal confirmation dialog when triggered
        val deps = removalDependencies
        if (showAdminRemoveDialog && deps != null) {
            AdminRemoveCampsiteDialog(
                campsite = campsite,
                dependencies = deps,
                isRemoving = isRemovingCampsite,
                onDismiss = {
                    showAdminRemoveDialog = false
                    removalDependencies = null
                },
                onConfirmRemoval = { forceOverride ->
                    isRemovingCampsite = true
                    val targetName = campsite.name
                    repository.removeCampsiteAsAdmin(
                        campsiteId = campsite.id,
                        forceOverride = forceOverride
                    ) { result ->
                        isRemovingCampsite = false
                        showAdminRemoveDialog = false
                        removalDependencies = null
                        result.onSuccess {
                            removedCampsiteName = targetName
                        }.onFailure { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = error.message ?: "Failed to remove campsite."
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
