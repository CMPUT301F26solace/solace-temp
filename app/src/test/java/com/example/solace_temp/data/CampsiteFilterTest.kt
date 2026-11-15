/**
 * CampsiteFilterTest.kt
 *
 * Purpose: Unit tests verifying campsite filtering by location, date, and guest count (US 01.01.02).
 */
package com.example.solace_temp.data

import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Geolocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class CampsiteFilterTest {

    private fun utcMillis(isoDate: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.parse(isoDate)!!.time
    }

    private val sampleCampsites = listOf(
        Campsite(
            id = "c1",
            name = "Tunnel Mountain Village I",
            location = Geolocation(51.1862, -115.5398, "Banff National Park, AB"),
            maxOccupancy = 4,
            availableDates = listOf("2027-05-07", "2027-05-08")
        ),
        Campsite(
            id = "c2",
            name = "Hinton-Grande Cache Pines",
            location = Geolocation(53.4004, -117.5684, "Hinton-Grande Cache, AB"),
            maxOccupancy = 8,
            availableDates = listOf("2027-05-21", "2027-05-22")
        ),
        Campsite(
            id = "c3",
            name = "Wapiti Campground",
            location = Geolocation(52.8456, -118.0750, "Jasper National Park, AB"),
            maxOccupancy = 6,
            availableDates = listOf("2027-05-07", "2027-05-08", "2027-05-21")
        )
    )

    @Test
    fun filterByLocation_matchesSpecifiedRegionCaseInsensitive() {
        val result = CampsiteFilter.filter(sampleCampsites, locationQuery = "banff")
        assertEquals(listOf("c1"), result.map { it.id })
    }

    @Test
    fun filterByDate_showsCampsitesAvailableForSelectedRange() {
        val result = CampsiteFilter.filter(
            sampleCampsites,
            startDateMillis = utcMillis("2027-05-07"),
            endDateMillis = utcMillis("2027-05-08")
        )
        assertEquals(listOf("c1", "c3"), result.map { it.id })
    }

    @Test
    fun filterByGuests_showsCampsitesWithCapacityGreaterThanOrEqual() {
        val result = CampsiteFilter.filter(sampleCampsites, minGuests = 6)
        assertEquals(listOf("c2", "c3"), result.map { it.id })
    }

    @Test
    fun combinedFilters_applyAllCriteriaTogether() {
        val result = CampsiteFilter.filter(
            campsites = sampleCampsites,
            locationQuery = "National Park",
            startDateMillis = utcMillis("2027-05-07"),
            endDateMillis = utcMillis("2027-05-08"),
            minGuests = 6
        )
        assertEquals(listOf("c3"), result.map { it.id })
    }

    @Test
    fun clearingFilters_restoresFullUnfilteredList() {
        val filtered = CampsiteFilter.filter(sampleCampsites, locationQuery = "Banff")
        assertEquals(1, filtered.size)

        val restored = CampsiteFilter.filter(sampleCampsites)
        assertEquals(3, restored.size)
    }

    @Test
    fun noMatchingCampsites_returnsEmptyList() {
        val result = CampsiteFilter.filter(
            sampleCampsites,
            locationQuery = "Waterton",
            minGuests = 10
        )
        assertTrue(result.isEmpty())
    }
}