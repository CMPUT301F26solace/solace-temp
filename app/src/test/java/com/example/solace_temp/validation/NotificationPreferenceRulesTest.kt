/**
 * NotificationPreferenceRulesTest.kt
 * Purpose: Unit tests for the entrant notification opt-out rules.
 * Issues: Posting to the Android status bar is verified on a device.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.models.AppNotification
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPreferenceRulesTest {
    private val optedIn = Profile(deviceId = "entrant-1", role = UserRole.ENTRANT, receiveNotifications = true)
    private val optedOut = optedIn.copy(receiveNotifications = false)

    private val win = AppNotification(id = "win-1", recipientId = "entrant-1", isLotteryWin = true)
    private val update = AppNotification(id = "update-1", recipientId = "entrant-1", isLotteryWin = false)
    private val seenWin = AppNotification(id = "win-2", recipientId = "entrant-1", isLotteryWin = true, isSeen = true)

    @Test
    fun optedInEntrantGetsNewLotteryWinAlerts() {
        val alerts = NotificationPreferenceRules.systemAlertsToPost(optedIn, listOf(win, update), emptySet())
        assertEquals(listOf(win), alerts)
    }

    @Test
    fun alreadyHandledAlertsAreNotPostedAgain() {
        val alerts = NotificationPreferenceRules.systemAlertsToPost(optedIn, listOf(win), setOf("win-1"))
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun optedOutEntrantGetsNoAlerts() {
        val alerts = NotificationPreferenceRules.systemAlertsToPost(optedOut, listOf(win, update), emptySet())
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun ownerWaitlistMessagesAreAlsoPosted() {
        val broadcast = AppNotification(id = "broadcast-note", recipientId = "entrant-1", broadcastId = "broadcast-1")
        val alerts = NotificationPreferenceRules.systemAlertsToPost(optedIn, listOf(broadcast, update), emptySet())
        assertEquals(listOf(broadcast), alerts)
    }

    @Test
    fun alreadySeenNotificationsAreNotPosted() {
        val alerts = NotificationPreferenceRules.systemAlertsToPost(optedIn, listOf(seenWin), emptySet())
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun badgeCountsUnseenNotificationsWhenOptedIn() {
        assertEquals(2, NotificationPreferenceRules.unreadBadgeCount(optedIn, listOf(win, update, seenWin)))
    }

    @Test
    fun badgeIsHiddenWhenOptedOut() {
        assertEquals(0, NotificationPreferenceRules.unreadBadgeCount(optedOut, listOf(win, update)))
    }

    @Test
    fun badgeIsHiddenForOwners() {
        val owner = optedIn.copy(role = UserRole.CAMPSITE_OWNER)
        assertEquals(0, NotificationPreferenceRules.unreadBadgeCount(owner, listOf(win)))
    }
}
