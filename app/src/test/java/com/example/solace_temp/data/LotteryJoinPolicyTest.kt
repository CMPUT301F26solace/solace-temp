package com.example.solace_temp.data

import com.example.solace_temp.models.Lottery
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryJoinPolicyTest {
    private val lottery = Lottery(registrationStartMillis = 1000L, registrationEndMillis = 2000L,
        bookingStartMillis = 3000L, bookingEndMillis = 5000L, isDrawn = true)

    @Test fun allowsEntryAfterFirstDrawDuringBooking() {
        assertTrue(canJoinLottery(lottery, 4000L))
    }

    @Test fun blocksEntryBeforeRegistration() {
        assertFalse(canJoinLottery(lottery, 999L))
    }

    @Test fun blocksEntryAfterBookingEnd() {
        assertFalse(canJoinLottery(lottery, 5001L))
    }
}
