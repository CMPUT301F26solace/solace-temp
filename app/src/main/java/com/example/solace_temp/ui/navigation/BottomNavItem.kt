package com.example.solace_temp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.solace_temp.models.UserRole

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

    // Campsite Owner Tabs (Matches Mockup 2, adapted from Events -> Campsites)
    
    /** Navigation item for the Owner's Campsites view. */
    object OwnerCampsites : BottomNavItem("owner_campsites", "Campsites", Icons.Default.Terrain)
    
    /** Navigation item for the Owner's Create Campsite view. */
    object OwnerCreate : BottomNavItem("owner_create", "Create", Icons.Default.AddCircleOutline)
    
    /** Navigation item for the Owner's Entrants list view. */
    object OwnerEntrants : BottomNavItem("owner_entrants", "Entrants", Icons.Default.Group)
    
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
 * @return A list of valid [BottomNavItem]s for the provided role.
 */
fun getBottomNavItemsForRole(role: UserRole): List<BottomNavItem> {
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
            BottomNavItem.OwnerEntrants,
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