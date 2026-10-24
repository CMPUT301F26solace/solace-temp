package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.OwnerCampsitesScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerCampsitesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class FakeListAppRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile())
        override val currentProfile: StateFlow<Profile> = _currentProfile

        val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
        override val campsites: StateFlow<List<Campsite>> = _campsites

        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun ownerCampsites_emptyState_showsPromptToAddCampsite() {
        // Acceptance Criteria:
        // System handles empty and error states gracefully
        // An informative message is displayed if the owner has not registered any campsites yet
        // Actionable prompt or button to add a new campsite is shown when the list is empty
        val fakeRepo = FakeListAppRepository()
        
        fakeRepo._currentProfile.value = Profile(
            deviceId = "owner-empty",
            role = UserRole.CAMPSITE_OWNER
        )
        fakeRepo._campsites.value = emptyList()

        var createClicked = false

        composeTestRule.setContent {
            OwnerCampsitesScreen(
                repository = fakeRepo,
                onSelectCampsite = {},
                onCreateNew = { createClicked = true }
            )
        }

        // Verify the informative message
        composeTestRule.onNodeWithText("You haven't registered any campsites yet.").assertIsDisplayed()
        
        // Verify the actionable button
        composeTestRule.onNodeWithText("Add a Campsite").assertIsDisplayed().performClick()
        
        // Verify the action works
        assert(createClicked)
    }

    @Test
    fun ownerCampsites_populatedState_showsOnlyOwnersCampsitesWithThumbnails() {
        // Acceptance Criteria:
        // Owner can access a dedicated view listing all campsites they own
        // System queries the database for campsites matching the owner's identifier
        // Campsites belonging to other owners are excluded from the list
        // Each campsite item displays summary information (name, location, occupancy, thumbnail)
        
        val fakeRepo = FakeListAppRepository()
        
        fakeRepo._currentProfile.value = Profile(
            deviceId = "owner-valid",
            role = UserRole.CAMPSITE_OWNER
        )
        
        // Setup one campsite owned by the user, one owned by someone else
        val myCampsite = Campsite(
            id = "camp-mine",
            ownerId = "owner-valid",
            name = "My Awesome Campsite",
            location = Geolocation(locationName = "My Park"),
            maxOccupancy = 12,
            pricePerNight = 100.0,
            imageUrl = "http://example.com/thumb.jpg"
        )
        
        val otherCampsite = Campsite(
            id = "camp-other",
            ownerId = "owner-other",
            name = "Someone Else's Campsite"
        )
        
        fakeRepo._campsites.value = listOf(myCampsite, otherCampsite)

        var selectedCampsiteId: String? = null

        composeTestRule.setContent {
            OwnerCampsitesScreen(
                repository = fakeRepo,
                onSelectCampsite = { id -> selectedCampsiteId = id },
                onCreateNew = {}
            )
        }

        // Verify my campsite is displayed with required summary info
        composeTestRule.onNodeWithText("My Awesome Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("📍 My Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max 12 guests").assertIsDisplayed()
        
        // Check for thumbnail (content description based on image logic)
        composeTestRule.onNodeWithContentDescription("Thumbnail").assertIsDisplayed()

        // Verify other owner's campsite is excluded
        composeTestRule.onNodeWithText("Someone Else's Campsite").assertDoesNotExist()

        // Verify navigation works on click
        composeTestRule.onNodeWithText("My Awesome Campsite").performClick()
        assert(selectedCampsiteId == "camp-mine")
    }
}