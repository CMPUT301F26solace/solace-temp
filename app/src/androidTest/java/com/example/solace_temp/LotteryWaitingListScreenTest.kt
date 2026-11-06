/**
 * LotteryWaitingListScreenTest.kt
 * Purpose: Entrant interaction tests for US 01.01.06, US 01.02.01
 * and US 01.02.02, using controlled save results instead of a live database.
 * Issues: Live Firestore persistence is checked during the app demonstration.
 */
package com.example.solace_temp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import com.example.solace_temp.ui.screens.LotteryWaitingListSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LotteryWaitingListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var repository: WaitingListTestRepository

    private val joinTag = "waitlist_join_lottery-test"
    private val leaveTag = "waitlist_leave_lottery-test"
    private val feedbackTag = "waitlist_feedback_lottery-test"

    @Before
    fun setUp() {
        repository = WaitingListTestRepository()
    }

    private fun showSection() {
        compose.setContent {
            MaterialTheme {
                LotteryWaitingListSection("camp-banff-01", repository)
            }
        }
    }

    @Test
    fun joinsFromCampsiteDetailsAndWaitsForSuccessfulSave() {
        compose.setContent {
            MaterialTheme {
                CampsiteDetailsScreen(
                    "camp-banff-01",
                    repository,
                    onBack = {}
                )
            }
        }

        compose.onNodeWithTag(joinTag)
            .performScrollTo()
            .performClick()
            .assertIsNotEnabled()

        compose.onNodeWithTag(feedbackTag).assertDoesNotExist()
        assertEquals(1, repository.joinCalls)

        compose.runOnIdle {
            repository.complete(Result.success(Unit))
        }

        compose.onNodeWithTag(joinTag).assertIsNotEnabled()
        compose.onNodeWithTag(leaveTag).assertIsEnabled()

        compose.onNodeWithTag(feedbackTag)
            .assertTextEquals("Joined the waiting list.")

        assertEquals(
            listOf("device-entrant"),
            repository.lotteries.value.single().waitingListEntrantIds
        )
    }

    @Test
    fun failedJoinKeepsEntrantOffListAndAllowsRetry() {
        showSection()

        compose.onNodeWithTag(joinTag).performClick()

        compose.runOnIdle {
            repository.complete(
                Result.failure(Exception("network unavailable"))
            )
        }

        compose.onNodeWithTag(feedbackTag)
            .assertTextContains("Could not update", substring = true)

        compose.onNodeWithTag(joinTag)
            .assertIsEnabled()
            .performClick()

        assertEquals(2, repository.joinCalls)

        assertEquals(
            emptyList<String>(),
            repository.lotteries.value.single().waitingListEntrantIds
        )
    }

    @Test
    fun leavingRequiresConfirmationAndCancelDoesNotWrite() {
        repository.setMembers(listOf("device-entrant", "other-entrant"))
        showSection()

        compose.onNodeWithTag(leaveTag).performClick()

        compose.onNodeWithTag("waitlist_cancel_leave_lottery-test")
            .performClick()

        assertEquals(0, repository.leaveCalls)

        compose.onNodeWithTag(leaveTag).performClick()

        compose.onNodeWithTag("waitlist_confirm_leave_lottery-test")
            .performClick()

        compose.onNodeWithTag(leaveTag).assertIsNotEnabled()

        compose.runOnIdle {
            repository.complete(Result.success(Unit))
        }

        compose.onNodeWithTag(feedbackTag)
            .assertTextEquals("Left the waiting list.")

        compose.onNodeWithTag(joinTag).assertIsEnabled()

        assertEquals(
            listOf("other-entrant"),
            repository.lotteries.value.single().waitingListEntrantIds
        )
    }

    @Test
    fun failedLeaveKeepsMembershipAndAllowsRetry() {
        repository.setMembers(listOf("device-entrant"))
        showSection()

        compose.onNodeWithTag(leaveTag).performClick()

        compose.onNodeWithTag("waitlist_confirm_leave_lottery-test")
            .performClick()

        compose.runOnIdle {
            repository.complete(
                Result.failure(Exception("network unavailable"))
            )
        }

        compose.onNodeWithTag(leaveTag).assertIsEnabled()
        compose.onNodeWithTag(joinTag).assertIsNotEnabled()

        compose.onNodeWithTag(feedbackTag)
            .assertTextContains("Could not update", substring = true)
    }

    @Test
    fun fullListPreventsNewEntryButMembersCanLeave() {
        repository.lotteries.value = listOf(
            repository.lotteries.value.single().copy(
                waitlistCapacity = 1,
                waitingListEntrantIds = listOf("other-entrant")
            )
        )

        showSection()

        compose.onNodeWithTag(joinTag).assertIsNotEnabled()

        compose.onNodeWithTag("waitlist_status_lottery-test")
            .assertTextEquals("This waiting list is full.")

        compose.runOnIdle {
            repository.setMembers(listOf("device-entrant"))
        }

        compose.onNodeWithTag(leaveTag).assertIsEnabled()
    }

    @Test
    fun closedListPreventsJoiningAndStillAllowsLeaving() {
        repository.lotteries.value = listOf(
            repository.lotteries.value.single().copy(
                bookingEndMillis = 1
            )
        )

        showSection()

        compose.onNodeWithTag(joinTag).assertIsNotEnabled()

        compose.runOnIdle {
            repository.setMembers(listOf("device-entrant"))
        }

        compose.onNodeWithTag(leaveTag).assertIsEnabled()
    }

    @Test
    fun detailsShowOnlyLotteriesForTheSelectedCampsite() {
        val unrelated = repository.lotteries.value.single().copy(
            id = "unrelated",
            campsiteIds = listOf("camp-jasper-01")
        )

        repository.lotteries.value =
            repository.lotteries.value + unrelated

        showSection()

        compose.onNodeWithTag(joinTag).assertIsEnabled()
        compose.onNodeWithTag("waitlist_join_unrelated").assertDoesNotExist()
    }

    @Test
    fun noLotteryShowsAnExplanationWithoutJoinControls() {
        repository.lotteries.value = emptyList()
        showSection()

        compose.onNodeWithTag("waitlist_empty").assertIsDisplayed()
        compose.onNodeWithTag(joinTag).assertDoesNotExist()
    }
}

