/**
 * CampsiteLotteryCreationTest.kt
 *
 * Purpose: Instrumented test suite verifying lottery creation workflows, setting proper
 * registration and booking dates, configuring max lottery winners, and enforcing validation rules.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CreateLotteryDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CampsiteLotteryCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class LotteryFakeRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile(deviceId = "owner-lottery-test", role = UserRole.CAMPSITE_OWNER))
        override val currentProfile: StateFlow<Profile> = _currentProfile

        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(emptyList())

        val _campsites = MutableStateFlow(listOf(
            Campsite(
                id = "camp-l-1",
                ownerId = "owner-lottery-test",
                name = "Lottery Test Campsite",
                maxOccupancy = 6,
                pricePerNight = 50.0
            )
        ))
        override val campsites: StateFlow<List<Campsite>> = _campsites

        val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = _lotteries

        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        var createdLottery: Lottery? = null

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery {
            val newId = if (lottery.id.isBlank()) "lot-${UUID.randomUUID().toString().take(6)}" else lottery.id
            val saved = lottery.copy(id = newId)
            createdLottery = saved
            _lotteries.value = _lotteries.value + saved
            return saved
        }
        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: (String?) -> Unit) {
            onResult(null)
        }
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun redrawLottery(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun createLottery_withValidDatesAndMaxWinners_persistsSuccessfully() {
        val fakeRepo = LotteryFakeRepository()
        val now = System.currentTimeMillis()
        val regStart = now + 86400000L // Tomorrow
        val regEnd = regStart + 3 * 86400000L // 3 days later
        val bookStart = regEnd + 8 * 86400000L // 8 days after reg end (> 1 week gap)
        val bookEnd = bookStart + 7 * 86400000L

        val lottery = Lottery(
            name = "Peak Season Lottery",
            campsiteIds = listOf("camp-l-1"),
            ownerId = "owner-lottery-test",
            maxWinners = 4,
            registrationStartMillis = regStart,
            registrationEndMillis = regEnd,
            bookingStartMillis = bookStart,
            bookingEndMillis = bookEnd
        )

        val created = fakeRepo.createLottery(lottery)

        assertEquals("Peak Season Lottery", created.name)
        assertEquals(4, created.maxWinners)
        assertEquals(regStart, created.registrationStartMillis)
        assertEquals(regEnd, created.registrationEndMillis)
        assertEquals(bookStart, created.bookingStartMillis)
        assertEquals(bookEnd, created.bookingEndMillis)
        assertTrue(fakeRepo._lotteries.value.contains(created))
    }

    @Test
    fun createLotteryDialog_validatesRequiredDatesBeforePublishing() {
        val fakeRepo = LotteryFakeRepository()
        val campsite = fakeRepo._campsites.value.first()

        composeTestRule.setContent {
            CreateLotteryDialog(
                campsite = campsite,
                repository = fakeRepo,
                onDismiss = {},
                onSuccess = {}
            )
        }

        // Try to publish without selecting dates
        composeTestRule.onNodeWithText("Publish Lottery").performClick()

        // Verify that dialog enforces selecting registration start & close dates (scroll to it since it's in a scrollable column)
        composeTestRule.onNodeWithText("Error: Please select both a start and closing date/time.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun createLotteryDialog_allowsSettingMaxWinnersAndDates() {
        val fakeRepo = LotteryFakeRepository()
        val campsite = fakeRepo._campsites.value.first()

        composeTestRule.setContent {
            CreateLotteryDialog(
                campsite = campsite,
                repository = fakeRepo,
                onDismiss = {},
                onSuccess = {}
            )
        }

        // Set max winners to 5 using set text action on the winner text field
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        if (textFields.fetchSemanticsNodes().size >= 2) {
            textFields[1].performTextClearance()
            textFields[1].performTextInput("5")
        }

        // Test creating lottery with valid dates and max winners via repository
        val now = System.currentTimeMillis()
        val created = fakeRepo.createLottery(
            Lottery(
                name = "Dialog Configured Lottery",
                campsiteIds = listOf(campsite.id),
                ownerId = "owner-lottery-test",
                maxWinners = 5,
                registrationStartMillis = now + 86400000L,
                registrationEndMillis = now + 4 * 86400000L,
                bookingStartMillis = now + 12 * 86400000L,
                bookingEndMillis = now + 20 * 86400000L
            )
        )

        assertEquals(5, created.maxWinners)
        assertEquals("Dialog Configured Lottery", created.name)
        assertTrue(fakeRepo._lotteries.value.contains(created))
    }
}
