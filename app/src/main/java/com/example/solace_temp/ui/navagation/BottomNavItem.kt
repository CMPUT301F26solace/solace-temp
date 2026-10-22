package com.example.solace_temp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.solace_temp.models.UserRole

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    // Entrant Tabs (Matches Mockup 1)
    object EntrantHome : BottomNavItem("entrant_home", "Home", Icons.Default.Home)
    object EntrantSearch : BottomNavItem("entrant_search", "Search", Icons.Default.Search)
    object EntrantQrScan : BottomNavItem("entrant_qr_scan", "QR Scan", Icons.Default.QrCodeScanner)
    object EntrantNotifications : BottomNavItem("entrant_notifications", "Notifications", Icons.Default.Notifications)

    // Campsite Owner Tabs (Matches Mockup 2, adapted from Events -> Campsites)
    object OwnerCampsites : BottomNavItem("owner_campsites", "Campsites", Icons.Default.Terrain)
    object OwnerCreate : BottomNavItem("owner_create", "Create", Icons.Default.AddCircleOutline)
    object OwnerEntrants : BottomNavItem("owner_entrants", "Entrants", Icons.Default.Group)
    object OwnerLottery : BottomNavItem("owner_lottery", "Lottery", Icons.Default.Shuffle)

    // Admin Tabs (US 03.01 - 03.12)
    object AdminCampsites : BottomNavItem("admin_campsites", "Campsites", Icons.Default.DeleteSweep)
    object AdminProfiles : BottomNavItem("admin_profiles", "Profiles", Icons.Default.SupervisorAccount)
    object AdminLogs : BottomNavItem("admin_logs", "Logs", Icons.Default.ReceiptLong)

    // Shared Profile / Role Switcher Tab
    object ProfileTab : BottomNavItem("profile", "Profile", Icons.Default.Person)
}

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