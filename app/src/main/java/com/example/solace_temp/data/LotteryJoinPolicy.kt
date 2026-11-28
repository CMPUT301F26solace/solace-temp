/**
 * LotteryJoinPolicy.kt
 * 
 * Purpose: Defines policy helper functions for determining whether an entrant can join a lottery
 * during its registration and booking window.
 * 
 * Issues / Outstanding Issues:
 * - None currently known; policy checks integrate with WaitingListRules.
 */
package com.example.solace_temp.data

import com.example.solace_temp.models.Lottery

/** Registration remains open to new entrants through the booking period. */
internal fun canJoinLottery(lottery: Lottery, nowMillis: Long = System.currentTimeMillis()): Boolean {
    return (lottery.registrationStartMillis == null || nowMillis >= lottery.registrationStartMillis) &&
        (lottery.bookingEndMillis == null || nowMillis <= lottery.bookingEndMillis)
}
