/**
 * EntrantCampsiteDetailsTest.kt
 *
 * Purpose: Instrumented UI test suite verifying entrant campsite details view,
 * complete information display (name, location, occupancy, price, rating), campsite rules,
 * cancellation policy, owner details, promotional image, availability schedule, active lottery
 * deadline, and graceful handling of missing optional information.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.WaitingListErrorCallback
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import com.example.solace_temp.ui.screens.CampsiteListScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntrantCampsiteDetailsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakeEntrantRepository : AppRepository {
        val currentProfileFlow = MutableStateFlow(Profile(deviceId = "entrant-123", role = UserRole.ENTRANT))
        override val currentProfile: StateFlow<Profile> = currentProfileFlow

        val profilesFlow = MutableStateFlow<List<Profile>>(emptyList())
        override val profiles: StateFlow<List<Profile>> = profilesFlow

        val campsitesFlow = MutableStateFlow<List<Campsite>>(emptyList())
        override val campsites: StateFlow<List<Campsite>> = campsitesFlow

        val lotteriesFlow = MutableStateFlow<List<Lottery>>(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = lotteriesFlow

        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback) {
            onResult.onResult(null)
        }
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun redrawLottery(lotteryId: String) {}
        override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {
            error("Not used in this test")
        }
        override suspend fun createReservation(
            campsiteId: String,
            notificationId: String,
            startDate: String,
            endDate: String
        ): Boolean = error("Not used in this test")
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun selectingCampsiteFromList_opensDedicatedDetailsView() {
        val fakeRepo = FakeEntrantRepository()
        val site = Campsite(
            id = "camp-ent-1",
            ownerId = "owner-999",
            name = "Mountain Haven Campsite",
            location = Geolocation(locationName = "Banff National Park"),
            pricePerNight = 60.0
        )
        fakeRepo.campsitesFlow.value = listOf(site)

        var selectedId: String? = null

        composeTestRule.setContent {
            CampsiteListScreen(
                title = "Explore Campsites",
                repository = fakeRepo,
                onSelectCampsite = { id -> selectedId = id }
            )
        }

        composeTestRule.onNodeWithText("Mountain Haven Campsite").assertIsDisplayed().performClick()
        assertEquals("camp-ent-1", selectedId)
    }

    @Test
    fun entrantViewsCampsiteDetails_displaysCompleteInformationRulesOwnerAndLottery() {
        val fakeRepo = FakeEntrantRepository()

        val owner = Profile(
            deviceId = "owner-88",
            name = "John Campground Owner",
            email = "john.owner@example.com",
            phone = "+1-403-555-0199",
            role = UserRole.CAMPSITE_OWNER
        )
        fakeRepo.profilesFlow.value = listOf(owner)

        val campsite = Campsite(
            id = "camp-full-1",
            ownerId = "owner-88",
            name = "Lake Louise Sanctuary",
            description = "Pristine lakeside campsite with mountain backdrops.",
            location = Geolocation(51.4254, -116.1773, "Lake Louise Park"),
            maxOccupancy = 6,
            pricePerNight = 85.0,
            rating = 4.9,
            imageUrl = "http://example.com/lake_louise.jpg",
            amenities = listOf("Bear Lockers", "Fire Pit", "Water Tap"),
            tags = listOf("Lakeside", "Hiking"),
            rulesAndPolicy = "No loud music after 10 PM. 100% refund up to 48 hours prior.",
            availableDates = listOf("2027-06-15", "2027-06-16")
        )
        fakeRepo.campsitesFlow.value = listOf(campsite)

        val lottery = Lottery(
            id = "lottery-ll-1",
            name = "Summer Peak Lottery",
            campsiteIds = listOf("camp-full-1"),
            ownerId = "owner-88",
            registrationStart = "2027-05-01",
            registrationEnd = "2027-05-15",
            bookingStart = "2027-06-15",
            bookingEnd = "2027-06-30"
        )
        fakeRepo.lotteriesFlow.value = listOf(lottery)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-full-1",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify complete basic info
        composeTestRule.onNodeWithText("Lake Louise Sanctuary").assertIsDisplayed()
        composeTestRule.onNodeWithText("$85/night").assertIsDisplayed()
        composeTestRule.onNodeWithText("📍 Lake Louise Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌐 Coordinates: 51.4254, -116.1773").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max 6 guests · ★ 4.9").assertIsDisplayed()

        // Verify promotional image indicator
        composeTestRule.onNodeWithContentDescription("Campsite Promotional Image").assertIsDisplayed()

        // Verify description & amenities
        composeTestRule.onNodeWithText("Pristine lakeside campsite with mountain backdrops.").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Bear Lockers").assertIsDisplayed()

        // Verify Rules & Cancellation Policy
        composeTestRule.onNodeWithText("Rules & Cancellation Policy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("No loud music after 10 PM. 100% refund up to 48 hours prior.").performScrollTo().assertIsDisplayed()

        // Verify Owner Details
        composeTestRule.onNodeWithText("Owner Details").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Name: John Campground Owner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Email: john.owner@example.com").performScrollTo().assertIsDisplayed()

        // Verify Availability Schedule
        composeTestRule.onNodeWithText("Availability & Schedule").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("• 2027-06-15").performScrollTo().assertIsDisplayed()

        // Verify Active Lottery Status & Deadline
        composeTestRule.onNodeWithText("Active Lottery Listing").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Name: Summer Peak Lottery").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Registration Deadline: 2027-05-15").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun entrantViewsCampsiteDetails_handlesMissingOptionalInfoGracefully() {
        val fakeRepo = FakeEntrantRepository()

        // Campsite with no image, no custom rules, no owner profile, no available dates
        val campsite = Campsite(
            id = "camp-bare-1",
            ownerId = "owner-unknown",
            name = "Minimalist Forest Camp",
            description = "",
            rulesAndPolicy = "",
            imageUrl = null
        )
        fakeRepo.campsitesFlow.value = listOf(campsite)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-bare-1",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify placeholder for missing image
        composeTestRule.onNodeWithText("No Image Available").assertIsDisplayed()

        // Verify fallback description
        composeTestRule.onNodeWithText("No description provided.").assertIsDisplayed()

        // Verify fallback rules & policy
        composeTestRule.onNodeWithText("Rules & Cancellation Policy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Quiet hours 11 PM - 7 AM. Full refund if forest fire warning.").performScrollTo().assertIsDisplayed()

        // Verify fallback owner info
        composeTestRule.onNodeWithText("Owner Details").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner: Registered Owner (ID: owner-unknown)").performScrollTo().assertIsDisplayed()

        // Verify fallback availability
        composeTestRule.onNodeWithText("Availability & Schedule").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Flexible availability. Select dates below to request booking.").performScrollTo().assertIsDisplayed()

        // Verify fallback active lottery
        composeTestRule.onNodeWithText("No active lottery listing currently scheduled.").performScrollTo().assertIsDisplayed()
    }
}
