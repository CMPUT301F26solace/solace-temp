package com.example.solace_temp.data

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DAY_MILLIS = 86_400_000L
private const val MAX_BOOKING_NIGHTS = 60

/** DateRangePicker uses UTC midnight; store each booked night as yyyy-MM-dd. */
internal fun bookingNights(checkInMillis: Long, checkOutMillis: Long): List<String> {
    if (checkOutMillis <= checkInMillis) return emptyList()
    val length = checkOutMillis - checkInMillis
    if (length % DAY_MILLIS != 0L) return emptyList()
    val count = length / DAY_MILLIS
    if (count !in 1..MAX_BOOKING_NIGHTS.toLong()) return emptyList()
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return (0 until count.toInt()).map { index ->
        format.format(Date(checkInMillis + index * DAY_MILLIS))
    }
}

/**
 * Validates winner eligibility, booking date availability, and existing reservations.
 *
 * An entrant selected in the lottery can book dates within the campsite's allowed booking window
 * as soon as they win the lottery. The booking window defines when the campsite is available to stay,
 * not a waiting period during which winners are prohibited from confirming their reservation.
 *
 * @param campsite The [Campsite] being booked.
 * @param lottery The [Lottery] associated with the campsite.
 * @param entrantId The device ID of the entrant attempting the booking.
 * @param checkInMillis The check-in timestamp in milliseconds (UTC midnight).
 * @param checkOutMillis The check-out timestamp in milliseconds (UTC midnight).
 * @param reservations Current list of existing [Reservation]s to check for night collisions.
 * @param nowMillis The current timestamp in milliseconds, used to prevent check-ins in the past.
 * @return An error message describing why the booking is invalid, or `null` if valid.
 */
internal fun bookingValidationError(
    campsite: Campsite,
    lottery: Lottery,
    entrantId: String,
    checkInMillis: Long,
    checkOutMillis: Long,
    reservations: List<Reservation>,
    nowMillis: Long = System.currentTimeMillis()
): String? {
    val nights = bookingNights(checkInMillis, checkOutMillis)
    return when {
        nights.isEmpty() -> "Choose a valid check-in and check-out date (up to 60 nights)."
        campsite.id !in lottery.campsiteIds || campsite.ownerId != lottery.ownerId ->
            "This campsite is not part of the selected lottery."
        entrantId !in lottery.invitedWinnerIds ->
            "Only entrants selected in the lottery can confirm a reservation."
        checkInMillis < (nowMillis / DAY_MILLIS) * DAY_MILLIS ->
            "Check-in date cannot be in the past."
        checkInMillis < (campsite.bookingStartDateMillis ?: lottery.bookingStartMillis ?: 0L) ||
            checkOutMillis > (campsite.bookingEndDateMillis ?: lottery.bookingEndMillis ?: Long.MAX_VALUE) ->
            "Selected dates are outside the allowed campsite booking dates."
        campsite.availableDates.isNotEmpty() && nights.any { it !in campsite.availableDates } ->
            "One or more selected nights are unavailable."
        reservations.any { booking ->
            booking.campsiteId == campsite.id &&
                booking.status == ReservationStatus.ACTIVE &&
                booking.entrantId == entrantId
        } -> "You already have a confirmed reservation for this campsite."
        reservations.any { booking ->
            booking.campsiteId == campsite.id &&
                booking.status == ReservationStatus.ACTIVE &&
                booking.bookedDates.any { it in nights }
        } -> "One or more selected dates are already booked."
        else -> null
    }
}
