package com.example.solace_temp.data

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.ReservationStatus
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.CalendarDayStatus
import com.example.solace_temp.ui.screens.activeOwnerReservations
import com.example.solace_temp.ui.screens.calendarDayStatus
import com.example.solace_temp.ui.screens.campsiteBookingsByDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/** Exercises the owner/entrant workflow using the app's in-memory repository. */
class LotteryToBookingWorkflowTest {
    @Test
    fun lotteryWinnersCanBookAndOwnerCanSeeTheirDates() {
        val repo = MockAppRepository
        val originalProfile = repo.currentProfile.value
        val ownerId = originalProfile.deviceId
        val suffix = UUID.randomUUID().toString().take(8)
        val firstEntrantId = "test-entrant-1-$suffix"
        val secondEntrantId = "test-entrant-2-$suffix"
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        val checkIn = (now / day) * day + 30 * day
        val checkOut = checkIn + 2 * day
        var campsiteId: String? = null

        try {
            val campsite = repo.createCampsite(
                Campsite(
                    ownerId = ownerId,
                    name = "Workflow Test Camp",
                    bookingStartDateMillis = checkIn,
                    bookingEndDateMillis = checkOut + 3 * day
                )
            )
            campsiteId = campsite.id
            val lottery = repo.createLottery(
                Lottery(
                    ownerId = ownerId,
                    campsiteIds = listOf(campsite.id),
                    registrationStartMillis = now - 2 * day,
                    registrationEndMillis = now - day,
                    bookingStartMillis = now - day / 2,
                    bookingEndMillis = now + day,
                    maxWinners = 2,
                    waitlistCapacity = 2
                )
            )

            // Two entrants join the waitlist while the lottery accepts entries.
            for (entrantId in listOf(firstEntrantId, secondEntrantId)) {
                repo.updateProfile(originalProfile.copy(deviceId = entrantId, role = UserRole.ENTRANT))
                var joinError: String? = "Callback not called"
                repo.joinWaitingList(lottery.id, entrantId) { joinError = it }
                assertNull(joinError)
            }
            assertEquals(
                setOf(firstEntrantId, secondEntrantId),
                repo.lotteries.value.single { it.id == lottery.id }.waitingListEntrantIds.toSet()
            )

            // Being on the waitlist is not enough to book before the draw.
            repo.updateProfile(originalProfile.copy(deviceId = firstEntrantId, role = UserRole.ENTRANT))
            var bookingError: String? = "Callback not called"
            repo.confirmBooking(campsite.id, lottery.id, checkIn, checkOut) { bookingError = it }
            assertEquals("Only entrants selected in the lottery can confirm a reservation.", bookingError)
            assertTrue(repo.reservations.value.none { it.campsiteId == campsite.id })

            // With two entrants and two winning spots, both must be selected.
            repo.updateProfile(originalProfile.copy(role = UserRole.CAMPSITE_OWNER))
            repo.drawLotteryWinners(lottery.id)
            val drawn = repo.lotteries.value.single { it.id == lottery.id }
            assertTrue(drawn.isDrawn)
            assertEquals(setOf(firstEntrantId, secondEntrantId), drawn.invitedWinnerIds.toSet())

            // The first winner books two nights; checkout day is not booked.
            repo.updateProfile(originalProfile.copy(deviceId = firstEntrantId, role = UserRole.ENTRANT))
            bookingError = "Callback not called"
            repo.confirmBooking(campsite.id, lottery.id, checkIn, checkOut) { bookingError = it }
            assertNull(bookingError)
            val firstBooking = repo.reservations.value.single {
                it.campsiteId == campsite.id && it.entrantId == firstEntrantId
            }
            assertEquals(ReservationStatus.ACTIVE, firstBooking.status)
            assertEquals(ownerId, firstBooking.ownerId)
            assertEquals(bookingNights(checkIn, checkOut), firstBooking.bookedDates)
            assertEquals(2, firstBooking.bookedDates.size)

            // A second winner cannot reserve one of the same nights.
            repo.updateProfile(originalProfile.copy(deviceId = secondEntrantId, role = UserRole.ENTRANT))
            bookingError = "Callback not called"
            repo.confirmBooking(campsite.id, lottery.id, checkIn + day, checkOut + day) {
                bookingError = it
            }
            assertEquals("One or more selected dates are already booked.", bookingError)
            assertTrue(repo.reservations.value.none {
                it.campsiteId == campsite.id && it.entrantId == secondEntrantId
            })

            // They can book different nights instead.
            bookingError = "Callback not called"
            repo.confirmBooking(campsite.id, lottery.id, checkOut, checkOut + day) { bookingError = it }
            assertNull(bookingError)

            // Both reservations are visible to the owner and shown as booked dates.
            repo.updateProfile(originalProfile.copy(role = UserRole.CAMPSITE_OWNER))
            val ownerBookings = activeOwnerReservations(
                repo.currentProfile.value.deviceId, repo.campsites.value, repo.reservations.value
            ).filter { it.campsiteId == campsite.id }
            assertEquals(setOf(firstEntrantId, secondEntrantId), ownerBookings.map { it.entrantId }.toSet())
            assertEquals(2, ownerBookings.size)

            val bookingsByDay = campsiteBookingsByDay(campsite.id, repo.reservations.value)
            for (night in bookingNights(checkIn, checkOut + day)) {
                assertEquals(CalendarDayStatus.BOOKED, calendarDayStatus(campsite, night, bookingsByDay))
            }
            val laterNight = bookingNights(checkOut + day, checkOut + 2 * day).single()
            assertEquals(CalendarDayStatus.AVAILABLE, calendarDayStatus(campsite, laterNight, bookingsByDay))
            assertFalse(bookingsByDay.containsKey(laterNight))
        } finally {
            repo.updateProfile(originalProfile)
            campsiteId?.let { repo.removeCampsiteAsAdmin(it) }
        }
    }
}
