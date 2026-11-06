/**
 * WaitingListRules.kt
 * Purpose: Shared entry checks for US 01.01.06, US 01.02.01 and US 01.02.02.
 * Issues: Checks use the device clock; backend enforcement is owned by the team.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.models.Lottery
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Checks lottery dates and capacity without depending on Android or Firestore. */
object WaitingListRules {
    /**
     * Returns why entry is unavailable, or null while entries are accepted.
     * Team clarification #79 permits entry after registration closes and after
     * a draw, until the booking period ends. Exact timestamps take precedence
     * over legacy ISO dates; a date-only closing date includes its entire UTC day.
     *
     * @param lottery The current lottery, or null when it was removed.
     * @param nowMillis Current time, injectable for deterministic tests.
     */
    fun entryError(
        lottery: Lottery?,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (lottery == null) return "This lottery is no longer available."

        val start = lottery.registrationStartMillis
            ?: isoDate(lottery.registrationStart)

        val end = lottery.bookingEndMillis
            ?: isoDate(lottery.bookingEnd, endOfDay = true)
            ?: lottery.registrationEndMillis
            ?: isoDate(lottery.registrationEnd, endOfDay = true)

        return when {
            start == null || end == null || end < start ->
                "This lottery's dates are unavailable."

            nowMillis < start -> "This lottery is not open yet."
            nowMillis > end -> "This lottery is closed."
            else -> null
        }
    }

    /**
     * Checks a new entry against the latest waiting list.
     *
     * @param lottery The lottery being joined.
     * @param entrantId The entrant's existing device identifier.
     * @param nowMillis Current time for checking the entry window.
     * @return A user-facing error, or null if a new entry is permitted.
     */
    fun joinError(
        lottery: Lottery?,
        entrantId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (entrantId.isBlank()) {
            return "Your device profile is unavailable. Please reopen the app."
        }

        if (lottery == null) return "This lottery is no longer available."

        if (entrantId in lottery.waitingListEntrantIds) {
            return "You are already on this waiting list."
        }

        entryError(lottery, nowMillis)?.let { return it }

        val capacity = lottery.waitlistCapacity

        if (
            capacity != null &&
            lottery.waitingListEntrantIds.distinct().size >= capacity
        ) {
            return "This waiting list is full."
        }

        return null
    }

    /**
     * Checks removal without a date restriction, so entrants can always leave.
     * An entrant already removed is treated as a successful no-op.
     *
     * @param lottery The lottery being left.
     * @param entrantId The entrant's existing device identifier.
     * @return An error, or null when removal is permitted.
     */
    fun leaveError(lottery: Lottery?, entrantId: String): String? = when {
        entrantId.isBlank() ->
            "Your device profile is unavailable. Please reopen the app."

        lottery == null -> "This lottery is no longer available."
        else -> null
    }

    /** Returns controlled validation messages without exposing backend details. */
    fun failureMessage(error: Throwable): String {
        var cause: Throwable? = error

        repeat(8) {
            val current = cause
                ?: return "Could not update the waiting list. Check your connection and try again."

            if (current is WaitingListException) {
                return current.message
                    ?: "The waiting list changed. Please try again."
            }

            cause = current.cause
        }

        return "Could not update the waiting list. Check your connection and try again."
    }

    private fun isoDate(value: String, endOfDay: Boolean = false): Long? {
        if (!value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null

        val utc = TimeZone.getTimeZone("UTC")

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = utc
        }

        val position = ParsePosition(0)
        val date = format.parse(value, position) ?: return null

        if (position.index != value.length) return null
        if (!endOfDay) return date.time

        return Calendar.getInstance(utc).apply {
            time = date
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis - 1
    }
}

/** A waiting-list validation failure safe to display to the entrant. */
class WaitingListException(message: String) : Exception(message)