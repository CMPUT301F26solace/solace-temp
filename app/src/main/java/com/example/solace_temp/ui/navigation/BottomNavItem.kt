/**
 * BottomNavItem.kt
 *
 * Purpose: Defines navigation bar items, routes, labels, icons, and the Owner floating pill
 * navigation bar with GPU-composited sliding indicator and tactile press feedback.
 */
package com.example.solace_temp.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.components.LocalReduceMotion
import com.example.solace_temp.ui.theme.CardSurface
import com.example.solace_temp.ui.theme.HairlineBorder
import com.example.solace_temp.ui.theme.Neutral600
import com.example.solace_temp.ui.theme.SolaceRed
import com.example.solace_temp.ui.theme.SolaceRedSoft
import com.example.solace_temp.ui.theme.SolaceRedVibrant
import com.example.solace_temp.ui.theme.SolaceTokens

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    // Entrant Tabs (Matches Mockup 1)

    /** Navigation item for the Entrant's Home view. */
    object EntrantHome : BottomNavItem("entrant_home", "Home", Icons.Default.Home)

    /** Navigation item for the Entrant's Search view. */
    object EntrantSearch : BottomNavItem("entrant_search", "Search", Icons.Default.Search)

    /** Navigation item for the Entrant's QR Code Scanner view. */
    object EntrantQrScan : BottomNavItem("entrant_qr_scan", "QR Scan", Icons.Default.QrCodeScanner)

    /** Navigation item for the Entrant's Notifications view. */
    object EntrantNotifications : BottomNavItem("entrant_notifications", "Notifications", Icons.Default.Notifications)

    // Campsite Owner Tabs (Campsites, Create +, Lottery, Profile)

    /** Navigation item for the Owner's Campsites view. */
    object OwnerCampsites : BottomNavItem("owner_campsites", "Campsites", Icons.Default.Terrain)

    /** Navigation item for the Owner's Create Campsite view. */
    object OwnerCreate : BottomNavItem("owner_create", "Create", Icons.Default.AddCircleOutline)

    /** Navigation item for the Owner's Lottery management view. */
    object OwnerLottery : BottomNavItem("owner_lottery", "Lottery", Icons.Default.Shuffle)

    // Admin Tabs (US 03.01 - 03.12)

    /** Navigation item for the Admin's Campsite moderation view. */
    object AdminCampsites : BottomNavItem("admin_campsites", "Campsites", Icons.Default.DeleteSweep)

    /** Navigation item for the Admin's Profile moderation view. */
    object AdminProfiles : BottomNavItem("admin_profiles", "Profiles", Icons.Default.SupervisorAccount)

    /** Navigation item for the Admin's System Logs view. */
    object AdminLogs : BottomNavItem("admin_logs", "Logs", Icons.Default.ReceiptLong)

    // Shared Profile / Role Switcher Tab

    /** Shared navigation item for the user's Profile and Role Switcher view. */
    object ProfileTab : BottomNavItem("profile", "Profile", Icons.Default.Person)
}

/**
 * Returns a list of [BottomNavItem]s that correspond to the specified user role.
 *
 * @param role The [UserRole] to retrieve navigation items for.
 * @param isAdmin Indicates whether the current user has verified admin privileges.
 * @return A list of valid [BottomNavItem]s for the provided role and privilege status.
 */
