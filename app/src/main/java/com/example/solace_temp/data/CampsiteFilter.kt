/**
 * CampsiteFilter.kt
 *
 * Purpose: Provides filtering logic for campsites by location, date range, and guest capacity (US 01.01.02).
 */
package com.example.solace_temp.data

import com.example.solace_temp.models.Campsite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility object for filtering campsites individually or across combined criteria.
 */
object CampsiteFilter {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Filters a list of campsites based on optional location, date range, and guest capacity criteria.
     *
     * @param campsites The full list of [Campsite] items to filter.
     * @param locationQuery Optional region, park, or campsite name query.
     * @param startDateMillis Optional check-in timestamp in milliseconds.
     * @param endDateMillis Optional check-out timestamp in milliseconds.
     * @param minGuests Optional minimum required guest capacity.
     * @return A filtered list of [Campsite] items matching all active criteria.
     */
    fun filter(
        campsites: List<Campsite>,
        locationQuery: String = "",
        startDateMillis: Long? = null,
        endDateMillis: Long? = null,
        minGuests: Int? = null
    ): List<Campsite> {
        val trimmedLocation = locationQuery.trim()

        return campsites.filter { site ->
            matchesLocation(site, trimmedLocation) &&
                    matchesDateRange(site, startDateMillis, endDateMillis) &&
                    matchesGuestCount(site, minGuests)
        }
    }

    /**
     * Checks whether a campsite matches the specified location or name query.
     *
     * @param site The [Campsite] to evaluate.
     * @param query The trimmed location search query.
     * @return True if the query is blank or matches the campsite's location or name.
     */
    fun matchesLocation(site: Campsite, query: String): Boolean {
        if (query.isBlank()) return true
        return site.location.locationName.contains(query, ignoreCase = true) ||
                site.name.contains(query, ignoreCase = true)
    }

    /**
     * Checks whether a campsite accommodates at least the requested number of guests.
     *
     * @param site The [Campsite] to evaluate.
     * @param minGuests The minimum required guest count, or null if unset.
     * @return True if no guest filter is applied or [Campsite.maxOccupancy] meets the count.
     */
    fun matchesGuestCount(site: Campsite, minGuests: Int?): Boolean {
        if (minGuests == null || minGuests <= 0) return true
        return site.maxOccupancy >= minGuests
    }

    /**
     * Checks whether a campsite is available across the requested date range.
     *
     * @param site The [Campsite] to evaluate.
     * @param startMillis Selected check-in timestamp in milliseconds, or null if unset.
     * @param endMillis Selected check-out timestamp in milliseconds, or null if unset.
     * @return True if the campsite is available for the entire requested date range.
     */
    fun matchesDateRange(site: Campsite, startMillis: Long?, endMillis: Long?): Boolean {
        if (startMillis == null && endMillis == null) return true
        val effectiveStart = startMillis ?: endMillis!!
        val effectiveEnd = endMillis ?: startMillis!!
        if (effectiveEnd < effectiveStart) return false

        // Check explicit available dates when configured on the campsite
        if (site.availableDates.isNotEmpty()) {
            val requestedDates = enumerateIsoDates(effectiveStart, effectiveEnd)
            return requestedDates.all { it in site.availableDates }
        }

        // Fall back to the campsite's allowed booking window bounds
        val campStart = site.bookingStartDateMillis
        val campEnd = site.bookingEndDateMillis
        if (campStart != null || campEnd != null) {
            val minAllowed = campStart ?: 0L
            val maxAllowed = campEnd ?: Long.MAX_VALUE
            return effectiveStart >= minAllowed && effectiveEnd <= maxAllowed
        }

        return true
    }

    private fun enumerateIsoDates(startMillis: Long, endMillis: Long): List<String> {
        val dayMillis = 24L * 60L * 60L * 1000L
        val dates = mutableListOf<String>()
        var current = startMillis
        while (current <= endMillis) {
            dates.add(isoDateFormat.format(Date(current)))
            current += dayMillis
        }
        return dates
    }
}