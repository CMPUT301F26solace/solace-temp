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
}