fun getBottomNavItemsForRole(role: UserRole, isAdmin: Boolean = true): List<BottomNavItem> {
    if (!isAdmin) {
        return when (role) {
            UserRole.CAMPSITE_OWNER -> listOf(
                BottomNavItem.OwnerCampsites,
                BottomNavItem.OwnerCreate,
                BottomNavItem.OwnerLottery,
                BottomNavItem.ProfileTab
            )
            else -> listOf(
                BottomNavItem.EntrantHome,
                BottomNavItem.EntrantSearch,
                BottomNavItem.EntrantQrScan,
                BottomNavItem.EntrantNotifications,
                BottomNavItem.ProfileTab
            )
        }
    }
    return when (role) {
        UserRole.ENTRANT -> listOf(
            BottomNavItem.EntrantHome,
            BottomNavItem.EntrantSearch,
            BottomNavItem.EntrantQrScan,
            BottomNavItem.EntrantNotifications,
            BottomNavItem.ProfileTab
        )
        UserRole.CAMPSITE_OWNER -> listOf(
            BottomNavItem.OwnerCampsites,
            BottomNavItem.OwnerCreate,
            BottomNavItem.OwnerLottery,
            BottomNavItem.ProfileTab
        )
        UserRole.ADMIN -> listOf(
            BottomNavItem.AdminCampsites,
            BottomNavItem.AdminProfiles,
            BottomNavItem.AdminLogs,
            BottomNavItem.ProfileTab
        )
    }
}

/**
 * Floating pill navigation bar styled with the Solace #FFFFFF / crimson accent palette,
 * shared across all role views (Entrant, Campsite Owner, and Admin).
 * Uses a single GPU-composited sliding indicator (`graphicsLayer { translationX; scaleX; scaleY }`)
 * and per-item tactile press feedback so tab switching stays locked at 60 fps.
 */
@Composable
fun SolaceFloatingBottomBar(
    navItems: List<BottomNavItem>,
    selectedItem: BottomNavItem,
    unseenNotificationCount: Int = 0,
    onSelectItem: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val safeItems = navItems.ifEmpty { listOf(BottomNavItem.OwnerCampsites) }
    val selectedIndex = safeItems.indexOf(selectedItem).let { if (it >= 0) it else 0 }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "floating_nav_indicator_index"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SolaceTokens.SpaceLG, vertical = SolaceTokens.SpaceMD),
        shape = RoundedCornerShape(SolaceTokens.RadiusPill),
        color = CardSurface,
        border = BorderStroke(SolaceTokens.HairlineWidth, HairlineBorder),
        shadowElevation = 10.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val slotWidth = maxWidth / safeItems.size.coerceAtLeast(1)
            val slotWidthPx = with(LocalDensity.current) { slotWidth.toPx() }
            val barHeight = 58.dp

            // GPU-composited sliding active indicator pill
            Box(
                modifier = Modifier
                    .width(slotWidth)
                    .height(barHeight)
                    .graphicsLayer {
                        translationX = animatedIndex * slotWidthPx
                        scaleX = 0.96f
                        scaleY = 0.92f
                    }
                    .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                    .background(SolaceRedSoft)
                    .border(
                        width = SolaceTokens.HairlineWidth,
                        color = SolaceRed.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(SolaceTokens.RadiusPill)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                safeItems.forEach { item ->
                    val isSelected = item == selectedItem
                    val interactionSource = remember(item.route) { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val pressScale by animateFloatAsState(
                        targetValue = when {
                            reduceMotion -> 1f
                            isPressed -> 0.90f
                            isSelected -> 1.04f
                            else -> 1f
                        },
                        animationSpec = if (reduceMotion) {
                            snap()
                        } else {
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        },
                        label = "nav_press_scale_${item.route}"
                    )

                    val contentTint by animateColorAsState(
                        targetValue = if (isSelected) SolaceRed else Neutral600,
                        animationSpec = if (reduceMotion) snap() else tween(180),
                        label = "nav_tint_${item.route}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(SolaceTokens.RadiusPill))
                            .selectable(
                                selected = isSelected,
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelectItem(item) }
                            )
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .testTag("bottom_nav_${item.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when {
                            item == BottomNavItem.OwnerCreate -> {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SolaceRed else SolaceRedVibrant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = item.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            item == BottomNavItem.EntrantNotifications && unseenNotificationCount > 0 -> {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = SolaceRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unseenNotificationCount > 99) "99+" else unseenNotificationCount.toString()
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = contentTint,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = contentTint,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        val labelStyle = if (safeItems.size > 4) {
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.sp
                            )
                        } else {
                            MaterialTheme.typography.labelSmall
                        }

                        Text(
                            text = item.label,
                            style = labelStyle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentTint,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
