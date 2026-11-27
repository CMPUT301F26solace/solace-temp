package com.example.solace_temp.ui.screens

import com.example.solace_temp.data.bookingValidationError
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LotteryStatusTest {
    @Test
    fun lotteryRemainsInBookingAfterFirstDraw() {
        val now = System.currentTimeMillis()
        val lottery = Lottery(
            registrationStartMillis = now - 120_000,
            registrationEndMillis = now - 60_000,
            bookingStartMillis = now - 30_000,
            bookingEndMillis = now + 120_000,
            isDrawn = true
        )
        assertEquals("Booking", getLotteryStatus(lottery))
    }

    @Test
    fun fastForwardLotteryUpdatesRegistrationDatesAndPreservesBookingDates() {
        val now = 1_700_000_000_000L
        val dayMillis = 86_400_000L
        val original = Lottery(
            id = "lottery-test",
            campsiteIds = listOf("site-1"),
            ownerId = "owner-1",
            registrationStartMillis = now + 1 * dayMillis,
            registrationEndMillis = now + 5 * dayMillis,
            registrationStart = "2023-11-15",
            registrationEnd = "2023-11-20",
            bookingStartMillis = now + 10 * dayMillis,
            bookingEndMillis = now + 40 * dayMillis,
            bookingStart = "2023-11-25",
            bookingEnd = "2023-12-25",
            isDrawn = false
        )

        val updated = fastForwardLotteryForTesting(original, now)

        // Registration end should be 1 minute from now
        assertEquals(now + 60_000L, updated.registrationEndMillis)
        assertEquals(formatDateMillis(now + 60_000L), updated.registrationEnd)

        // Registration start should be in the past or now - 60s
        assertEquals(now - 60_000L, updated.registrationStartMillis)
        assertEquals(formatDateMillis(now - 60_000L), updated.registrationStart)

        // Booking dates must be preserved exactly as originally configured
        assertEquals(original.bookingStartMillis, updated.bookingStartMillis)
        assertEquals(original.bookingEndMillis, updated.bookingEndMillis)
        assertEquals(original.bookingStart, updated.bookingStart)
        assertEquals(original.bookingEnd, updated.bookingEnd)

        // Should not be marked drawn yet (until deadline passes or owner triggers draw)
        assertFalse(updated.isDrawn)
    }

    @Test
    fun drawnWinnerCanBookWithinAllowedCampsiteBookingDates() {
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L

        // Booking availability window is set far in the future (e.g. 30 days ahead)
        val bookStart = now + 30 * dayMillis
        val bookEnd = now + 60 * dayMillis

        val originalLottery = Lottery(
            id = "lottery-test",
            campsiteIds = listOf("site-1"),
            ownerId = "owner-1",
            registrationStartMillis = now - 120_000L,
            registrationEndMillis = now + 60_000L,
            bookingStartMillis = bookStart,
            bookingEndMillis = bookEnd
        )

        // Fast-forward lottery for testing
        val fastForwarded = fastForwardLotteryForTesting(originalLottery, now)

        // Simulate 1 minute passing and winner being drawn
        val drawn = fastForwarded.copy(
            isDrawn = true,
            invitedWinnerIds = listOf("entrant-1")
        )

        val campsite = Campsite(
            id = "site-1",
            ownerId = "owner-1",
            bookingStartDateMillis = null,
            bookingEndDateMillis = null
        )

        // Entrant selects dates within the campsite's allowed booking window
        val checkIn = (bookStart / dayMillis) * dayMillis + 2 * dayMillis
        val checkOut = checkIn + 3 * dayMillis

        val error = bookingValidationError(
            campsite = campsite,
            lottery = drawn,
            entrantId = "entrant-1",
            checkInMillis = checkIn,
            checkOutMillis = checkOut,
            reservations = emptyList(),
            nowMillis = now + 65_000L // Even though wall-clock time is well before the stay window
        )

        assertNull("Winner should be able to book within the booking window immediately upon winning, but got: $error", error)

        // Dates outside the allowed booking window must still be rejected
        val outsideError = bookingValidationError(
            campsite = campsite,
            lottery = drawn,
            entrantId = "entrant-1",
            checkInMillis = bookEnd + 5 * dayMillis,
            checkOutMillis = bookEnd + 7 * dayMillis,
            reservations = emptyList(),
            nowMillis = now + 65_000L
        )

        assertEquals("Selected dates are outside the allowed campsite booking dates.", outsideError)
    }

    @Test
    fun campsiteAvailability_whenNoLottery_returnsTrue() {
        val campsite = Campsite(id = "site-1")
        val lotteries = emptyList<Lottery>()
        org.junit.Assert.assertTrue(isCampsiteAvailableForLottery(campsite, lotteries))
    }

    @Test
    fun campsiteAvailability_whenUpcomingLottery_returnsFalse() {
        val now = System.currentTimeMillis()
        val campsite = Campsite(id = "site-1")
        val upcoming = Lottery(
            id = "lot-1",
            campsiteIds = listOf("site-1"),
            registrationStartMillis = now + 50_000L,
            registrationEndMillis = now + 100_000L
        )
        assertFalse(isCampsiteAvailableForLottery(campsite, listOf(upcoming)))
    }

    @Test
    fun campsiteAvailability_whenActiveLottery_returnsFalse() {
        val now = System.currentTimeMillis()
        val campsite = Campsite(id = "site-1")
        val active = Lottery(
            id = "lot-1",
            campsiteIds = listOf("site-1"),
            registrationStartMillis = now - 50_000L,
            registrationEndMillis = now + 50_000L
        )
        assertFalse(isCampsiteAvailableForLottery(campsite, listOf(active)))
    }

    @Test
    fun campsiteAvailability_whenDrawnBookingLottery_returnsFalse() {
        val now = System.currentTimeMillis()
        val campsite = Campsite(id = "site-1")
        val drawn = Lottery(
            id = "lot-1",
            campsiteIds = listOf("site-1"),
            registrationStartMillis = now - 100_000L,
            registrationEndMillis = now - 50_000L,
            bookingStartMillis = now + 50_000L,
            bookingEndMillis = now + 200_000L,
            isDrawn = true
        )
        assertFalse(isCampsiteAvailableForLottery(campsite, listOf(drawn)))
    }

    @Test
    fun campsiteAvailability_whenFinishedLottery_returnsTrue() {
        val now = System.currentTimeMillis()
        val campsite = Campsite(id = "site-1")
        val finished = Lottery(
            id = "lot-1",
            campsiteIds = listOf("site-1"),
            registrationStartMillis = now - 200_000L,
            registrationEndMillis = now - 100_000L,
            bookingStartMillis = now - 80_000L,
            bookingEndMillis = now - 10_000L,
            isDrawn = true
        )
        org.junit.Assert.assertTrue(isCampsiteAvailableForLottery(campsite, listOf(finished)))
    }

    @Test
    fun campsiteAvailability_whenLotteryBelongsToAnotherCampsite_returnsTrue() {
        val now = System.currentTimeMillis()
        val campsite = Campsite(id = "site-1")
        val otherLottery = Lottery(
            id = "lot-2",
            campsiteIds = listOf("site-2"),
            registrationStartMillis = now - 50_000L,
            registrationEndMillis = now + 50_000L
        )
        org.junit.Assert.assertTrue(isCampsiteAvailableForLottery(campsite, listOf(otherLottery)))
    }
}
