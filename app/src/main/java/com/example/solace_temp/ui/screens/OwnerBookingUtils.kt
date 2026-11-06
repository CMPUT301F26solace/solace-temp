package com.example.solace_temp.ui.screens

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.ReservationStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** The reservation model stores individual booked days as yyyy-MM-dd strings. */
internal fun isIsoDate(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value) != null
    } catch (_: Exception) {
        false
    }
}

internal fun todayIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

/** Only current or future confirmed bookings belonging to this owner. */
internal fun activeOwnerReservations(
    ownerId: String,
    campsites: List<Campsite>,
    reservations: List<Reservation>,
    today: String = todayIsoDate()
): List<Reservation> {
    val ownedIds = campsites.filter { it.ownerId == ownerId }.map { it.id }.toSet()
    return reservations.filter { booking ->
        booking.campsiteId in ownedIds &&
            booking.status == ReservationStatus.ACTIVE &&
            booking.bookedDates.any { isIsoDate(it) && it >= today }
    }.sortedBy { booking ->
        booking.bookedDates.filter(::isIsoDate).minOrNull() ?: "9999-12-31"
    }
}

/** The dates shown as booked are exactly the days recorded in a confirmed reservation. */
internal fun campsiteBookingsByDay(
    campsiteId: String,
    reservations: List<Reservation>
): Map<String, List<Reservation>> = reservations
    .filter { it.campsiteId == campsiteId && it.status == ReservationStatus.ACTIVE }
    .flatMap { booking ->
        booking.bookedDates.filter(::isIsoDate).distinct().map { date -> date to booking }
    }
    .groupBy({ it.first }, { it.second })

internal enum class CalendarDayStatus { BOOKED, BLOCKED, AVAILABLE }

internal fun calendarDayStatus(
    campsite: Campsite,
    date: String,
    bookingsByDay: Map<String, List<Reservation>>
): CalendarDayStatus {
    if (bookingsByDay[date].orEmpty().isNotEmpty()) return CalendarDayStatus.BOOKED
    if (campsite.availableDates.isNotEmpty() && date !in campsite.availableDates) {
        return CalendarDayStatus.BLOCKED
    }
    val dayMillis = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)?.time
    } catch (_: Exception) { null }
    if (dayMillis != null) {
        // Use date boundaries rather than comparing midnight against a timestamp with a time of day.
        val dayEnd = dayMillis + 86_400_000L - 1L
        if (campsite.bookingStartDateMillis?.let { dayEnd < it } == true ||
            campsite.bookingEndDateMillis?.let { dayMillis > it } == true
        ) return CalendarDayStatus.BLOCKED
    }
    return CalendarDayStatus.AVAILABLE
}

internal fun reservationDateRange(reservation: Reservation): String {
    val dates = reservation.bookedDates.filter(::isIsoDate).sorted()
    return when (dates.size) {
        0 -> "Dates not set"
        1 -> dates.first()
        else -> "${dates.first()} to ${dates.last()}"
    }
}
