package com.example.solace_temp.ui.screens

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerBookingUtilsTest {
    private val site = Campsite(id = "owned", ownerId = "owner", name = "Test Site",
        availableDates = listOf("2027-05-10", "2027-05-11", "2027-05-12"))
    private val otherSite = Campsite(id = "other", ownerId = "someone-else")

    @Test fun onlyShowsFutureConfirmedReservationsForOwnedCampsites() {
        val reservations = listOf(
            Reservation(id = "good", campsiteId = "owned", bookedDates = listOf("2027-05-10"),
                status = ReservationStatus.ACTIVE),
            Reservation(id = "other", campsiteId = "other", bookedDates = listOf("2027-05-11"),
                status = ReservationStatus.ACTIVE),
            Reservation(id = "old", campsiteId = "owned", bookedDates = listOf("2025-01-01"),
                status = ReservationStatus.ACTIVE),
            Reservation(id = "cancel", campsiteId = "owned", bookedDates = listOf("2027-05-12"),
                status = ReservationStatus.CANCELLED)
        )
        assertEquals(listOf("good"), activeOwnerReservations("owner", listOf(site, otherSite),
            reservations, today = "2026-11-05").map { it.id })
    }

    @Test fun bookedDatesTakePriorityOverAvailableDates() {
        val booking = Reservation(id = "r1", campsiteId = "owned", entrantId = "e1",
            bookedDates = listOf("2027-05-10", "2027-05-11"))
        val byDay = campsiteBookingsByDay("owned", listOf(booking))
        assertEquals(CalendarDayStatus.BOOKED, calendarDayStatus(site, "2027-05-10", byDay))
        assertEquals(CalendarDayStatus.BOOKED, calendarDayStatus(site, "2027-05-11", byDay))
        assertEquals(CalendarDayStatus.AVAILABLE, calendarDayStatus(site, "2027-05-12", byDay))
        assertEquals(CalendarDayStatus.BLOCKED, calendarDayStatus(site, "2027-05-13", byDay))
        assertEquals("2027-05-10 to 2027-05-11", reservationDateRange(booking))
    }

    @Test fun ignoresCancelledAndInvalidBookedDays() {
        val cancelled = Reservation(campsiteId = "owned", status = ReservationStatus.CANCELLED,
            bookedDates = listOf("2027-05-10"))
        val invalid = Reservation(campsiteId = "owned", status = ReservationStatus.ACTIVE,
            bookedDates = listOf("not-a-date"))
        assertTrue(campsiteBookingsByDay("owned", listOf(cancelled, invalid)).isEmpty())
    }
}
