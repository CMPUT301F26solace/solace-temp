/**
 * EntrantNotificationsTest.kt
 *
 * Purpose: Unit tests for entrant lottery notification handling and declining invitations.
 */
package com.example.solace_temp

import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.AppNotification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EntrantNotificationsTest {

    private val testNotification = AppNotification(
        id = "test-notif-101",
        campsiteId = "camp-banff-01",
        title = "You Won!",
        message = "Selected for campsite reservation.",
        isLotteryWin = true
    )

    @Before
    fun setUp() {
        // Ensure fresh state before each test run
        MockAppRepository.updateNotifications(listOf(testNotification))
    }

    @Test
    fun testWinningNotificationIsPresent() = runBlocking {
        val notifications = MockAppRepository.notifications.value
        val notification = notifications.firstOrNull { it.id == testNotification.id }

        assertNotNull("Winning notification should be present in repository", notification)
        assertTrue("Notification must be marked as lottery win", notification!!.isLotteryWin)
        assertEquals("camp-banff-01", notification.campsiteId)
    }

    @Test
    fun testDeclineLotteryInvitationRemovesNotification() = runBlocking {
        val initialNotif = MockAppRepository.notifications.value.firstOrNull { it.id == testNotification.id }
        assertNotNull("Initial test notification should exist", initialNotif)

        MockAppRepository.declineLotteryInvitation(initialNotif!!.id, initialNotif.campsiteId)

        val remaining = MockAppRepository.notifications.value
        assertFalse("Notification should be removed after declining", remaining.any { it.id == initialNotif.id })
    }
}