/** Holds campsite data and lets tests control asynchronous save completion. */
private class WaitingListTestRepository :
    AppRepository by MockAppRepository {

    override val currentProfile = MutableStateFlow(
        Profile(
            deviceId = "device-entrant",
            name = "Test Camper",
            email = "camper@example.com"
        )
    )

    override val campsites = MutableStateFlow(
        listOf(
            Campsite(
                id = "camp-banff-01",
                ownerId = "owner-1",
                name = "Tunnel Mountain Village I",
                description = "Mountain views near Banff townsite.",
                qrCodePayload = "solace://campsite/camp-banff-01"
            )
        )
    )

    override val lotteries = MutableStateFlow(
        listOf(
            Lottery(
                id = "lottery-test",
                name = "Banff Weekend Lottery",
                campsiteIds = listOf("camp-banff-01"),
                registrationStartMillis = 0,
                registrationEndMillis = 1,
                bookingEndMillis = Long.MAX_VALUE,
                isDrawn = true
            )
        )
    )

    var joinCalls = 0
    var leaveCalls = 0

    private var pending: ((Result<Unit>) -> Unit)? = null
    private var joining = false

    override fun joinWaitingList(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        check(pending == null)
        joinCalls++
        joining = true
        pending = onResult
    }

    override fun leaveWaitingList(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        check(pending == null)
        leaveCalls++
        joining = false
        pending = onResult
    }

    fun setMembers(members: List<String>) {
        lotteries.value = lotteries.value.map {
            it.copy(waitingListEntrantIds = members)
        }
    }

    fun complete(result: Result<Unit>) {
        val callback = checkNotNull(pending)
        pending = null

        if (result.isSuccess) {
            val members = lotteries.value.single().waitingListEntrantIds

            setMembers(
                if (joining) {
                    (members + "device-entrant").distinct()
                } else {
                    members - "device-entrant"
                }
            )
        }

        callback(result)
    }
}