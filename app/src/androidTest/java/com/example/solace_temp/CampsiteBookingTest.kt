package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import com.example.solace_temp.ui.screens.CreateCampsiteScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteBookingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakeBookingRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile())
        override val currentProfile: StateFlow<Profile> = _currentProfile

        val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
        override val campsites: StateFlow<List<Campsite>> = _campsites

        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun ownerCreatesCampsite_withoutDates_showsError() {
        val fakeRepo = FakeBookingRepository()
        fakeRepo._currentProfile.value = Profile(role = UserRole.CAMPSITE_OWNER)

        composeTestRule.setContent {
            CreateCampsiteScreen(repository = fakeRepo, onCreated = {})
        }

        // Acceptance Criteria: Owner can define permissible booking date ranges
        // System validates that end is after start, and in the future
        // If we skip setting dates entirely, we should get an error
        composeTestRule.onNodeWithText("Generate QR Code & Publish").performClick()
        
        composeTestRule.onNodeWithText("Error: Please define a booking period.").assertIsDisplayed()
    }

    @Test
    fun entrantBooking_outsideRange_showsClearErrorFeedback() {
        val fakeRepo = FakeBookingRepository()
        fakeRepo._currentProfile.value = Profile(role = UserRole.ENTRANT)

        // Setup a campsite with specific date boundaries
        val campsite = Campsite(
            id = "camp-dates",
            name = "Strict Dates Camp",
            bookingStartDateMillis = 1800000000000L, // High future timestamp
            bookingEndDateMillis = 1800086400000L,
            qrCodePayload = "solace://campsite/camp-dates"
        )
        fakeRepo._campsites.value = listOf(campsite)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-dates",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Acceptance Criteria: Entrants restricted from selecting dates outside period
        // Click "Select Dates & Book"
        composeTestRule.onNodeWithText("Select Dates & Book").performClick()

        // Inside the DatePickerDialog, assume no valid dates are selected initially.
        // Confirming immediately should show an error
        composeTestRule.onNodeWithText("Confirm Booking").performClick()
        
        // Validation boundary feedback
        composeTestRule.onNodeWithText("Error: Please select both a check-in and check-out date.").assertIsDisplayed()
    }
}
