/**
 * WaitlistNotificationRulesTest.kt
 * Purpose: Unit tests for validating owner waitlist notifications and choosing their recipients.
 * Issues: Firestore batch writes are verified on a device.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.models.AccountStatus
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaitlistNotificationRulesTest {
    private val lottery = Lottery(
        id = "lottery-1",
        ownerId = "owner",
        campsiteIds = listOf("camp-1"),
        waitingListEntrantIds = listOf("entrant-1", "entrant-2", "entrant-3")
    )

    @Test
    fun blankTitleIsRejected() {
        assertEquals("Enter a title.", WaitlistNotificationRules.messageError("   ", "Gates open at 9."))
    }

    @Test
    fun blankMessageIsRejected() {
        assertEquals("Enter a message.", WaitlistNotificationRules.messageError("Reminder", ""))
    }

    @Test
    fun filledInMessageIsAccepted() {
        assertNull(WaitlistNotificationRules.messageError("Reminder", "Gates open at 9."))
    }

    @Test
    fun everyWaitlistedEntrantIsARecipientByDefault() {
        val profiles = listOf(Profile(deviceId = "entrant-1"), Profile(deviceId = "entrant-2"))
        assertEquals(
            listOf("entrant-1", "entrant-2", "entrant-3"),
            WaitlistNotificationRules.recipients(lottery, profiles)
        )
    }

    @Test
    fun optedOutEntrantsAreSkipped() {
        val profiles = listOf(
            Profile(deviceId = "entrant-1"),
            Profile(deviceId = "entrant-2", receiveNotifications = false)
        )
        assertEquals(
            listOf("entrant-1", "entrant-3"),
            WaitlistNotificationRules.recipients(lottery, profiles)
        )
    }

    @Test
    fun inactiveAccountsAreSkipped() {
        val profiles = listOf(
            Profile(deviceId = "entrant-1", accountStatus = AccountStatus.SUSPENDED),
            Profile(deviceId = "entrant-2", accountStatus = AccountStatus.INACTIVE)
        )
        assertEquals(listOf("entrant-3"), WaitlistNotificationRules.recipients(lottery, profiles))
    }

    @Test
    fun duplicateWaitlistEntriesAreNotifiedOnce() {
        val duplicated = lottery.copy(waitingListEntrantIds = listOf("entrant-1", "entrant-1"))
        assertEquals(listOf("entrant-1"), WaitlistNotificationRules.recipients(duplicated, emptyList()))
    }

    @Test
    fun onlyTheOwnerCanSend() {
        assertEquals(
            "Only the lottery owner can notify its waitlist.",
            WaitlistNotificationRules.sendError(lottery, "someone-else", "Reminder", "Hi", listOf("entrant-1"))
        )
    }

    @Test
    fun sendingRequiresAtLeastOneRecipient() {
        assertEquals(
            "No entrants on this waitlist can receive notifications.",
            WaitlistNotificationRules.sendError(lottery, "owner", "Reminder", "Hi", emptyList())
        )
    }

    @Test
    fun validOwnerMessageCanBeSent() {
        assertNull(WaitlistNotificationRules.sendError(lottery, "owner", "Reminder", "Hi", listOf("entrant-1")))
    }
}
