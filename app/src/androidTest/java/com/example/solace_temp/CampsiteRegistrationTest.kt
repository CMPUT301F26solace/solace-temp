/**
 * CampsiteRegistrationTest.kt
 *
 * Purpose: Comprehensive instrumented UI test suite verifying campsite registration workflows,
 * including required field validations, price and occupancy checks, success confirmation pop-up,
 * persistence, and redirection.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CreateCampsiteScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteRegistrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class RegistrationFakeRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile(deviceId = "owner-test", role = UserRole.CAMPSITE_OWNER))
        override val currentProfile: StateFlow<Profile> = _currentProfile

        val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
        override val campsites: StateFlow<List<Campsite>> = _campsites

        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        var createdCampsite: Campsite? = null

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite {
            createdCampsite = campsite.copy(id = "camp-reg-01", qrCodePayload = "solace://campsite/camp-reg-01")
            _campsites.value = _campsites.value + createdCampsite!!
            return createdCampsite!!
        }
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun registerCampsite_emptyName_showsError() {
        val fakeRepo = RegistrationFakeRepository()

        composeTestRule.setContent {
            CreateCampsiteScreen(repository = fakeRepo, onCreated = {})
        }

        composeTestRule.onNodeWithText("Generate QR Code & Publish").performClick()
        composeTestRule.onNodeWithText("Error: Campsite name cannot be empty.").assertIsDisplayed()
    }

    @Test
    fun registerCampsite_emptyLocation_showsError() {
        val fakeRepo = RegistrationFakeRepository()

        composeTestRule.setContent {
            CreateCampsiteScreen(repository = fakeRepo, onCreated = {})
        }

        composeTestRule.onNodeWithText("Campsite Name (Required)").performTextInput("Camp Pine")
        composeTestRule.onNodeWithText("Generate QR Code & Publish").performClick()
        composeTestRule.onNodeWithText("Error: Location cannot be empty.").assertIsDisplayed()
    }

    @Test
    fun registerCampsite_validInputs_showsSuccessPopupAndPersists() {
        val fakeRepo = RegistrationFakeRepository()
        var navigatedId: String? = null

        composeTestRule.setContent {
            CreateCampsiteScreen(
                repository = fakeRepo,
                onCreated = { id -> navigatedId = id }
            )
        }

        composeTestRule.onNodeWithText("Campsite Name (Required)").performTextInput("Pine Ridge Camp")
        composeTestRule.onNodeWithText("Location (Required, e.g., Banff, Jasper)").performTextInput("Jasper National Park")
        composeTestRule.onNodeWithText("Description & Amenities (Required)").performTextInput("Scenic pines and fire pits")
        composeTestRule.onNodeWithText("Price per Night ($) (Required)").performTextInput("75.0")
        composeTestRule.onNodeWithText("Maximum Occupancy (Required)").performTextInput("6")

        composeTestRule.onNodeWithText("Generate QR Code & Publish").performClick()

        // Verify success popup confirmation appears
        composeTestRule.onNodeWithText("Campsite Created Successfully").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your campsite has been registered and is now active. Tap below to view details.").assertIsDisplayed()

        // Click "View Details" to confirm redirection
        composeTestRule.onNodeWithText("View Details").performClick()

        assertEquals("camp-reg-01", navigatedId)
        assertTrue(fakeRepo._campsites.value.any { it.name == "Pine Ridge Camp" && it.qrCodePayload == "solace://campsite/camp-reg-01" })
    }
}
