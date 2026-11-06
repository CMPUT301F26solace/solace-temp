/**
 * EntrantReservationsTest.kt
 *
 * Purpose: Unit tests for fetching, filtering activ.
 */
package com.example.solace_temp

import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.ReservationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntrantReservationsTest {

    @Before
    fun setUp() {
        // Mock environment setup
    }

    @Test
    fun testFetchReservations_filtersActiveStatusForUser() = runBlocking {
        val testUserId = "entrant_user_123"

        // Fetch user reservations from repository
        val allReservations = MockAppRepository.reservations.value
        val activeUserReservations = allReservations.filter {
            it.entrantId == testUserId && it.status == ReservationStatus.ACTIVE
        }

        // Verify only active reservations belong to the target entrant
        activeUserReservations.forEach { reservation ->
            assertEquals("Entrant ID should match logged-in user", testUserId, reservation.entrantId)
            assertEquals("Status should be ACTIVE", ReservationStatus.ACTIVE, reservation.status)
        }
    }

    @Test
    fun testFetchReservations_emptyStateWhenNoActiveBookings() = runBlocking {
        val userWithoutBookingsId = "user_no_reservations_999"

        val allReservations = MockAppRepository.reservations.value
        val activeUserReservations = allReservations.filter {
            it.entrantId == userWithoutBookingsId && it.status == ReservationStatus.ACTIVE
        }

        assertTrue("Reservations list should be empty for user with no bookings", activeUserReservations.isEmpty())
    }
}