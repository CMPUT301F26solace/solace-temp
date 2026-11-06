package com.example.solace_temp

import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerWaitlistTest {
    @Test
    fun rejectsEntryWhenWaitlistIsFull() {
        val lottery = MockAppRepository.createLottery(
            Lottery(maxWinners = 1, waitlistCapacity = 1,
                waitingListEntrantIds = listOf("already-on-list"))
        )
        var error: String? = null
        MockAppRepository.joinWaitingList(lottery.id, "new-entrant", com.example.solace_temp.data.WaitingListErrorCallback { error = it })
        assertEquals("Waitlist is full.", error)
        assertFalse(MockAppRepository.lotteries.value
            .first { it.id == lottery.id }.waitingListEntrantIds.contains("new-entrant"))
    }

    @Test
    fun recordsEntryTimeWhenEntrantJoins() {
        val lottery = MockAppRepository.createLottery(
            Lottery(maxWinners = 1, waitlistCapacity = 2)
        )
        var error: String? = "unsubmitted"
        MockAppRepository.joinWaitingList(lottery.id, "test-entrant", com.example.solace_temp.data.WaitingListErrorCallback { error = it })
        assertNull(error)
        val saved = MockAppRepository.lotteries.value.first { it.id == lottery.id }
        assertTrue("test-entrant" in saved.waitingListEntrantIds)
        assertNotNull(saved.waitlistJoinedAtMillis["test-entrant"])
    }

    @Test
    fun unlimitedWaitlistAllowsMoreEntrants() {
        val lottery = MockAppRepository.createLottery(Lottery(maxWinners = 1))
        val entrants = listOf("entrant-a", "entrant-b", "entrant-c")
        entrants.forEach { id ->
            var error: String? = "not submitted"
            MockAppRepository.joinWaitingList(lottery.id, id, com.example.solace_temp.data.WaitingListErrorCallback { error = it })
            assertNull(error)
        }
        val saved = MockAppRepository.lotteries.value.first { it.id == lottery.id }
        assertEquals(3, saved.waitingListEntrantIds.size)
    }

    @Test
    fun leavingWaitlistRemovesEntrant() {
        val lottery = MockAppRepository.createLottery(
            Lottery(waitingListEntrantIds = listOf("entrant-a", "entrant-b"))
        )
        MockAppRepository.leaveWaitingList(lottery.id, "entrant-a")
        val saved = MockAppRepository.lotteries.value.first { it.id == lottery.id }
        assertEquals(listOf("entrant-b"), saved.waitingListEntrantIds)
    }
}
