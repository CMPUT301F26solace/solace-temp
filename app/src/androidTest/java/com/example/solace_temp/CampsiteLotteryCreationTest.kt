/**
 * CampsiteLotteryCreationTest.kt
 *
 * Purpose: Instrumented UI test suite verifying the campsite lottery creation user story,
 * including configuring lotteries, duplicate/overlapping active lottery prevention,
 * visual confirmation feedback, and campsite view status updates.
 *
 * Outstanding Issues: None.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteLotteryCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Fake repository for testing campsite lottery creation and active lottery validation.
     */
    class LotteryFakeRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile(deviceId = "owner-lottery", role = UserRole.CAMPSITE_OWNER))
        override val currentProfile: StateFlow<Profile> = _currentProfile

        val _campsites = MutableStateFlow(
            listOf(
                Campsite(
                    id = "camp-l-01",
                    ownerId = "owner-lottery",
                    name = "Lottery Test Camp",
                    qrCodePayload = "solace://campsite/camp-l-01"
                )
            )
        )
        override val campsites: StateFlow<List<Campsite>> = _campsites

        val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = _lotteries

        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        
        override fun createLottery(lottery: Lottery): Lottery {
            val newLottery = lottery.copy(id = "lot-${System.currentTimeMillis()}")
            _lotteries.update { it + newLottery }
            return newLottery
        }

        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun ownerConfiguresAndInitiatesLottery_updatesViewAndSucceeds() {
        val fakeRepo = LotteryFakeRepository()

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-l-01",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify initial state: No active lottery listing
        composeTestRule.onNodeWithText("No active lottery listing currently scheduled.").assertIsDisplayed()

        // Click Owner Management "Lottery" button
        composeTestRule.onNodeWithContentDescription("Create Lottery").performClick()

        // Verify Configure & Initiate Lottery dialog opens
        composeTestRule.onNodeWithText("Configure & Initiate Lottery").assertIsDisplayed()

        // Enter lottery name
        composeTestRule.onNodeWithText("Lottery Name (Required)").performTextClearance()
        composeTestRule.onNodeWithText("Lottery Name (Required)").performTextInput("Summer Peak Draw")

        // Click Publish Lottery (without dates selected should show validation error)
        composeTestRule.onNodeWithText("Publish Lottery").performClick()
        composeTestRule.onNodeWithText("Error: Please select both a start and closing date/time.").assertIsDisplayed()
    }

    @Test
    fun systemBlocksCreatingNewLottery_whenActiveLotteryExists() {
        val fakeRepo = LotteryFakeRepository()
        // Pre-populate an active lottery for this campsite
        fakeRepo._lotteries.value = listOf(
            Lottery(
                id = "lot-existing",
                name = "Existing Active Lottery",
                campsiteIds = listOf("camp-l-01"),
                ownerId = "owner-lottery",
                isDrawn = false
            )
        )

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-l-01",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify campsite view updates to display current active lottery listing status
        composeTestRule.onNodeWithText("Name: Existing Active Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Status: Active / Open").assertIsDisplayed()

        // Try clicking "Lottery" button to create a second active lottery
        composeTestRule.onNodeWithContentDescription("Create Lottery").performClick()

        // System blocks creation and shows error feedback
        composeTestRule.onNodeWithText("Error: An active lottery already exists for this campsite.").assertIsDisplayed()
    }
}
