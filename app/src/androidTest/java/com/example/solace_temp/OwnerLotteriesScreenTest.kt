package com.example.solace_temp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.WaitingListErrorCallback
import com.example.solace_temp.models.AppNotification
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.LotteryDetailsScreen
import com.example.solace_temp.ui.screens.OwnerLotteriesScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerLotteriesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeOwnerLotteryRepository : AppRepository {
        val _currentProfile = MutableStateFlow(
            Profile(deviceId = "owner-1", role = UserRole.CAMPSITE_OWNER)
        )
        val _profiles = MutableStateFlow(
            listOf(Profile(deviceId = "winner-1", name = "Alice Trailblazer"))
        )
        val _campsites = MutableStateFlow(
            listOf(
                Campsite(id = "camp-1", ownerId = "owner-1", name = "Alpine Ridge"),
                Campsite(id = "camp-2", ownerId = "owner-1", name = "Cedar Creek")
            )
        )
        val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())

        override val currentProfile: StateFlow<Profile> = _currentProfile
        override val profiles: StateFlow<List<Profile>> = _profiles
        override val campsites: StateFlow<List<Campsite>> = _campsites
        override val lotteries: StateFlow<List<Lottery>> = _lotteries
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
        override suspend fun createReservation(
            campsiteId: String,
            notificationId: String,
            startDate: String,
            endDate: String
        ): Boolean = false
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
        override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {}
    }

    @Test
    fun ownerLotteries_kpisSearchAndStatusPills_filterFeed() {
        val repo = FakeOwnerLotteryRepository()
        val now = System.currentTimeMillis()
        repo._lotteries.value = listOf(
            Lottery(
                id = "lot-active",
                name = "Summer Alpine Draw",
                campsiteIds = listOf("camp-1"),
                ownerId = "owner-1",
                registrationStartMillis = now - 60_000L,
                registrationEndMillis = now + 60_000L,
                bookingStartMillis = now + 120_000L,
                bookingEndMillis = now + 240_000L,
                waitingListEntrantIds = listOf("e1", "e2")
            ),
            Lottery(
                id = "lot-upcoming",
                name = "Autumn Cedar Draw",
                campsiteIds = listOf("camp-2"),
                ownerId = "owner-1",
                registrationStartMillis = now + 120_000L,
                registrationEndMillis = now + 240_000L,
                bookingStartMillis = now + 360_000L,
                bookingEndMillis = now + 480_000L
            )
        )

        var selectedLotteryId: String? = null
        composeTestRule.setContent {
            OwnerLotteriesScreen(
                repository = repo,
                onSelectLottery = { selectedLotteryId = it }
            )
        }

        // Verify Manage Lotteries header and KPI labels
        composeTestRule.onNodeWithText("Manage Lotteries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("In Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Waitlist").assertIsDisplayed()

        // Filter to Upcoming
        composeTestRule.onNodeWithTag("lottery_filter_UPCOMING").performClick()
        composeTestRule.onNodeWithText("Autumn Cedar Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summer Alpine Draw").assertDoesNotExist()

        // Filter to Active
        composeTestRule.onNodeWithTag("lottery_filter_ACTIVE").performClick()
        composeTestRule.onNodeWithText("Summer Alpine Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Autumn Cedar Draw").assertDoesNotExist()

        // Reset to All and search
        composeTestRule.onNodeWithTag("lottery_filter_ALL").performClick()
        composeTestRule.onNodeWithTag("owner_lotteries_search").performTextInput("Cedar")
        composeTestRule.onNodeWithText("Autumn Cedar Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summer Alpine Draw").assertDoesNotExist()

        // Tap card
        composeTestRule.onNodeWithText("Autumn Cedar Draw").performClick()
        assertEquals("lot-upcoming", selectedLotteryId)
    }

    @Test
    fun lotteryDetails_drawnLottery_supportsOneTapWinnerReveal() {
        val repo = FakeOwnerLotteryRepository()
        val now = System.currentTimeMillis()
        repo._lotteries.value = listOf(
            Lottery(
                id = "lot-drawn",
                name = "Drawn Peak Draw",
                campsiteIds = listOf("camp-1"),
                ownerId = "owner-1",
                registrationStartMillis = now - 240_000L,
                registrationEndMillis = now - 120_000L,
                bookingStartMillis = now + 60_000L,
                bookingEndMillis = now + 240_000L,
                invitedWinnerIds = listOf("winner-1"),
                isDrawn = true
            )
        )

        composeTestRule.setContent {
            LotteryDetailsScreen(
                lotteryId = "lot-drawn",
                repository = repo,
                onBack = {},
                onSelectCampsite = {},
                onViewWaitlist = {}
            )
        }

        composeTestRule.onNodeWithText("Winner Reveal").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_reveal_winners_instantly")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("Alice Trailblazer").performScrollTo().assertIsDisplayed()
    }
}
