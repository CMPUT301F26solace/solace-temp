/**
 * CampsiteReservationTest.kt
 *
 * Purpose: Unit tests for confirming date selection, reservation creation,
 * and notification cleanup.
 */
package com.example.solace_temp

import com.example.solace_temp.data.MockAppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CampsiteReservationTest {

    @Before
    fun setUp() {
        // Reset or seed mock data state if needed before each run
    }

    @Test
    fun testCreateReservation_success() = runBlocking {
        val campsiteId = "camp_001"
        val notificationId = "notif_win_001"
        val startDate = "2026-07-01"
        val endDate = "2026-07-07"

        // Execute reservation creation via repository
        val result = MockAppRepository.createReservation(
            campsiteId = campsiteId,
            notificationId = notificationId,
            startDate = startDate,
            endDate = endDate
        )

        assertTrue("Reservation should be created successfully", result)
    }

    @Test
    fun testCreateReservation_clearsNotification() = runBlocking {
        val campsiteId = "camp_002"
        val notificationId = "notif_win_002"

        // Create reservation
        MockAppRepository.createReservation(
            campsiteId = campsiteId,
            notificationId = notificationId,
            startDate = "2026-08-10",
            endDate = "2026-08-15"
        )

        // Verify the winning notification was removed from active notifications
        val remainingNotifications = MockAppRepository.notifications.value
        val notificationStillExists = remainingNotifications.any { it.id == notificationId }

        assertFalse("Winning notification should be removed after booking", notificationStillExists)
    }
}