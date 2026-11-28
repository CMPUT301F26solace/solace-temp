package com.example.solace_temp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.navigation.BottomNavItem
import com.example.solace_temp.ui.navigation.SolaceFloatingBottomBar
import com.example.solace_temp.ui.navigation.getBottomNavItemsForRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SolaceBottomNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ownerBottomNavBar_rendersAllOwnerItemsAndUpdatesSelection() {
        val ownerItems = getBottomNavItemsForRole(UserRole.CAMPSITE_OWNER, isAdmin = false)
        var lastClicked: BottomNavItem? = null

        composeTestRule.setContent {
            var selected by remember { mutableStateOf<BottomNavItem>(BottomNavItem.OwnerCampsites) }
            SolaceFloatingBottomBar(
                navItems = ownerItems,
                selectedItem = selected,
                unseenNotificationCount = 0,
                onSelectItem = { item ->
                    selected = item
                    lastClicked = item
                }
            )
        }

        // Verify all 4 Owner pill tabs are displayed
        composeTestRule.onNodeWithText("Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_owner_campsites").assertIsSelected()

        // Tap Create + button
        composeTestRule.onNodeWithTag("bottom_nav_owner_create").performClick()
        assertEquals(BottomNavItem.OwnerCreate, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_owner_create").assertIsSelected()

        // Tap Lottery tab
        composeTestRule.onNodeWithTag("bottom_nav_owner_lottery").performClick()
        assertEquals(BottomNavItem.OwnerLottery, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_owner_lottery").assertIsSelected()

        // Tap Profile tab
        composeTestRule.onNodeWithTag("bottom_nav_profile").performClick()
        assertEquals(BottomNavItem.ProfileTab, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_profile").assertIsSelected()
    }

    @Test
    fun entrantBottomNavBar_rendersAllEntrantItemsBadgeAndUpdatesSelection() {
        val entrantItems = getBottomNavItemsForRole(UserRole.ENTRANT, isAdmin = false)
        var lastClicked: BottomNavItem? = null

        composeTestRule.setContent {
            var selected by remember { mutableStateOf<BottomNavItem>(BottomNavItem.EntrantHome) }
            SolaceFloatingBottomBar(
                navItems = entrantItems,
                selectedItem = selected,
                unseenNotificationCount = 3,
                onSelectItem = { item ->
                    selected = item
                    lastClicked = item
                }
            )
        }

        // Verify all 5 Entrant pill tabs and notification badge are displayed
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
        composeTestRule.onNodeWithText("QR Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_entrant_home").assertIsSelected()

        // Tap Search tab
        composeTestRule.onNodeWithTag("bottom_nav_entrant_search").performClick()
        assertEquals(BottomNavItem.EntrantSearch, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_entrant_search").assertIsSelected()

        // Tap QR Scan tab
        composeTestRule.onNodeWithTag("bottom_nav_entrant_qr_scan").performClick()
        assertEquals(BottomNavItem.EntrantQrScan, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_entrant_qr_scan").assertIsSelected()

        // Tap Notifications tab
        composeTestRule.onNodeWithTag("bottom_nav_entrant_notifications").performClick()
        assertEquals(BottomNavItem.EntrantNotifications, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_entrant_notifications").assertIsSelected()

        // Tap Profile tab
        composeTestRule.onNodeWithTag("bottom_nav_profile").performClick()
        assertEquals(BottomNavItem.ProfileTab, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_profile").assertIsSelected()
    }

    @Test
    fun adminBottomNavBar_rendersAllAdminItemsAndUpdatesSelection() {
        val adminItems = getBottomNavItemsForRole(UserRole.ADMIN, isAdmin = true)
        var lastClicked: BottomNavItem? = null

        composeTestRule.setContent {
            var selected by remember { mutableStateOf<BottomNavItem>(BottomNavItem.AdminCampsites) }
            SolaceFloatingBottomBar(
                navItems = adminItems,
                selectedItem = selected,
                unseenNotificationCount = 0,
                onSelectItem = { item ->
                    selected = item
                    lastClicked = item
                }
            )
        }

        // Verify all 4 Admin pill tabs are displayed
        composeTestRule.onNodeWithText("Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logs").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()

        composeTestRule.onNodeWithTag("bottom_nav_admin_campsites").assertIsSelected()

        // Tap Profiles tab
        composeTestRule.onNodeWithTag("bottom_nav_admin_profiles").performClick()
        assertEquals(BottomNavItem.AdminProfiles, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_admin_profiles").assertIsSelected()

        // Tap Logs tab
        composeTestRule.onNodeWithTag("bottom_nav_admin_logs").performClick()
        assertEquals(BottomNavItem.AdminLogs, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_admin_logs").assertIsSelected()

        // Tap Profile tab
        composeTestRule.onNodeWithTag("bottom_nav_profile").performClick()
        assertEquals(BottomNavItem.ProfileTab, lastClicked)
        composeTestRule.onNodeWithTag("bottom_nav_profile").assertIsSelected()
    }
}
