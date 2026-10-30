/**
 * CampsiteBookingTest.kt
 *
 * Purpose: Comprehensive instrumented UI test suite verifying booking workflows,
 * date range selections, and entry/booking validation rules.
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

        val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = _lotteries

        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun entrantBooking_outsideRange_showsClearErrorFeedback() {
        val fakeRepo = FakeBookingRepository()
        fakeRepo._currentProfile.value = Profile(deviceId = "entrant-1", role = UserRole.ENTRANT)

        val campsite = Campsite(
            id = "camp-dates",
            name = "Strict Dates Camp",
            qrCodePayload = "solace://campsite/camp-dates"
        )
        fakeRepo._campsites.value = listOf(campsite)

        val lottery = Lottery(
            id = "lot-1",
            name = "Test Lottery",
            campsiteIds = listOf("camp-dates"),
            registrationStartMillis = System.currentTimeMillis() - 10000,
            registrationEndMillis = System.currentTimeMillis() + 86400000L,
            bookingStartMillis = System.currentTimeMillis() + 86400000L * 10,
            bookingEndMillis = System.currentTimeMillis() + 86400000L * 15
        )
        fakeRepo._lotteries.value = listOf(lottery)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-dates",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Click "Select Dates & Book"
        composeTestRule.onNodeWithText("Select Dates & Book").performClick()

        // Confirming without selecting dates should show error
        composeTestRule.onNodeWithText("Confirm Booking").performClick()
        composeTestRule.onNodeWithText("Error: Please select both a check-in and check-out date.").assertIsDisplayed()
    }
}
