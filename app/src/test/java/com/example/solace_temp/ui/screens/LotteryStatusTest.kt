package com.example.solace_temp.ui.screens

import com.example.solace_temp.models.Lottery
import org.junit.Assert.assertEquals
import org.junit.Test

class LotteryStatusTest {
    @Test fun lotteryRemainsInBookingAfterFirstDraw() {
        val now = System.currentTimeMillis()
        val lottery = Lottery(registrationStartMillis = now - 120_000,
            registrationEndMillis = now - 60_000,
            bookingStartMillis = now - 30_000, bookingEndMillis = now + 120_000,
            isDrawn = true)
        assertEquals("Booking", getLotteryStatus(lottery))
    }
}
