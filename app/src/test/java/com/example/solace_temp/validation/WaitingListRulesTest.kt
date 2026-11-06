/**
 * WaitingListRulesTest.kt
 * Purpose: Model/rule and repository-control tests for US 01.02.01/US 01.02.02.
 * Issues: Firestore networking is verified separately on the Android app.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.Lottery
import org.junit.Assert.*
import org.junit.Test

class WaitingListRulesTest {
    private val entrant = "device-entrant"

    private val lottery = Lottery(
        id = "lottery-test",
        campsiteIds = listOf("camp-banff-01"),
        registrationStartMillis = 100,
        registrationEndMillis = 200,
        bookingEndMillis = 500
    )

    @Test
    fun entryWindowIncludesItsBoundaries() {
        assertNotNull(WaitingListRules.entryError(lottery, 99))
        assertNull(WaitingListRules.entryError(lottery, 100))
        assertNull(WaitingListRules.entryError(lottery, 500))
        assertNotNull(WaitingListRules.entryError(lottery, 501))
    }

    @Test
    fun bookingStageRemainsJoinableAfterRegistrationAndDraw() {
        assertNull(
            WaitingListRules.joinError(
                lottery.copy(isDrawn = true),
                entrant,
                300
            )
        )
    }

    @Test
    fun legacyDatesIncludeTheWholeClosingDay() {
        val legacy = lottery.copy(
            registrationStartMillis = null,
            registrationEndMillis = null,
            bookingEndMillis = null,
            registrationStart = "2026-11-04",
            bookingEnd = "2026-11-05"
        )

        // November 5, 21:00 UTC.
        assertNull(WaitingListRules.entryError(legacy, 1793912400000))

        // November 6, 00:00 UTC.
        assertNotNull(WaitingListRules.entryError(legacy, 1793923200000))
    }

    @Test
    fun missingOrInvalidLotteryDatesDoNotAllowEntry() {
        assertNotNull(WaitingListRules.entryError(null, 150))

        assertNotNull(
            WaitingListRules.entryError(
                lottery.copy(registrationStartMillis = 600),
                150
            )
        )

        assertNotNull(
            WaitingListRules.entryError(
                lottery.copy(
                    registrationStartMillis = null,
                    registrationStart = "2026-02-30"
                ),
                150
            )
        )
    }

    @Test
    fun exactTimestampsTakePrecedenceOverLegacyDates() {
        assertNull(
            WaitingListRules.entryError(
                lottery.copy(
                    registrationStart = "invalid",
                    bookingEnd = "invalid"
                ),
                150
            )
        )
    }

    @Test
    fun capacityAllowsTheLastPlaceAndRejectsTheNext() {
        val limited = lottery.copy(
            waitlistCapacity = 2,
            waitingListEntrantIds = listOf("other-1")
        )

        assertNull(WaitingListRules.joinError(limited, entrant, 150))

        assertNotNull(
            WaitingListRules.joinError(
                limited.copy(
                    waitingListEntrantIds = listOf("other-1", "other-2")
                ),
                entrant,
                150
            )
        )

        assertNotNull(
            WaitingListRules.joinError(
                lottery.copy(waitlistCapacity = 0),
                entrant,
                150
            )
        )
    }

    @Test
    fun unlimitedWaitlistAndDuplicatePrevention() {
        assertNull(WaitingListRules.joinError(lottery, entrant, 150))

        assertNotNull(
            WaitingListRules.joinError(
                lottery.copy(waitingListEntrantIds = listOf(entrant)),
                entrant,
                150
            )
        )

        assertNotNull(WaitingListRules.joinError(lottery, "", 150))
    }

    @Test
    fun leavingRemainsAvailableAfterClosingOrCapacityIsReached() {
        val closed = lottery.copy(
            bookingEndMillis = 0,
            waitlistCapacity = 1,
            waitingListEntrantIds = listOf(entrant)
        )

        assertNull(WaitingListRules.leaveError(closed, entrant))

        assertNull(
            WaitingListRules.leaveError(
                closed.copy(waitingListEntrantIds = emptyList()),
                entrant
            )
        )

        assertNotNull(WaitingListRules.leaveError(null, entrant))
        assertNotNull(WaitingListRules.leaveError(closed, ""))
    }

    @Test
    fun repositoryJoinReportsSuccessThenRejectsDuplicate() {
        val created = MockAppRepository.createLottery(
            lottery.copy(
                id = "",
                registrationStartMillis = 0,
                bookingEndMillis = Long.MAX_VALUE
            )
        )

        var outcome: Result<Unit>? = null

        MockAppRepository.joinWaitingListWithResult(created.id, entrant) {
            outcome = it
        }

        assertTrue(outcome!!.isSuccess)

        MockAppRepository.joinWaitingListWithResult(created.id, entrant) {
            outcome = it
        }

        assertTrue(outcome!!.isFailure)

        assertEquals(
            1,
            MockAppRepository.lotteries.value
                .first { it.id == created.id }
                .waitingListEntrantIds.count { it == entrant }
        )
        assertNotNull(
            MockAppRepository.lotteries.value
                .first { it.id == created.id }
                .waitlistJoinedAtMillis[entrant]
        )
    }

    @Test
    fun repositoryRejectsFullListsWithoutChangingMembers() {
        val created = MockAppRepository.createLottery(
            lottery.copy(
                id = "",
                registrationStartMillis = 0,
                bookingEndMillis = Long.MAX_VALUE,
                waitlistCapacity = 1,
                waitingListEntrantIds = listOf("other-entrant")
            )
        )

        var outcome: Result<Unit>? = null

        MockAppRepository.joinWaitingListWithResult(created.id, entrant) {
            outcome = it
        }

        assertTrue(outcome!!.isFailure)

        assertEquals(
            listOf("other-entrant"),
            MockAppRepository.lotteries.value
                .first { it.id == created.id }
                .waitingListEntrantIds
        )
    }

    @Test
    fun repositoryLeaveRemovesOnlyTheRequestedEntrant() {
        val created = MockAppRepository.createLottery(
            lottery.copy(
                id = "",
                bookingEndMillis = 0,
                waitingListEntrantIds = listOf(entrant, "other-entrant"),
                waitlistJoinedAtMillis = mapOf(
                    entrant to 100L,
                    "other-entrant" to 200L
                )
            )
        )

        var outcome: Result<Unit>? = null

        MockAppRepository.leaveWaitingList(created.id, entrant) {
            outcome = it
        }

        assertTrue(outcome!!.isSuccess)

        assertEquals(
            listOf("other-entrant"),
            MockAppRepository.lotteries.value
                .first { it.id == created.id }
                .waitingListEntrantIds
        )
        assertEquals(
            mapOf("other-entrant" to 200L),
            MockAppRepository.lotteries.value
                .first { it.id == created.id }
                .waitlistJoinedAtMillis
        )
    }

    @Test
    fun safeFailureMessagesPreserveValidationAndHideBackendDetails() {
        val specific = "This waiting list is full."

        assertEquals(
            specific,
            WaitingListRules.failureMessage(
                RuntimeException(
                    "backend wrapper",
                    WaitingListException(specific)
                )
            )
        )

        assertFalse(
            WaitingListRules.failureMessage(
                Exception("private backend details")
            ).contains("private backend details")
        )
    }
}