/**
 * WaitlistNotificationTest.kt
 *
 * Intent tests for campsite owners sending notifications to their lottery waitlist.
 * Uses an in-memory repository instead of Firestore.
 */
package com.example.solace_temp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.BroadcastNotification
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.OwnerWaitlistScreen
import com.example.solace_temp.validation.WaitlistNotificationRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaitlistNotificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val waitlistedLottery = Lottery(
        id = "lot",
        name = "Summer Draw",
        ownerId = "owner",
        campsiteIds = listOf("camp"),
        waitingListEntrantIds = listOf("entrant-1", "entrant-2", "entrant-3")
    )

    private fun launch(lottery: Lottery = waitlistedLottery): NotifyTestRepository {
        val repository = NotifyTestRepository(lottery)
        composeRule.setContent {
            MaterialTheme {
                OwnerWaitlistScreen(lotteryId = lottery.id, repository = repository, onBack = {})
            }
        }
        return repository
    }

    private fun openDialog() {
        composeRule.onNodeWithTag("waitlist_notify_button").performClick()
    }

    private fun fillIn(title: String, message: String) {
        composeRule.onNodeWithTag("waitlist_notify_title").performTextInput(title)
        composeRule.onNodeWithTag("waitlist_notify_message").performTextInput(message)
    }

    @Test
    fun dialogShowsHowManyEntrantsWillBeNotified() {
        launch()
        openDialog()

        // entrant-3 turned notifications off
        composeRule.onNodeWithText("Sends to 2 entrants", substring = true).assertIsDisplayed()
    }

    @Test
    fun blankTitleShowsAnErrorWithoutSending() {
        val repository = launch()
        openDialog()
        composeRule.onNodeWithTag("waitlist_notify_message").performTextInput("Gates open at 9.")
        composeRule.onNodeWithTag("waitlist_notify_send").performClick()

        composeRule.onNodeWithTag("waitlist_notify_error").assertTextEquals("Enter a title.")
        composeRule.runOnIdle { assertEquals(0, repository.sendCalls) }
    }

    @Test
    fun blankMessageShowsAnErrorWithoutSending() {
        val repository = launch()
        openDialog()
        composeRule.onNodeWithTag("waitlist_notify_title").performTextInput("Reminder")
        composeRule.onNodeWithTag("waitlist_notify_send").performClick()

        composeRule.onNodeWithTag("waitlist_notify_error").assertTextEquals("Enter a message.")
        composeRule.runOnIdle { assertEquals(0, repository.sendCalls) }
    }

    @Test
    fun sendingShowsConfirmationWithRecipientCount() {
        val repository = launch()
        openDialog()
        fillIn("Reminder", "Gates open at 9.")
        composeRule.onNodeWithTag("waitlist_notify_send").performClick()

        composeRule.onNodeWithTag("waitlist_notify_feedback")
            .assertTextEquals("Notification sent to 2 entrants.")
        composeRule.onNodeWithTag("waitlist_notify_title").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(1, repository.sendCalls)
            assertEquals("Reminder", repository.sentTitle)
            assertEquals("Gates open at 9.", repository.sentMessage)
        }
    }

    @Test
    fun failedSendShowsErrorAndKeepsTheMessage() {
        val repository = launch()
        repository.failure = Exception("Could not send the notification: Network unavailable")
        openDialog()
        fillIn("Reminder", "Gates open at 9.")
        composeRule.onNodeWithTag("waitlist_notify_send").performClick()

        composeRule.onNodeWithTag("waitlist_notify_error")
            .assertTextEquals("Could not send the notification: Network unavailable")
        composeRule.onNodeWithTag("waitlist_notify_title").assertTextContains("Reminder")
        composeRule.onNodeWithTag("waitlist_notify_feedback").assertDoesNotExist()
    }

    @Test
    fun notifyButtonIsDisabledWhenWaitlistIsEmpty() {
        launch(waitlistedLottery.copy(waitingListEntrantIds = emptyList()))

        composeRule.onNodeWithTag("waitlist_notify_button").assertIsNotEnabled()
    }

    /** Serves one owner, campsite and lottery, and records send requests instead of writing to Firestore. */
    private class NotifyTestRepository(lottery: Lottery) : AppRepository by MockAppRepository {
        override val currentProfile: StateFlow<Profile> =
            MutableStateFlow(Profile(deviceId = "owner", role = UserRole.CAMPSITE_OWNER, isAdmin = false))
        override val campsites: StateFlow<List<Campsite>> =
            MutableStateFlow(listOf(Campsite(id = "camp", ownerId = "owner", name = "Mountain Camp")))
        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(listOf(lottery))
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(
            listOf(
                Profile(deviceId = "entrant-1"),
                Profile(deviceId = "entrant-2"),
                Profile(deviceId = "entrant-3", receiveNotifications = false)
            )
        )

        var failure: Exception? = null
        var sendCalls = 0
            private set
        var sentTitle: String? = null
            private set
        var sentMessage: String? = null
            private set

        override fun sendWaitlistNotification(
            lotteryId: String,
            title: String,
            message: String,
            onResult: (Result<BroadcastNotification>) -> Unit
        ) {
            sendCalls++
            sentTitle = title
            sentMessage = message
            failure?.let {
                onResult(Result.failure(it))
                return
            }
            val lottery = lotteries.value.first { it.id == lotteryId }
            val recipients = WaitlistNotificationRules.recipients(lottery, profiles.value)
            onResult(
                Result.success(
                    BroadcastNotification(
                        id = "broadcast-test",
                        ownerId = "owner",
                        lotteryId = lotteryId,
                        title = title,
                        message = message,
                        recipientIds = recipients,
                        recipientCount = recipients.size
                    )
                )
            )
        }
    }
}
