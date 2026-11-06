package com.example.solace_temp.data

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BookingValidationTest {
    private fun date(value: String): Long = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(value)!!.time

    private val campsite = Campsite(id = "camp", ownerId = "owner")
    private val lottery = Lottery(
        id = "lottery", ownerId = "owner", campsiteIds = listOf("camp"),
        invitedWinnerIds = listOf("winner")
    )

    @Test fun bookedNightsExcludeCheckoutDay() {
        assertEquals(
            listOf("2027-05-07", "2027-05-08"),
            bookingNights(date("2027-05-07"), date("2027-05-09"))
        )
    }

    @Test fun rejectsEntrantsWhoDidNotWin() {
        assertEquals(
            "Only entrants selected in the lottery can confirm a reservation.",
            bookingValidationError(campsite, lottery, "not-winner", date("2027-05-07"),
                date("2027-05-09"), emptyList())
        )
    }

    @Test fun rejectsOverlappingConfirmedNights() {
        val reserved = Reservation(
            id = "existing", campsiteId = "camp", entrantId = "another",
            bookedDates = listOf("2027-05-08"), status = ReservationStatus.ACTIVE
        )
        assertEquals(
            "One or more selected dates are already booked.",
            bookingValidationError(campsite, lottery, "winner", date("2027-05-07"),
                date("2027-05-09"), listOf(reserved))
        )
    }

    @Test fun winnerCanBookAvailableNights() {
        assertNull(bookingValidationError(
            campsite, lottery, "winner", date("2027-05-07"), date("2027-05-09"), emptyList()
        ))
    }

    @Test fun rejectsCheckInInThePast() {
        assertEquals(
            "Check-in date cannot be in the past.",
            bookingValidationError(campsite, lottery, "winner", date("2027-05-07"),
                date("2027-05-09"), emptyList(), nowMillis = date("2027-05-10"))
        )
    }

    @Test fun rejectsBookingBeforeBookingWindow() {
        val future = lottery.copy(bookingStartMillis = date("2027-05-01"))
        assertEquals(
            "The lottery booking period has not started yet.",
            bookingValidationError(campsite, future, "winner", date("2027-05-07"),
                date("2027-05-09"), emptyList(), nowMillis = date("2027-04-01"))
        )
    }

    @Test fun rejectsNightsNotListedAsAvailable() {
        val limited = campsite.copy(availableDates = listOf("2027-05-07"))
        assertEquals(
            "One or more selected nights are unavailable.",
            bookingValidationError(limited, lottery, "winner", date("2027-05-07"),
                date("2027-05-09"), emptyList())
        )
    }
}
