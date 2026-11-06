/**
 * LotterySystemAndRedrawTest.kt
 *
 * Purpose: Instrumented test suite extensively testing the lottery system, automated draws,
 * backup redraws, and UI features across Upcoming, Active, Booking, and Finished states.
 *
 * Outstanding Issues:
 * - None.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.LotteryDetailsScreen
import com.example.solace_temp.ui.screens.getLotteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LotterySystemAndRedrawTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Fake repository simulating lottery states, waitlists, winner draws, and reservations for testing.
     */
    class ComprehensiveLotteryFakeRepository : AppRepository {
        private val _currentProfile = MutableStateFlow(Profile(deviceId = "test-user-id", role = UserRole.CAMPSITE_OWNER))
        override val currentProfile: StateFlow<Profile> = _currentProfile

        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(listOf(
            Profile(deviceId = "user-1", name = "Entrant One"),
            Profile(deviceId = "user-2", name = "Entrant Two"),
            Profile(deviceId = "user-3", name = "Entrant Three")
        ))

        private val _campsites = MutableStateFlow(listOf(
            Campsite(
                id = "camp-1",
                ownerId = "test-user-id",
                name = "Test Campsite",
                bookingStartDateMillis = System.currentTimeMillis() + 100000L,
                bookingEndDateMillis = System.currentTimeMillis() + 864000000L
            )
        ))
        override val campsites: StateFlow<List<Campsite>> = _campsites

        val now = System.currentTimeMillis()
        private val _lotteries = MutableStateFlow(listOf(
            Lottery(
                id = "lot-upcoming",
                name = "Upcoming Lottery",
                campsiteIds = listOf("camp-1"),
                ownerId = "test-user-id",
                registrationStartMillis = now + 1000000L,
                registrationEndMillis = now + 2000000L,
                maxWinners = 1,
                waitingListEntrantIds = listOf("user-1")
            ),
            Lottery(
                id = "lot-active",
                name = "Active Lottery",
                campsiteIds = listOf("camp-1"),
                ownerId = "test-user-id",
                registrationStartMillis = now - 100000L,
                registrationEndMillis = now + 1000000L,
                maxWinners = 1,
                waitingListEntrantIds = listOf("user-1", "user-2")
            ),
            Lottery(
                id = "lot-booking",
                name = "Booking Lottery",
                campsiteIds = listOf("camp-1"),
                ownerId = "test-user-id",
                registrationStartMillis = now - 2000000L,
                registrationEndMillis = now - 1000000L,
                bookingEndMillis = now + 864000000L,
                maxWinners = 1,
                waitingListEntrantIds = listOf("user-1", "user-2", "user-3"),
                invitedWinnerIds = listOf("user-1"),
                isDrawn = true
            )
        ))
        override val lotteries: StateFlow<List<Lottery>> = _lotteries

        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)

        private val _reservations = MutableStateFlow(listOf(
            Reservation(
                id = "res-1",
                campsiteId = "camp-1",
                campsiteName = "Test Campsite",
                entrantId = "user-1",
                ownerId = "test-user-id",
                bookedDates = listOf("2027-05-01"),
                status = ReservationStatus.ACTIVE
            )
        ))
        override val reservations: StateFlow<List<Reservation>> = _reservations

        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery

        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: (String?) -> Unit) {
            onResult(null)
        }

        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}

        override fun drawLotteryWinners(lotteryId: String) {
            _lotteries.value = _lotteries.value.map { lot ->
                if (lot.id == lotteryId) {
                    val count = minOf(lot.waitingListEntrantIds.size, lot.maxWinners)
                    val winners = lot.waitingListEntrantIds.shuffled().take(count)
                    lot.copy(invitedWinnerIds = winners, isDrawn = true)
                } else lot
            }
        }

        override fun redrawLottery(lotteryId: String) {
            _lotteries.value = _lotteries.value.map { lot ->
                if (lot.id == lotteryId) {
                    val remaining = lot.waitingListEntrantIds.filter { it !in lot.invitedWinnerIds }
                    val needed = lot.maxWinners - lot.invitedWinnerIds.size
                    val newWinners = remaining.shuffled().take(needed)
                    lot.copy(invitedWinnerIds = lot.invitedWinnerIds + newWinners, isDrawn = true)
                } else lot
            }
        }

        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun testLotteryStatusDetermination() {
        val now = System.currentTimeMillis()
        val upcoming = Lottery(registrationStartMillis = now + 50000L, registrationEndMillis = now + 100000L)
        assertEquals("Upcoming", getLotteryStatus(upcoming))

        val active = Lottery(registrationStartMillis = now - 50000L, registrationEndMillis = now + 50000L)
        assertEquals("Active", getLotteryStatus(active))

        val finished = Lottery(registrationStartMillis = now - 200000L, registrationEndMillis = now - 100000L, bookingEndMillis = now - 1000L)
        assertEquals("Finished", getLotteryStatus(finished))
    }

    @Test
    fun testDrawLotteryWinnersAndRedraw() {
        val repo = ComprehensiveLotteryFakeRepository()
        val lotteryId = "lot-active"

        // Perform draw
        repo.drawLotteryWinners(lotteryId)
        val lotteryAfterDraw = repo.lotteries.value.find { it.id == lotteryId }!!
        assertTrue(lotteryAfterDraw.isDrawn)
        assertTrue(lotteryAfterDraw.invitedWinnerIds.isNotEmpty())
        assertTrue(lotteryAfterDraw.invitedWinnerIds.size <= lotteryAfterDraw.maxWinners)

        // Perform redraw / backup draw
        repo.redrawLottery(lotteryId)
        val lotteryAfterRedraw = repo.lotteries.value.find { it.id == lotteryId }!!
        assertTrue(lotteryAfterRedraw.invitedWinnerIds.size >= lotteryAfterDraw.invitedWinnerIds.size)
    }

    @Test
    fun testLotteryDetailsScreenUpcomingState() {
        val repo = ComprehensiveLotteryFakeRepository()
        composeTestRule.setContent {
            LotteryDetailsScreen(
                lotteryId = "lot-upcoming",
                repository = repo,
                onBack = {},
                onSelectCampsite = {},
                onViewWaitlist = {}
            )
        }

        composeTestRule.onAllNodesWithText("Upcoming Lottery").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery opening in:").assertIsDisplayed()
    }

    @Test
    fun testLotteryDetailsScreenActiveState() {
        val repo = ComprehensiveLotteryFakeRepository()
        composeTestRule.setContent {
            LotteryDetailsScreen(
                lotteryId = "lot-active",
                repository = repo,
                onBack = {},
                onSelectCampsite = {},
                onViewWaitlist = {}
            )
        }

        composeTestRule.onAllNodesWithText("Active Lottery").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery drawing in:").assertIsDisplayed()
    }

    @Test
    fun testLotteryDetailsScreenBookingStateAndRedraw() {
        val repo = ComprehensiveLotteryFakeRepository()
        composeTestRule.setContent {
            LotteryDetailsScreen(
                lotteryId = "lot-booking",
                repository = repo,
                onBack = {},
                onSelectCampsite = {},
                onViewWaitlist = {}
            )
        }

        composeTestRule.onAllNodesWithText("Booking Lottery").onFirst().assertIsDisplayed()
        // Check reserved count display
        composeTestRule.onNodeWithText("People who have reserved: 1").assertIsDisplayed()
        // Check Redraw button
        composeTestRule.onNodeWithText("Redraw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Redraw").performClick()
    }
}
