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
                repo.joinWaitingList(lottery.id, entrantId, com.example.solace_temp.data.WaitingListErrorCallback { joinError = it })
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

            // Verify winner notifications were generated for each newly invited winner
            val winnerNotifs = repo.notifications.value.filter {
                it.isLotteryWin && it.campsiteId == campsite.id
            }
            assertEquals(2, winnerNotifs.size)
            assertEquals(setOf(firstEntrantId, secondEntrantId), winnerNotifs.map { it.recipientId }.toSet())

            // Verify duplicate notifications are avoided if drawLotteryWinners is called again
            val notifCountBeforeReDraw = repo.notifications.value.size
            repo.drawLotteryWinners(lottery.id)
            assertEquals(notifCountBeforeReDraw, repo.notifications.value.size)

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

    @Test
    fun declineLotteryInvitationRemovesEntrantAndRedrawPicksReplacement() = kotlinx.coroutines.runBlocking {
        val repo = MockAppRepository
        val originalProfile = repo.currentProfile.value
        val ownerId = originalProfile.deviceId
        val suffix = UUID.randomUUID().toString().take(8)
        val entrant1 = "entrant-decline-1-$suffix"
        val entrant2 = "entrant-decline-2-$suffix"
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        var campsiteId: String? = null

        try {
            val campsite = repo.createCampsite(
                Campsite(ownerId = ownerId, name = "Decline Test Camp")
            )
            campsiteId = campsite.id
            val lottery = repo.createLottery(
                Lottery(
                    ownerId = ownerId,
                    campsiteIds = listOf(campsite.id),
                    registrationStartMillis = now - 2 * day,
                    registrationEndMillis = now - day,
                    bookingStartMillis = now - day / 2,
                    bookingEndMillis = now + 10 * day,
                    maxWinners = 1
                )
            )

            // Both entrants join waitlist
            for (id in listOf(entrant1, entrant2)) {
                repo.updateProfile(originalProfile.copy(deviceId = id, role = UserRole.ENTRANT))
                repo.joinWaitingList(lottery.id, id) {}
            }

            // Draw 1 winner (with maxWinners = 1, one of them will win)
            repo.updateProfile(originalProfile.copy(role = UserRole.CAMPSITE_OWNER))
            repo.drawLotteryWinners(lottery.id)
            val initialDrawn = repo.lotteries.value.single { it.id == lottery.id }
            assertEquals(1, initialDrawn.invitedWinnerIds.size)
            val winnerId = initialDrawn.invitedWinnerIds.first()
            val remainingId = if (winnerId == entrant1) entrant2 else entrant1

            // Winner declines invitation
            repo.updateProfile(originalProfile.copy(deviceId = winnerId, role = UserRole.ENTRANT))
            val notif = repo.notifications.value.first { it.recipientId == winnerId && it.isLotteryWin }
            repo.declineLotteryInvitation(notif.id, campsite.id)

            val updatedAfterDecline = repo.lotteries.value.single { it.id == lottery.id }
            assertFalse(winnerId in updatedAfterDecline.invitedWinnerIds)
            assertFalse(winnerId in updatedAfterDecline.waitingListEntrantIds)

            // Owner triggers redraw
            repo.updateProfile(originalProfile.copy(role = UserRole.CAMPSITE_OWNER))
            repo.redrawLottery(lottery.id)

            val redrawn = repo.lotteries.value.single { it.id == lottery.id }
            assertEquals(listOf(remainingId), redrawn.invitedWinnerIds)
            assertFalse(winnerId in redrawn.invitedWinnerIds)
        } finally {
            repo.updateProfile(originalProfile)
            campsiteId?.let { repo.removeCampsiteAsAdmin(it) }
        }
    }

    @Test
    fun redrawLotteryDoesNotAddWinnersWhenMaxWinnersCapacityFull() {
        val repo = MockAppRepository
        val originalProfile = repo.currentProfile.value
        val ownerId = originalProfile.deviceId
        val suffix = UUID.randomUUID().toString().take(8)
        val entrant1 = "entrant-full-1-$suffix"
        val entrant2 = "entrant-full-2-$suffix"
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        var campsiteId: String? = null

        try {
            val campsite = repo.createCampsite(
                Campsite(ownerId = ownerId, name = "Capacity Test Camp")
            )
            campsiteId = campsite.id
            val lottery = repo.createLottery(
                Lottery(
                    ownerId = ownerId,
                    campsiteIds = listOf(campsite.id),
                    registrationStartMillis = now - 2 * day,
                    registrationEndMillis = now - day,
                    bookingStartMillis = now - day / 2,
                    bookingEndMillis = now + 10 * day,
                    maxWinners = 1
                )
            )

            for (id in listOf(entrant1, entrant2)) {
                repo.updateProfile(originalProfile.copy(deviceId = id, role = UserRole.ENTRANT))
                repo.joinWaitingList(lottery.id, id) {}
            }

            repo.updateProfile(originalProfile.copy(role = UserRole.CAMPSITE_OWNER))
            repo.drawLotteryWinners(lottery.id)
            val initialWinners = repo.lotteries.value.single { it.id == lottery.id }.invitedWinnerIds

            // Attempting to redraw when maxWinners (1) is already satisfied should be a no-op
            repo.redrawLottery(lottery.id)
            val winnersAfterRedraw = repo.lotteries.value.single { it.id == lottery.id }.invitedWinnerIds
            assertEquals(initialWinners, winnersAfterRedraw)
        } finally {
            repo.updateProfile(originalProfile)
            campsiteId?.let { repo.removeCampsiteAsAdmin(it) }
        }
    }

    @Test
    fun drawLotteryWinnersHandlesEmptyWaitlistGracefully() {
        val repo = MockAppRepository
        val originalProfile = repo.currentProfile.value
        val ownerId = originalProfile.deviceId
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        var campsiteId: String? = null

        try {
            val campsite = repo.createCampsite(
                Campsite(ownerId = ownerId, name = "Empty Waitlist Camp")
            )
            campsiteId = campsite.id
            val lottery = repo.createLottery(
                Lottery(
                    ownerId = ownerId,
                    campsiteIds = listOf(campsite.id),
                    registrationStartMillis = now - 2 * day,
                    registrationEndMillis = now - day,
                    bookingStartMillis = now - day / 2,
                    bookingEndMillis = now + 10 * day,
                    maxWinners = 3
                )
            )

            repo.drawLotteryWinners(lottery.id)
            val drawn = repo.lotteries.value.single { it.id == lottery.id }
            assertTrue(drawn.isDrawn)
            assertTrue(drawn.invitedWinnerIds.isEmpty())
        } finally {
            repo.updateProfile(originalProfile)
            campsiteId?.let { repo.removeCampsiteAsAdmin(it) }
        }
    }

    @Test
    fun waitlistCapacityEnforcementAndLeaveReopensSlot() {
        val repo = MockAppRepository
        val originalProfile = repo.currentProfile.value
        val suffix = UUID.randomUUID().toString().take(8)
        val entrant1 = "capacity-entrant-1-$suffix"
        val entrant2 = "capacity-entrant-2-$suffix"
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val lottery = repo.createLottery(
            Lottery(
                ownerId = originalProfile.deviceId,
                registrationStartMillis = now - day,
                bookingEndMillis = now + 10 * day,
                waitlistCapacity = 1
            )
        )

        // First entrant joins
        repo.updateProfile(originalProfile.copy(deviceId = entrant1, role = UserRole.ENTRANT))
        var error1: String? = "Not set"
        repo.joinWaitingList(lottery.id, entrant1, com.example.solace_temp.data.WaitingListErrorCallback { error1 = it })
        assertNull(error1)

        // Second entrant tries to join -> blocked because capacity = 1
        repo.updateProfile(originalProfile.copy(deviceId = entrant2, role = UserRole.ENTRANT))
        var error2: String? = "Not set"
        repo.joinWaitingList(lottery.id, entrant2, com.example.solace_temp.data.WaitingListErrorCallback { error2 = it })
        assertEquals("Waitlist is full.", error2)

        // First entrant leaves
        repo.updateProfile(originalProfile.copy(deviceId = entrant1, role = UserRole.ENTRANT))
        repo.leaveWaitingList(lottery.id, entrant1)
        assertFalse(entrant1 in repo.lotteries.value.single { it.id == lottery.id }.waitingListEntrantIds)

        // Second entrant joins again -> succeeds now that slot opened
        repo.updateProfile(originalProfile.copy(deviceId = entrant2, role = UserRole.ENTRANT))
        var error3: String? = "Not set"
        repo.joinWaitingList(lottery.id, entrant2, com.example.solace_temp.data.WaitingListErrorCallback { error3 = it })
        assertNull(error3)
        assertTrue(entrant2 in repo.lotteries.value.single { it.id == lottery.id }.waitingListEntrantIds)
    }
}
