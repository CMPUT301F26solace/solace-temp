/**
 *
 * OwnerScreensTest.kt
 *
 * Purpose: Comprehensive instrumented UI test suite verifying campsite owner workflows,
 * including lottery waitlist viewing, owner access restrictions, waitlist capacity,
 * active reservations, and booked dates calendar functionality.
 *
 */
package com.example.solace_temp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class OwnerScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val site = Campsite(
        id = "camp",
        ownerId = "owner",
        name = "Mountain Camp",
        qrCodePayload = "solace://campsite/camp"
    )

    private class TestRepository : AppRepository by MockAppRepository {
        val profileFlow =
            MutableStateFlow(Profile(deviceId = "owner", role = UserRole.CAMPSITE_OWNER))
        val campsiteFlow = MutableStateFlow<List<Campsite>>(emptyList())
        val lotteryFlow = MutableStateFlow<List<Lottery>>(emptyList())
        val reservationFlow = MutableStateFlow<List<Reservation>>(emptyList())
        override val currentProfile: StateFlow<Profile> = profileFlow
        override val campsites: StateFlow<List<Campsite>> = campsiteFlow
        override val lotteries: StateFlow<List<Lottery>> = lotteryFlow
        override val reservations: StateFlow<List<Reservation>> = reservationFlow
        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
    }

    @Test
    fun ownerSeesWaitlistButOtherUserDoesNot() {
        val repo = TestRepository()
        repo.campsiteFlow.value = listOf(site)
        repo.lotteryFlow.value = listOf(
            Lottery(
                id = "lot", ownerId = "owner",
                campsiteIds = listOf("camp"), waitingListEntrantIds = listOf("entrant1"),
                waitlistCapacity = 2
            )
        )
        rule.setContent { MaterialTheme { OwnerWaitlistScreen("lot", repo) {} } }
        rule.onNodeWithText("Entrant ID: entrant1").assertIsDisplayed()
        rule.onNodeWithText("Capacity: 2").assertIsDisplayed()
        rule.runOnIdle { repo.profileFlow.value = Profile(deviceId = "other") }
        rule.onNodeWithText("Only the campsite owner can view this waitlist.").assertIsDisplayed()
        rule.onNodeWithText("Entrant ID: entrant1").assertDoesNotExist()
    }

    @Test
    fun waitlistDisplaysWaitingSelectedAndConfirmed() {
        val repo = TestRepository()
        repo.campsiteFlow.value = listOf(site)
        repo.lotteryFlow.value = listOf(
            Lottery(
                id = "lot", ownerId = "owner",
                campsiteIds = listOf("camp"), waitingListEntrantIds =
                    listOf("waiting-entrant", "selected-entrant", "confirmed-entrant"),
                invitedWinnerIds = listOf("selected-entrant", "confirmed-entrant")
            )
        )
        repo.reservationFlow.value = listOf(
            Reservation(
                id = "reservation", campsiteId = "camp",
                entrantId = "confirmed-entrant", status = ReservationStatus.ACTIVE
            )
        )
        rule.setContent { MaterialTheme { OwnerWaitlistScreen("lot", repo) {} } }
        rule.onNodeWithText("Status: Waiting").assertIsDisplayed()
        rule.onNodeWithText("Status: Selected").assertIsDisplayed()
        rule.onNodeWithText("Status: Confirmed").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun leavingWaitlistRemovesEntrantInsteadOfShowingCancelled() {
        val repo = TestRepository()
        repo.campsiteFlow.value = listOf(site)
        val lottery = Lottery(
            id = "lot", ownerId = "owner", campsiteIds = listOf("camp"),
            waitingListEntrantIds = listOf("entrant1")
        )
        repo.lotteryFlow.value = listOf(lottery)
        rule.setContent { MaterialTheme { OwnerWaitlistScreen("lot", repo) {} } }
        rule.onNodeWithText("Entrant ID: entrant1").assertIsDisplayed()
        rule.runOnIdle { repo.lotteryFlow.value = listOf(lottery.copy(waitingListEntrantIds = emptyList())) }
        rule.onNodeWithText("No entrants have joined the waitlist yet.").assertIsDisplayed()
        rule.onNodeWithText("Entrant ID: entrant1").assertDoesNotExist()
    }

    @Test
    fun fullWaitlistShowsCapacityMessage() {
        val repo = TestRepository()
        repo.profileFlow.value = Profile(deviceId = "entrant", role = UserRole.ENTRANT)
        repo.campsiteFlow.value = listOf(site)
        val now = System.currentTimeMillis()
        repo.lotteryFlow.value = listOf(
            Lottery(
                id = "lot", ownerId = "owner",
                campsiteIds = listOf("camp"), registrationStartMillis = now - 60_000,
                registrationEndMillis = now + 60_000, bookingEndMillis = now + 120_000,
                waitlistCapacity = 1, waitingListEntrantIds = listOf("existing")
            )
        )
        rule.setContent { MaterialTheme { CampsiteDetailsScreen("camp", repo, {}) } }
        rule.onNodeWithTag("waitlist_status_lot")
            .performScrollTo()
            .assertTextEquals("This waiting list is full.")
        rule.onNodeWithTag("waitlist_join_lot")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun reservationsShowOnlyOwnersConfirmedBookings() {
        val repo = TestRepository()
        repo.campsiteFlow.value = listOf(site, Campsite(id = "other", ownerId = "someone-else"))
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        repo.reservationFlow.value = listOf(
            Reservation(
                id = "owned", campsiteId = "camp", entrantId = "entrant1",
                bookedDates = listOf(today), status = ReservationStatus.ACTIVE
            ),
            Reservation(
                id = "foreign", campsiteId = "other", entrantId = "entrant2",
                bookedDates = listOf(today), status = ReservationStatus.ACTIVE
            ),
            Reservation(
                id = "cancelled", campsiteId = "camp", entrantId = "entrant3",
                bookedDates = listOf(today), status = ReservationStatus.CANCELLED
            )
        )
        rule.setContent { MaterialTheme { OwnerReservationsScreen(repo) {} } }
        rule.onNodeWithTag("reservation_owned").assertIsDisplayed()
        rule.onNodeWithTag("reservation_foreign").assertDoesNotExist()
        rule.onNodeWithTag("reservation_cancelled").assertDoesNotExist()
        rule.onNodeWithTag("reservation_owned").performClick()
        rule.onNodeWithText("Reservation Details").assertIsDisplayed()
    }

    @Test
    fun calendarMarksAndExplainsBookedDate() {
        val repo = TestRepository()
        repo.campsiteFlow.value = listOf(site)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        repo.reservationFlow.value = listOf(
            Reservation(
                id = "reservation1",
                campsiteId = "camp",
                entrantId = "entrant1",
                bookedDates = listOf(today),
                status = ReservationStatus.ACTIVE
            )
        )
        rule.setContent { MaterialTheme { OwnerCalendarScreen(repo) {} } }
        rule.onNodeWithTag("calendar_day_$today").performScrollTo().performClick()
        rule.onNodeWithTag("calendar_day_details").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Booked", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Entrant ID: entrant1").assertIsDisplayed()
    }
}