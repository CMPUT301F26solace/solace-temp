package com.example.solace_temp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.AdminCampsitesScreen
import org.junit.After
import org.junit.Rule
import org.junit.Test

class AdminCampsitesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun restoreRole() {
        MockAppRepository.switchRole(UserRole.ENTRANT)
    }

    @Test
    fun adminCampsitesScreen_deniesAccessToNonAdmins() {
        // Setup mock repo with an ENTRANT profile (isAdmin = false)
        val mockRepo = MockAppRepository
        mockRepo.switchRole(UserRole.ENTRANT)

        composeTestRule.setContent {
            AdminCampsitesScreen(repository = mockRepo, onSelectCampsite = {})
        }

        // Assert the access denied message is displayed
        composeTestRule.onNodeWithText("Access Denied: Administrator credentials required.")
            .assertIsDisplayed()
    }

    @Test
    fun adminCampsitesScreen_displaysAndFiltersCampsites() {
        // Setup mock repo with an ADMIN profile
        val mockRepo = MockAppRepository
        mockRepo.switchRole(UserRole.ADMIN)

        composeTestRule.setContent {
            AdminCampsitesScreen(repository = mockRepo, onSelectCampsite = {})
        }

        // Assert the search bar exists
        val searchBar = composeTestRule.onNodeWithText("Search by name, location, or owner ID...")
        searchBar.assertIsDisplayed()

        // Type a search query
        searchBar.performTextInput("Tunnel Mountain")

        // Assert the empty state is NOT shown, meaning items filtered correctly
        composeTestRule.onNodeWithText("No campsites match search/filter criteria.")
            .assertDoesNotExist()
    }
}

